# Next Best Step (Android)

Project 01B, *Finding the Next Best Step*, by Team DSIders, built for Barabari Collective.

A student says what they want to become, answers a few questions written for them, and gets **one concrete next step**.
When the app isn't sure, it hands the student to a named Barabari caseworker instead of guessing.
Caseworkers see who asked for help, can override any judgement, and manage the roles and job openings the app reasons over.

The UI follows the Figma prototype in *Assignment 11, Final UI Design Document* (frames 0 to 11).

---

## Setup

1. Open the `NextBestStep/` folder in Android Studio (Ladybug or newer). Let Gradle sync.
2. Add your OpenAI key to `NextBestStep/local.properties` (Android Studio creates the file; it is git-ignored):
   ```properties
   OPENAI_API_KEY=sk-...
   # optional
   OPENAI_MODEL=gpt-4.1-mini
   ```
   See `local.properties.example`. The key is compiled into the app at build time, so **rebuild after changing it**.
3. Run on a phone or emulator (Android 8.0 / API 26 or newer).

Without a key the app still works: it falls back to standard template questions and keyword matching.
Caseworkers can check the connection under **Profile → ⚙ → Test connection**.

### Sample accounts (debug builds)

| Role | Username | Password |
|------|----------|----------|
| Student | `priya` (also `sana`, `arjun`, `kiran`, `divya`, `mahesh`) | `barabari123` |
| Caseworker | `ravi` (available), `lakshmi` (away) | `barabari123` |

The sample roles, requirements, openings and students mirror the sample data in the Figma.
**Barabari staff should review the roles under the Jobs tab before using the app with real students.**

---

## What's in the app

### Student path (bottom bar: Home · My path · Help · Profile)

| Frame | Screen | Notes |
|-------|--------|-------|
| 0 / 0b | Log in / Create account | Student / Caseworker switch; password ≥ 8 characters; terms checkbox |
| 1 | Goal entry | One sentence, starter chips taken from the NGO's roles, optional resume (PDF, photo or text) |
| 2 | Reading it back | Shows what was understood while the first question is prepared |
| 3 | First question | Kind of work (from the NGO's roles), "Something else" free text, "I don't know yet" |
| 4 | Follow-up question | Personalised wording, a "You said …, so …" banner, "Why are you asking this?" |
| 5 | Where you stand | Have / Missing / Not worth worrying about yet, each with a reason; tap to correct; mentor card when unsure |
| 6 | Your next step | One action, what to bring, how long, where to get help; *Show me how*, *I've done this* |
| 7 | Your path | Done steps, *You are here*, faded later steps, goal pinned at the bottom |
| 8 | My profile | Everything held about the student, all editable; language, mentor, privacy, *Download my details* |
| — | Help | Message a mentor, see replies |
| — | Openings | Shown on Home with "you have 2 of 4 things"; *Make this my goal* |

### Caseworker path (bottom bar: Students · Requests · Jobs · Profile)

| Frame | Screen | Notes |
|-------|--------|-------|
| 9 | My students | *Asked for help* first with a one-line reason; filters; search; *Add a student* |
| 10 | Student detail | Same reasoning, compact; **Override** on every row; next step via WhatsApp / Print; notes; session timer; reply to requests |
| 11 | Caseworker profile | *Available to help* switch (controls whether students are offered this person), today's numbers, topics, languages, hours |
| — | Requests | All help requests, new first, with a badge on the tab |
| — | Jobs | **Roles** (what a goal needs, as ordered steps with prerequisites) and **Openings** (real vacancies linked to a role). *Suggest steps* drafts a role's requirements with AI for the worker to review. |
| — | Add a student | Caseworker-led intake: create the account and go straight into the questions together |

---

## How the engine works

It follows the four services from the Sprint 1 design document (`app/src/main/java/org/dsiders/nextstep/engine/`):

| Service | File | Uses OpenAI? |
|---------|------|--------------|
| **S1 Intake parser**: goal sentence or resume into fields, explicit evidence only | `NextStepEngine.runIntake` | Yes, with keyword matching as the fallback |
| **S2 Question picker**: ranks unknowns by how much they'd change the next step, stops when gain is below threshold | `QuestionPicker` | No. Only the **wording** is personalised by OpenAI |
| **S3 Gap reasoner**: compares the student with the role's requirement graph and finds the nearest reachable step | `GapReasoner.analyse` | **No, deterministic** |
| **S4 Confidence gate**: below 70 % confidence, or after "I don't know", offers a named mentor | `GapReasoner.analyse` | No |

**Division of labour:** the language model turns text into fields and phrases questions in the student's language (English, Telugu or Hindi). It never decides what a student is missing. Every gap comes from a check against the requirement graph the NGO maintains. All prompts are in `ai/Prompts.kt`.

Personalisation: each question is written using the student's own goal sentence, course, college, earlier answers and preferred language, so two students asked about the same requirement get different questions.

Engine tests: `./gradlew :app:testDebugUnitTest` (runs the Sprint 1 worked example, prerequisites, handover rules and overrides).

---

## Design decisions to review

- **Data is stored on the phone** (Room / SQLite). Students and caseworkers share one device, for example at a centre. A student on one phone can't reach a caseworker on another. Moving to a shared backend later only means replacing `data/Repository.kt`.
- **The OpenAI key is inside the APK.** That's fine for a demo or pilot, but anyone with the APK can extract it. Before a wider rollout, route calls through a small server or Firebase Function. The only place to change is `ai/OpenAiClient.kt` (`OPENAI_BASE_URL` already supports a proxy).
- **A fourth caseworker tab, "Jobs", was added.** The Figma shows three tabs; managing roles and openings needed a home.
- **Login was added**, as Frames 0 / 0b describe. Anyone can create a caseworker account on the device; add an access code if that's a concern.
- Fixed UI text is in English. Questions, guidance and readbacks follow the student's language setting.
- No colour means rejection: missing items are amber and nothing is red, as the design specifies.

## Project layout

```
app/src/main/java/org/dsiders/nextstep/
  ai/          OpenAI client + all prompts
  data/        Room entities, DAOs, repository, seed data, session
  engine/      Intake, question picker, gap reasoner, resume reader
  ui/          Theme (Figma colours, Plus Jakarta Sans + Noto Sans), components, screens
    auth/  student/  caseworker/
```

Fonts: Plus Jakarta Sans and Noto Sans, both under the SIL Open Font License.
