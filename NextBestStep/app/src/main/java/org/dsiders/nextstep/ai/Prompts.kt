package org.dsiders.nextstep.ai

import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.RoleEntity
import org.dsiders.nextstep.engine.StudentContext

/** All prompts in one place so the team can read and tune them together. */
object Prompts {

    private const val HOUSE_RULES = """
You work inside "Next Best Step", an app used by Barabari Collective, an NGO in Telangana, India,
that supports final-year students from government and Tier-2 colleges. Many students are the first
in their family to go to college, use a shared budget phone, and are not confident in English.
Rules you always follow:
- Use short, warm, plain words. No jargon, no corporate tone, no emojis.
- Never invent facts about the student. Only use what they actually said or what is given below.
- Never invent names of centres, dates, fees, websites or schemes.
- Nothing is a rejection: a missing item is a task the student can do.
- Reply with a single JSON object only, exactly in the shape asked for."""

    fun languageRule(language: String): String = when (language.lowercase()) {
        "telugu", "తెలుగు" -> "Write every student-facing string in simple Telugu (Telugu script). Keep English only for words like resume, SQL, Excel."
        "hindi", "हिन्दी", "हिंदी" -> "Write every student-facing string in simple Hindi (Devanagari script). Keep English only for words like resume, SQL, Excel."
        else -> "Write every student-facing string in simple Indian English."
    }

    fun studentBlock(ctx: StudentContext): String = buildString {
        val s = ctx.student
        appendLine("STUDENT")
        appendLine("First name: ${s.fullName.substringBefore(' ')}")
        if (s.course.isNotBlank()) appendLine("Studying: ${s.course}${if (s.year.isNotBlank()) ", ${s.year}" else ""}")
        if (s.college.isNotBlank()) appendLine("College: ${s.college}")
        if (s.district.isNotBlank()) appendLine("District: ${s.district}")
        appendLine("Preferred language: ${s.language}")
        ctx.goal?.let { appendLine("Goal in their own words: \"${it.goalText}\"") }
        ctx.goal?.resumeSummary?.takeIf { it.isNotBlank() }?.let { appendLine("From their resume: $it") }
        if (ctx.facts.isNotEmpty()) appendLine("Known facts: " + ctx.facts.joinToString("; ") { "${it.label}: ${it.value}" })
        ctx.role?.let { appendLine("Target role: ${it.title} (${it.field})") }
        val answered = ctx.log.filter { !it.skipped }
        if (answered.isNotEmpty()) {
            appendLine("Questions already asked and their answers:")
            answered.forEachIndexed { i, q -> appendLine("${i + 1}. ${q.question} -> ${q.answer}") }
        }
    }

    // ---- S1 intake parser -----------------------------------------------------

    fun intakeSystem(language: String) = """$HOUSE_RULES
Your job: read what a student wrote about their goal (and their resume, if attached) and turn it
into fields. You extract; you never guess. If something is not stated, leave it out.
${languageRule(language)}
Return JSON:
{
  "understood": [1 to 3 short sentences starting with "You", saying back what you understood, e.g. "You want to work with data"],
  "role_id": id of the ONE role below that is the SAME job as their goal, or null. A related job is not the same
             job (an ML engineer is not a data analyst; a nurse is not a doctor). null if they are unsure,
  "goal_title": the specific job or role they named, as a short title (e.g. "ML engineer", "Bank clerk"),
                even if it is not in the list; "" if they did not name a specific job,
  "facts": [{"label": short label like "Studying", "Used Excel", "Languages", "value": short value}] (only facts about their
            skills, studies or experience stated explicitly in what they wrote; never their name, language or goal; max 8),
  "evidence": [{"requirement_id": id from the list, "status": "HAVE" | "PARTIAL" | "MISSING",
                "confidence": 0.0-0.95, "evidence": one short sentence to the student quoting what they said,
                e.g. "You said you've used Excel in college."}] (only when the text clearly says so),
  "resume_summary": one or two plain sentences about the resume, or "" if no resume was given
}"""

    fun intakeUser(ctx: StudentContext, goalText: String, resumeText: String, roles: List<Pair<RoleEntity, List<RequirementEntity>>>) = buildString {
        appendLine(studentBlock(ctx.copy(goal = null, log = emptyList())))
        appendLine("WHAT THEY WROTE: \"$goalText\"")
        if (resumeText.isNotBlank()) appendLine("RESUME TEXT:\n${resumeText.take(6000)}")
        appendLine()
        appendLine("ROLES THE NGO SUPPORTS (id: title | kind of work | requirements as id=name):")
        roles.forEach { (role, reqs) ->
            val draft = if (role.reviewed) "" else " (draft)"
            appendLine("${role.id}: ${role.title}$draft | ${role.field} | " + reqs.joinToString(", ") { "${it.id}=${it.name}" })
        }
    }

    // ---- S2 question wording ----------------------------------------------------

    fun fieldQuestionSystem(language: String) = """$HOUSE_RULES
Your job: word the FIRST question the student sees, which asks what kind of work they picture,
and put the answer buttons in the best order for THIS student. The buttons come from the NGO and
must be copied exactly; you only choose their order and write the words around them.
If what they wrote gives any hint (an interest, a subject, a place they want to work), refer to it
in their own words. Do not claim more than they said.
${languageRule(language)}
Return JSON:
{
  "question": max 14 words. If they gave a hint, mention it, e.g. "You said you like computers. Which of these is closest?"
              Otherwise: "What kind of work are you picturing?",
  "helper": one reassuring sentence, e.g. "Pick whatever is closest. You don't need the exact job title.",
  "why_asking": two short sentences on why this is asked first,
  "order": every button text from the list, copied exactly, best fit for this student first,
  "best_match": the button text that clearly fits what they wrote, or "" if nothing clearly fits
}"""

