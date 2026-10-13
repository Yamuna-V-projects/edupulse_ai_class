"""
EduPulse AI: Interactive Classroom & Adaptive Quiz Studio
Student View & Gemini RAG Pipeline (Standalone Streamlit Web Application)
"""

import os
import json
import sqlite3
import re
from typing import List, Dict, Any, Optional
import streamlit as st

# Optional google-genai or google.generativeai integration
try:
    from google import genai
    from google.genai import types
    HAS_GENAI = True
except ImportError:
    try:
        import google.generativeai as genai_legacy
        HAS_GENAI = False
    except ImportError:
        HAS_GENAI = False

DB_PATH = "edupulse_classroom.db"

# ==============================================================================
# DATABASE LAYER (SQLite & Schema Initialization)
# ==============================================================================
def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        role TEXT NOT NULL,
        password TEXT NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS classes (
        id TEXT PRIMARY KEY,
        code TEXT UNIQUE NOT NULL,
        title TEXT NOT NULL,
        subject TEXT NOT NULL,
        educator_name TEXT NOT NULL,
        description TEXT
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS enrollments (
        id TEXT PRIMARY KEY,
        class_code TEXT NOT NULL,
        student_id TEXT NOT NULL,
        student_name TEXT NOT NULL,
        joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(class_code, student_id)
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS materials (
        id TEXT PRIMARY KEY,
        class_code TEXT NOT NULL,
        title TEXT NOT NULL,
        topic TEXT NOT NULL,
        content TEXT NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS flashcard_decks (
        id TEXT PRIMARY KEY,
        class_code TEXT NOT NULL,
        material_id TEXT NOT NULL,
        title TEXT NOT NULL,
        cards_json TEXT NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS quizzes (
        id TEXT PRIMARY KEY,
        class_code TEXT NOT NULL,
        material_id TEXT NOT NULL,
        title TEXT NOT NULL,
        difficulty TEXT NOT NULL,
        questions_json TEXT NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS quiz_attempts (
        id TEXT PRIMARY KEY,
        quiz_id TEXT NOT NULL,
        class_code TEXT NOT NULL,
        student_id TEXT NOT NULL,
        student_name TEXT NOT NULL,
        score INTEGER NOT NULL,
        total_questions INTEGER NOT NULL,
        answers_json TEXT NOT NULL,
        completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # Seed Default Data if empty
    cursor.execute("SELECT COUNT(*) FROM classes")
    if cursor.fetchone()[0] == 0:
        # Seed educator and student
        cursor.execute("INSERT OR IGNORE INTO users VALUES ('edu_1', 'Dr. Aris Vance', 'aris@university.edu', 'EDUCATOR', 'password123')")
        cursor.execute("INSERT OR IGNORE INTO users VALUES ('stu_1', 'Maya Chen', 'maya@student.edu', 'STUDENT', 'password123')")

        # Seed Class: Physics 101
        class_code = "XJ79K2"
        cursor.execute("""
        INSERT OR IGNORE INTO classes VALUES (
            'cls_1', 'XJ79K2', 'Physics 101: Mechanics & Dynamics', 'Physics', 'Dr. Aris Vance',
            'Fundamental laws of classical motion, inertial frames, and force interactions.'
        )
        """)

        # Enroll Maya
        cursor.execute("INSERT OR IGNORE INTO enrollments VALUES ('enr_1', 'XJ79K2', 'stu_1', 'Maya Chen', CURRENT_TIMESTAMP)")

        physics_notes = """
# Lecture 3: Newton's Laws & Force Interactions
## 1. Newton's First Law (Law of Inertia)
An object remains at rest or in uniform straight-line motion unless acted upon by a net external force. An inertial reference frame is defined as one where Newton's First Law holds true without invoking fictitious forces.

## 2. Newton's Second Law
Net force equals mass times acceleration (F_net = m * a). More fundamentally, force is the rate of change of linear momentum: F = dp/dt.

## 3. Newton's Third Law (Action-Reaction Pairs)
When body A exerts a force on body B, body B simultaneously exerts an equal and opposite force on body A (F_AB = -F_BA). Action and reaction forces act on two DIFFERENT objects; therefore, they never cancel each other in a single body's free-body diagram.

## 4. Friction
Static friction opposes the onset of motion up to f_s_max = μ_s * N. Kinetic friction opposes sliding motion with f_k = μ_k * N, where μ_k is generally lower than μ_s.
        """.strip()

        cursor.execute("""
        INSERT OR IGNORE INTO materials VALUES (
            'mat_1', 'XJ79K2', 'Unit 3: Newton''s Laws & Dynamics Notes', 'Classical Mechanics', ?
        )
        """, (physics_notes,))

        # Seed Deck
        cards = [
            {"front": "What defines an Inertial Reference Frame?", "back": "A reference frame where Newton's First Law holds true without fictitious forces."},
            {"front": "Why don't Action-Reaction pairs cancel each other out?", "back": "Because they act on two different bodies, not on the same object's free-body diagram."},
            {"front": "What is the relation between static and kinetic friction coefficients?", "back": "Static friction coefficient (μ_s) is generally higher than kinetic friction coefficient (μ_k)."},
            {"front": "State the fundamental calculus definition of Force in terms of momentum.", "back": "Force is the time derivative of momentum: F = dp/dt."}
        ]
        cursor.execute("""
        INSERT OR IGNORE INTO flashcard_decks VALUES (
            'deck_1', 'XJ79K2', 'mat_1', 'Mechanics Core Concepts Deck', ?
        )
        """, (json.dumps(cards),))

        # Seed Quiz
        questions = [
            {
                "id": 1,
                "question": "A book rests motionless on a desk. What is the Newton's Third Law reaction force to Earth's gravitational pull on the book?",
                "options": [
                    "A: The normal contact force exerted upward by the desk on the book",
                    "B: The upward gravitational pull exerted by the book on the Earth",
                    "C: The static friction between the book cover and the desk",
                    "D: Zero, because the book is in static equilibrium"
                ],
                "correct_answer": "B: The upward gravitational pull exerted by the book on the Earth",
                "explanation": "Action-reaction pairs act between the exact same two interacting bodies (Earth and Book). The Earth pulls down on the book, so the reaction is the book pulling up on Earth.",
                "topic_tag": "Newton's Third Law"
            },
            {
                "id": 2,
                "question": "Why do passengers jerk forward when a bus decelerates rapidly?",
                "options": [
                    "A: A fictitious centrifugal force is generated by the engine",
                    "B: Air currents inside the cabin push them forward",
                    "C: Their inertia keeps them moving at original velocity until stopped by an external force",
                    "D: Road friction acts directly on the passengers' torso"
                ],
                "correct_answer": "C: Their inertia keeps them moving at original velocity until stopped by an external force",
                "explanation": "Newton's First Law states an object in motion maintains its velocity unless a net external force acts upon it.",
                "topic_tag": "Inertial Reference Frames"
            },
            {
                "id": 3,
                "question": "A 10 kg box requires 40 N of horizontal force to start sliding, but 30 N to keep moving at constant velocity. What is μ_s (g = 10 m/s²)?",
                "options": [
                    "A: 0.30",
                    "B: 0.40",
                    "C: 0.70",
                    "D: 0.10"
                ],
                "correct_answer": "B: 0.40",
                "explanation": "Normal force N = m*g = 100 N. Maximum static friction is 40 N. Therefore μ_s = f_s_max / N = 40 / 100 = 0.40.",
                "topic_tag": "Friction"
            }
        ]
        cursor.execute("""
        INSERT OR IGNORE INTO quizzes VALUES (
            'quiz_1', 'XJ79K2', 'mat_1', 'Mechanics Adaptive Assessment', 'Medium', ?
        )
        """, (json.dumps(questions),))

    conn.commit()
    conn.close()


# ==============================================================================
# GEMINI API INTEGRATION & LOCALIZED RAG PIPELINE
# ==============================================================================
def extract_json_from_response(raw_text: str) -> Any:
    """Extract and parse clean JSON array from Gemini API output."""
    raw_text = raw_text.strip()
    match = re.search(r'\[.*\]', raw_text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except Exception:
            pass
    try:
        return json.loads(raw_text)
    except Exception:
        return []


def chunk_text(text: str, max_chunk_words: int = 150) -> List[Dict[str, str]]:
    """Localized semantic chunking for RAG."""
    sections = text.split("## ")
    chunks = []
    for sec in sections:
        sec = sec.strip()
        if not sec:
            continue
        lines = sec.split("\n", 1)
        header = lines[0].strip()
        body = lines[1].strip() if len(lines) > 1 else header
        chunks.append({"header": header, "content": body})
    if not chunks:
        chunks = [{"header": "Overview", "content": text}]
    return chunks


def generate_study_flashcards(material_text: str, api_key: Optional[str] = None) -> List[Dict[str, str]]:
    """
    Calls Gemini API to generate study flashcards strictly in JSON format:
    [{"front": "...", "back": "..."}]
    """
    chunks = chunk_text(material_text)
    context_str = "\n\n".join([f"[{c['header']}]: {c['content']}" for c in chunks[:5]])

    prompt = f"""
You are an expert curriculum designer. Based STRICTLY on the course material chunks below, generate 5 study flashcards.

COURSE MATERIAL CONTEXT:
{context_str}

REQUIREMENTS:
- Return ONLY a strict JSON array of objects with the exact schema:
[
  {{"front": "Question or term", "back": "Grounded answer"}}
]
- Do not output markdown code blocks (no ```json).
- Ensure cards are 100% grounded in the provided course text.
"""
    api_key = api_key or os.environ.get("GEMINI_API_KEY")
    if api_key:
        try:
            # Using google-genai client
            client = genai.Client(api_key=api_key)
            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=prompt,
                config=types.GenerateContentConfig(response_mime_type="application/json")
            )
            cards = extract_json_from_response(response.text)
            if isinstance(cards, list) and len(cards) > 0:
                return cards
        except Exception as e:
            st.warning(f"Gemini API Notice: {e}. Generating localized RAG cards.")

    # High-quality fallback generator from chunks
    fallback_cards = []
    for c in chunks:
        sentences = [s.strip() for s in c["content"].split(".") if len(s.strip()) > 20]
        if sentences:
            fallback_cards.append({
                "front": f"Key Principle ({c['header']}): What is the primary definition?",
                "back": sentences[0] + "."
            })
    return fallback_cards or [
        {"front": "Core Principle", "back": "Grounded concept derived from classroom textbook notes."}
    ]


def generate_adaptive_quiz(
    material_text: str,
    difficulty: str = "Medium",
    api_key: Optional[str] = None
) -> List[Dict[str, Any]]:
    """
    Calls Gemini API to generate an adaptive quiz strictly in JSON format:
    [{"id": 1, "question": "...", "options": ["A: ...", "B: ...", "C: ...", "D: ..."], "correct_answer": "...", "explanation": "...", "topic_tag": "..."}]
    """
    chunks = chunk_text(material_text)
    context_str = "\n\n".join([f"[{c['header']}]: {c['content']}" for c in chunks[:5]])

    prompt = f"""
You are an educational assessment expert. Based STRICTLY on the course material chunks below, generate a {difficulty} 4-question multiple-choice quiz.

COURSE MATERIAL CONTEXT:
{context_str}

REQUIREMENTS:
- Return ONLY a strict JSON array of objects matching this exact schema:
[
  {{
    "id": 1,
    "question": "Question text...",
    "options": ["A: ...", "B: ...", "C: ...", "D: ..."],
    "correct_answer": "Exact matching option e.g. A: ...",
    "explanation": "Clear explanation citing the course text",
    "topic_tag": "Specific concept tag"
  }}
]
- Exactly 4 options per question (A, B, C, D).
- Do not output markdown code fences.
"""
    api_key = api_key or os.environ.get("GEMINI_API_KEY")
    if api_key:
        try:
            client = genai.Client(api_key=api_key)
            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=prompt,
                config=types.GenerateContentConfig(response_mime_type="application/json")
            )
            quiz = extract_json_from_response(response.text)
            if isinstance(quiz, list) and len(quiz) > 0:
                return quiz
        except Exception as e:
            st.warning(f"Gemini API Notice: {e}. Generating localized RAG quiz questions.")

    # Robust fallback generator
    fallback_quiz = []
    for idx, c in enumerate(chunks[:4]):
        sentences = [s.strip() for s in c["content"].split(".") if len(s.strip()) > 20]
        fact = sentences[0] if sentences else "Standard principle applies under boundary conditions."
        fallback_quiz.append({
            "id": idx + 1,
            "question": f"[{difficulty}] According to the lecture on '{c['header']}', which statement is correct?",
            "options": [
                f"A: {fact}",
                "B: The opposite effect occurs under standard conditions",
                "C: This relation is non-linear and invalid in classical states",
                "D: None of the above interpretations are valid"
            ],
            "correct_answer": f"A: {fact}",
            "explanation": f"Grounded directly in source section '{c['header']}': {fact}",
            "topic_tag": c["header"]
        })
    return fallback_quiz


# ==============================================================================
# STREAMLIT UI: STUDENT VIEW
# ==============================================================================
def render_flashcard_component(cards: List[Dict[str, str]]):
    """Interactive Flip Card UI with 3D animation and navigation."""
    if not cards:
        st.info("No cards found in this deck.")
        return

    if "card_idx" not in st.session_state:
        st.session_state.card_idx = 0
    if "card_flipped" not in st.session_state:
        st.session_state.card_flipped = False

    idx = st.session_state.card_idx
    flipped = st.session_state.card_flipped
    current_card = cards[idx]
    total = len(cards)

    # Progress bar
    st.progress((idx + 1) / total, text=f"Card {idx + 1} of {total}")

    # Interactive 3D Flip Card Container via CSS
    front_text = current_card.get("front", "")
    back_text = current_card.get("back", "")

    card_bg = "#1e293b" if not flipped else "#1e3a8a"
    header_label = "QUESTION / TERM" if not flipped else "ANSWER / EXPLANATION"
    content_text = front_text if not flipped else back_text

    st.markdown(f"""
    <div style="
        background-color: {card_bg};
        border-radius: 18px;
        padding: 32px 24px;
        min-height: 220px;
        box-shadow: 0 8px 24px rgba(0,0,0,0.25);
        color: white;
        text-align: center;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        margin: 16px 0;
        border: 2px solid #3b82f6;
    ">
        <div style="font-size: 12px; font-weight: 700; color: #38bdf8; letter-spacing: 1.5px;">
            {header_label}
        </div>
        <div style="font-size: 20px; font-weight: 600; line-height: 1.5; margin: 20px 0;">
            {content_text}
        </div>
        <div style="font-size: 12px; color: #94a3b8;">
            (Click 'Flip Card' below to reveal {"answer" if not flipped else "question"})
        </div>
    </div>
    """, unsafe_allow_html=True)

    # Controls Row
    col_prev, col_flip, col_next = st.columns([1, 2, 1])
    with col_prev:
        if st.button("⬅️ Previous", disabled=(idx == 0), use_container_width=True):
            st.session_state.card_idx -= 1
            st.session_state.card_flipped = False
            st.rerun()

    with col_flip:
        flip_label = "🔄 Flip to Question" if flipped else "🔄 Flip Card (Show Answer)"
        if st.button(flip_label, use_container_width=True, type="primary"):
            st.session_state.card_flipped = not st.session_state.card_flipped
            st.rerun()

    with col_next:
        if st.button("Next ➡️", disabled=(idx == total - 1), use_container_width=True):
            st.session_state.card_idx += 1
            st.session_state.card_flipped = False
            st.rerun()


def render_quiz_engine(quiz: Dict[str, Any], questions: List[Dict[str, Any]], student_id: str, student_name: str):
    """Presents questions one by one, records answers, and renders RAG explanations."""
    if not questions:
        st.info("No questions in this quiz.")
        return

    q_idx = st.session_state.get("quiz_q_idx", 0)
    total_q = len(questions)

    # Check if quiz completed
    if st.session_state.get("quiz_completed", False):
        score = st.session_state.get("quiz_score", 0)
        pct = int((score / total_q) * 100) if total_q > 0 else 0
        st.success(f"🎉 Quiz Completed! Final Score: **{score} / {total_q} ({pct}%)**")

        st.subheader("Question Review & Grounded Explanations")
        user_answers = st.session_state.get("quiz_answers", {})
        for q in questions:
            qid = q["id"]
            chosen = user_answers.get(qid, "Not Answered")
            is_corr = chosen.strip() == q["correct_answer"].strip()
            with st.expander(f"Q{qid}: {q['question']} — {'✅ Correct' if is_corr else '❌ Missed'}"):
                st.write(f"**Your Choice:** {chosen}")
                st.write(f"**Correct Answer:** {q['correct_answer']}")
                st.info(f"**RAG Grounded Explanation:** {q['explanation']}")
                st.caption(f"Topic: {q['topic_tag']}")

        if st.button("Return to Classroom Hub"):
            st.session_state.active_quiz = None
            st.session_state.quiz_completed = False
            st.session_state.quiz_q_idx = 0
            st.session_state.quiz_answers = {}
            st.session_state.quiz_score = 0
            st.rerun()
        return

    curr_q = questions[q_idx]
    st.progress((q_idx + 1) / total_q, text=f"Question {q_idx + 1} of {total_q} • Level: {quiz.get('difficulty', 'Medium')}")

    st.markdown(f"### Q{curr_q['id']}: {curr_q['question']}")
    st.caption(f"🏷️ Topic Tag: **{curr_q['topic_tag']}**")

    # Radio options
    selected_opt = st.radio(
        "Select your answer:",
        curr_q["options"],
        key=f"q_radio_{curr_q['id']}"
    )

    is_answered = st.session_state.get(f"q_answered_{curr_q['id']}", False)

    if not is_answered:
        if st.button("Submit & Verify Answer", type="primary"):
            st.session_state[f"q_answered_{curr_q['id']}"] = True
            is_correct = selected_opt.strip() == curr_q["correct_answer"].strip()
            if is_correct:
                st.session_state.quiz_score = st.session_state.get("quiz_score", 0) + 1
            if "quiz_answers" not in st.session_state:
                st.session_state.quiz_answers = {}
            st.session_state.quiz_answers[curr_q["id"]] = selected_opt
            st.rerun()
    else:
        chosen = st.session_state.get("quiz_answers", {}).get(curr_q["id"], selected_opt)
        is_corr = chosen.strip() == curr_q["correct_answer"].strip()
        if is_corr:
            st.success("✅ **Correct!**")
        else:
            st.error(f"❌ **Incorrect.** Correct answer is: **{curr_q['correct_answer']}**")

        st.info(f"📖 **RAG Grounded Explanation:**\n\n{curr_q['explanation']}")

        if q_idx < total_q - 1:
            if st.button("Next Question ➡️", type="primary"):
                st.session_state.quiz_q_idx += 1
                st.rerun()
        else:
            if st.button("Finish & Record Score 🏁", type="primary"):
                st.session_state.quiz_completed = True
                # Record to SQLite database
                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                import uuid
                cursor.execute("""
                INSERT INTO quiz_attempts (id, quiz_id, class_code, student_id, student_name, score, total_questions, answers_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    str(uuid.uuid4())[:8],
                    quiz["id"],
                    quiz["class_code"],
                    student_id,
                    student_name,
                    st.session_state.get("quiz_score", 0),
                    total_q,
                    json.dumps(st.session_state.get("quiz_answers", {}))
                ))
                conn.commit()
                conn.close()
                st.rerun()


def main():
    st.set_page_config(
        page_title="EduPulse AI: Student View",
        page_icon="🎓",
        layout="wide",
        initial_sidebar_state="expanded"
    )
    init_db()

    # Sidebar: User Profile & Join Class
    st.sidebar.title("🎓 EduPulse AI")
    st.sidebar.caption("Interactive Classroom & Quiz Studio")

    student_name = st.sidebar.text_input("Student Name", value="Maya Chen")
    student_id = "stu_1"

    st.sidebar.divider()
    st.sidebar.subheader("Join a New Class")
    join_code_input = st.sidebar.text_input("Enter 6-Char Class Code", placeholder="e.g. XJ79K2").strip().upper()
    if st.sidebar.button("Join Class", use_container_width=True):
        if len(join_code_input) == 6:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cur.execute("SELECT title FROM classes WHERE code = ?", (join_code_input,))
            cls_row = cur.fetchone()
            if cls_row:
                import uuid
                cur.execute("""
                INSERT OR IGNORE INTO enrollments (id, class_code, student_id, student_name)
                VALUES (?, ?, ?, ?)
                """, (str(uuid.uuid4())[:8], join_code_input, student_id, student_name))
                conn.commit()
                st.sidebar.success(f"Enrolled in {cls_row[0]}!")
            else:
                st.sidebar.error("Class code not found!")
            conn.close()
            st.rerun()
        else:
            st.sidebar.warning("Class code must be exactly 6 characters.")

    st.sidebar.divider()
    api_key_input = st.sidebar.text_input("Gemini API Key (Optional)", type="password", help="Leave blank for built-in RAG fallback.")
    if api_key_input:
        os.environ["GEMINI_API_KEY"] = api_key_input

    # Query Joined Classes
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("""
    SELECT c.id, c.code, c.title, c.subject, c.educator_name, c.description
    FROM classes c
    JOIN enrollments e ON c.code = e.class_code
    WHERE e.student_id = ?
    """, (student_id,))
    joined_classes = cur.fetchall()

    if not joined_classes:
        st.title("Welcome to EduPulse AI")
        st.info("You haven't joined any classes yet. Use the sidebar to enter a Class Code like **XJ79K2**!")
        conn.close()
        return

    # Class Tabs or Selector
    class_options = {f"{c[2]} ({c[1]})": c for c in joined_classes}
    selected_class_key = st.selectbox("Select Classroom Group:", list(class_options.keys()))
    selected_class = class_options[selected_class_key]
    class_code = selected_class[1]

    st.title(f"📚 {selected_class[2]}")
    st.caption(f"Instructor: **{selected_class[4]}** • Subject: **{selected_class[3]}** • Join Code: `{class_code}`")

    # Fetch materials, flashcards, quizzes
    cur.execute("SELECT id, title, topic, content FROM materials WHERE class_code = ?", (class_code,))
    materials = cur.fetchall()

    cur.execute("SELECT id, title, cards_json FROM flashcard_decks WHERE class_code = ?", (class_code,))
    decks = cur.fetchall()

    cur.execute("SELECT id, title, difficulty, questions_json FROM quizzes WHERE class_code = ?", (class_code,))
    quizzes = cur.fetchall()

    conn.close()

    # Active Session handling
    if st.session_state.get("active_deck"):
        deck_data = st.session_state.active_deck
        st.subheader(f"🎴 Flashcard Studio: {deck_data['title']}")
        if st.button("⬅️ Back to Class Hub"):
            st.session_state.active_deck = None
            st.rerun()
        render_flashcard_component(json.loads(deck_data["cards_json"]))
        return

    if st.session_state.get("active_quiz"):
        quiz_data = st.session_state.active_quiz
        st.subheader(f"📝 Quiz: {quiz_data['title']}")
        render_quiz_engine(
            quiz=quiz_data,
            questions=json.loads(quiz_data["questions_json"]),
            student_id=student_id,
            student_name=student_name
        )
        return

    # Main Workspace Tabs
    tab_materials, tab_flashcards, tab_quizzes, tab_progress = st.tabs([
        f"📖 Reading Materials ({len(materials)})",
        f"🎴 Assigned Flashcards ({len(decks)})",
        f"📝 Assigned Quizzes ({len(quizzes)})",
        "📊 My Performance"
    ])

    with tab_materials:
        if not materials:
            st.write("No reading materials uploaded for this class yet.")
        for m in materials:
            with st.expander(f"📄 {m[1]} (Topic: {m[2]})"):
                st.markdown(m[3])

    with tab_flashcards:
        if not decks:
            st.write("No flashcard decks assigned yet.")
        for d in decks:
            col1, col2 = st.columns([3, 1])
            with col1:
                cards_list = json.loads(d[2])
                st.markdown(f"**{d[1]}** ({len(cards_list)} Cards)")
                st.caption("Interactive 3D Flip Card Deck")
            with col2:
                if st.button(f"Study Deck", key=f"btn_deck_{d[0]}", type="primary", use_container_width=True):
                    st.session_state.active_deck = {"id": d[0], "title": d[1], "cards_json": d[2]}
                    st.session_state.card_idx = 0
                    st.session_state.card_flipped = False
                    st.rerun()
            st.divider()

    with tab_quizzes:
        if not quizzes:
            st.write("No quizzes assigned yet.")
        for q in quizzes:
            col1, col2 = st.columns([3, 1])
            with col1:
                q_list = json.loads(q[3])
                st.markdown(f"**{q[1]}** ({len(q_list)} Questions)")
                st.caption(f"Difficulty: **{q[2]}** • Instant RAG Explanations")
            with col2:
                if st.button(f"Take Quiz", key=f"btn_quiz_{q[0]}", type="primary", use_container_width=True):
                    st.session_state.active_quiz = {"id": q[0], "title": q[1], "difficulty": q[2], "questions_json": q[3], "class_code": class_code}
                    st.session_state.quiz_q_idx = 0
                    st.session_state.quiz_score = 0
                    st.session_state.quiz_completed = False
                    st.session_state.quiz_answers = {}
                    st.rerun()
            st.divider()

    with tab_progress:
        conn = sqlite3.connect(DB_PATH)
        cur = conn.cursor()
        cur.execute("""
        SELECT q.title, qa.score, qa.total_questions, qa.completed_at
        FROM quiz_attempts qa
        JOIN quizzes q ON qa.quiz_id = q.id
        WHERE qa.student_id = ? AND qa.class_code = ?
        ORDER BY qa.completed_at DESC
        """, (student_id, class_code))
        attempts = cur.fetchall()
        conn.close()

        if attempts:
            st.subheader("Your Completed Attempts")
            for att in attempts:
                pct = int((att[1] / att[2]) * 100) if att[2] > 0 else 0
                st.write(f"• **{att[0]}**: {att[1]}/{att[2]} ({pct}%) — *{att[3]}*")
        else:
            st.info("No quizzes completed yet in this class. Take a quiz from the Assigned Quizzes tab!")


if __name__ == "__main__":
    main()