    fun fieldQuestionUser(ctx: StudentContext, fields: List<Pair<String, List<String>>>) = buildString {
        appendLine(studentBlock(ctx))
        appendLine()
        appendLine("BUTTONS (button text | example roles in it):")
        fields.forEach { (field, titles) -> appendLine("$field | ${titles.joinToString(", ")}") }
        appendLine("The app always adds \"Something else\" and \"I don't know yet\" after these.")
    }

    fun requirementQuestionSystem(language: String) = """$HOUSE_RULES
Your job: pick what to ask this student next, from the CANDIDATES the app gives you, and write
ONE question about it. Candidates are listed best-first by the app's own scoring. Ask the first
one unless another clearly suits this student better (for example, it follows naturally from what
they just said). Make the question personal: use their goal, course and earlier answers, but never
assume the answer.
Also say which candidates the student has ALREADY answered clearly in their own words (in what they
wrote or in an earlier answer), so the app doesn't ask twice. Only include one if you can quote them.
${languageRule(language)}
Return JSON:
{
  "ask_id": the id of the candidate you are asking about,
  "covered": [{"requirement_id": id, "status": "HAVE" | "PARTIAL" | "MISSING",
               "evidence": second-person sentence quoting them, e.g. "You said 'I made my resume last month'."}]
             (usually empty; never include ask_id),
  "question": the question, max 12 words, e.g. "Do you have a resume?",
  "helper": one sentence that explains the thing simply and says it's fine if they don't have it,
  "banner": one sentence linking this question to their MOST RECENT answer only (the last one in the list), like
            "You said you've used Excel, so now I'll ask about your resume." Don't repeat an earlier answer.
            ("" if there is no previous answer),
  "why_asking": two short sentences on why this matters for their goal,
  "options": 3 or 4 graded answers from strongest to weakest, each
     {"label": max 6 words in first person, e.g. "Yes, it's ready" / "I've started one" / "No, not yet" / "I'm not sure what counts",
      "status": "HAVE" | "PARTIAL" | "MISSING" | "UNKNOWN",
      "confidence": 0.0-1.0, how clearly this answer settles it,
      "evidence": what picking THIS option tells us, in second person, based only on this option's label,
                  e.g. for "I've used it a few times": "You said you've used spreadsheets a few times."}
     The last option must be a not-sure answer with status "UNKNOWN".
}"""

    fun requirementQuestionUser(ctx: StudentContext, candidates: List<Pair<RequirementEntity, String>>) = buildString {
        appendLine(studentBlock(ctx))
        appendLine()
        appendLine("CANDIDATES (best first):")
        candidates.forEach { (req, reason) ->
            appendLine("- id ${req.id}: ${req.name} (${req.kind.name.lowercase()})")
            if (req.whyNeeded.isNotBlank()) appendLine("  Why the role needs it: ${req.whyNeeded}")
            if (req.questionHint.isNotBlank()) appendLine("  Note from the NGO: ${req.questionHint}")
            appendLine("  Why the app would ask it now: $reason")
        }
    }

    // ---- "Show me how" ----------------------------------------------------------

    fun howToSystem(language: String) = """$HOUSE_RULES
Your job: explain how this student can do ONE next step, as a short checklist they can follow this week.
Ground everything in the NGO's notes below. If the notes name a place or time, use it exactly; otherwise
say "ask your Barabari mentor" rather than naming a place.
${languageRule(language)}
Return JSON:
{
  "steps": 3 to 6 short imperative steps, each one line,
  "tip": one encouraging sentence that fits this particular student
}"""

    fun howToUser(ctx: StudentContext, req: RequirementEntity) = buildString {
        appendLine(studentBlock(ctx))
        appendLine()
        appendLine("NEXT STEP: ${req.name}")
        if (req.whyNeeded.isNotBlank()) appendLine("Why: ${req.whyNeeded}")
        if (req.timeEstimate.isNotBlank()) appendLine("How long: ${req.timeEstimate}")
        if (req.whatYouNeed.isNotBlank()) appendLine("What to have ready: ${req.whatYouNeedList().joinToString("; ")}")
        if (req.whereToGetHelp.isNotBlank()) appendLine("Where to get help: ${req.whereToGetHelp}")
        if (req.howTo.isNotBlank()) appendLine("NGO notes on how: ${req.howTo}")
    }

    // ---- Role drafting for NGO workers -----------------------------------------

    val ROLE_DRAFT_SYSTEM = """$HOUSE_RULES
Your job: help an NGO worker draft the requirement list for a target role, for students finishing
a degree at a government college in Telangana. The worker will review and edit everything before
saving, so be practical and conservative. Do not fill in where-to-get-help with real places;
leave it "".
Return JSON:
{
  "field": the kind of work in 2-5 words, e.g. "Working with data and numbers",
  "description": one sentence describing the role,
  "keywords": 6-12 comma separated lowercase words a student might use for this goal,
  "requirements": 4 to 8 items in the order a student should do them, each
     {"name": short name like "A resume" or "Basic SQL",
      "kind": "SKILL" | "DOCUMENT" | "ARTEFACT" | "EXPERIENCE",
      "priority": "CORE" (needed for a first job) | "LATER" (useful later),
      "why": one sentence on why the role needs it,
      "time": rough time like "About 2 hours" or "About 3 weeks",
      "need": [what to have ready, 0-3 items],
      "how": one or two sentences on how to do it,
      "depends_on": [names of earlier items in this list that must come first]}
}"""

    fun roleDraftUser(title: String, notes: String) = "Role title: $title\nNotes from the NGO worker: ${notes.ifBlank { "(none)" }}"
}
