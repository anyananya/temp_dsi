package org.dsiders.nextstep.engine

import android.util.Log
import org.dsiders.nextstep.ai.AiUnavailableException
import org.dsiders.nextstep.ai.OpenAiClient
import org.dsiders.nextstep.ai.Prompts
import org.dsiders.nextstep.data.GoalEntity
import org.dsiders.nextstep.data.Priority
import org.dsiders.nextstep.data.QuestionLogEntity
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.ReqStatusEntity
import org.dsiders.nextstep.data.Repository
import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.RequirementKind
import org.dsiders.nextstep.data.RoleEntity
import org.dsiders.nextstep.data.Source
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * The next-step engine: S1 intake parser and S2 question picker live here,
 * S3/S4 are in [GapReasoner]. OpenAI is used to read free text and to word
 * questions for each student; when it is unavailable, offline templates take over.
 */
class NextStepEngine(private val repo: Repository, private val ai: OpenAiClient) {

    private val questionCache = ConcurrentHashMap<Long, Pair<String, Question>>()

    val aiConfigured: Boolean get() = ai.isConfigured

    // ==== S1: intake parser =====================================================

    suspend fun runIntake(studentId: Long, resume: ResumeInput?): IntakeResult {
        val ctx = repo.context(studentId)
        val goal = ctx.goal ?: GoalEntity(studentId, "")
        val rolesWithReqs = ctx.allRoles.map { it to repo.requirements(it.id) }
        val reqById = rolesWithReqs.flatMap { it.second }.associateBy { it.id }

        var usedAi = false
        var readback = emptyList<String>()
        var role: RoleEntity? = null
        var resumeSummary = goal.resumeSummary

        try {
            val reply = ai.json(
                Prompts.intakeSystem(ctx.student.language),
                Prompts.intakeUser(ctx, goal.goalText, resume?.text.orEmpty(), rolesWithReqs),
                resume?.imagesBase64Jpeg.orEmpty(),
            )
            usedAi = true
            readback = reply.optJSONArray("understood").strings().take(3)
            role = reply.optLong("role_id", -1).let { id -> ctx.allRoles.firstOrNull { it.id == id } }
            reply.optJSONArray("facts")?.objects()?.take(8)?.forEach { f ->
                val label = f.optString("label").trim()
                val value = f.optString("value").trim()
                if (label.isNotEmpty() && value.isNotEmpty() && label.length < 40) repo.upsertFact(studentId, label, value, Source.STUDENT)
            }
            reply.optJSONArray("evidence")?.objects()?.forEach { e ->
                val req = reqById[e.optLong("requirement_id", -1)] ?: return@forEach
                val status = e.optString("status").toStatus() ?: return@forEach
                if (status == ReqStatus.UNKNOWN || status == ReqStatus.LATER) return@forEach
                val existing = repo.status(studentId, req.id)
                if (existing?.source == Source.CASEWORKER) return@forEach
                val source = if (resume != null && !resume.isEmpty) Source.RESUME else Source.STUDENT
                repo.setStatus(studentId, req.id, status, e.optDouble("confidence", 0.6).toFloat().coerceIn(0.1f, 0.95f),
                    e.optString("evidence").take(160), source)
            }
            resumeSummary = reply.optString("resume_summary").ifBlank { resumeSummary }
        } catch (e: AiUnavailableException) {
            Log.w(TAG, "Intake falling back to offline matching: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Intake reply could not be read", e)
        }

        if (!usedAi) {
            role = matchRoleOffline(goal.goalText, ctx.allRoles)
            readback = listOf(offlineReadback(goal.goalText, role))
        }
        if (readback.isEmpty()) readback = listOf(offlineReadback(goal.goalText, role))

        // Attaching a resume is itself proof that one exists.
        if (resume != null && !resume.isEmpty) {
            rolesWithReqs.flatMap { it.second }.filter { it.isResume() }.forEach {
                val existing = repo.status(studentId, it.id)
                if (existing?.source != Source.CASEWORKER) {
                    repo.setStatus(studentId, it.id, ReqStatus.HAVE, 0.95f, "You attached your resume.", Source.RESUME)
                }
            }
            if (resumeSummary.isBlank()) resumeSummary = "Resume attached."
        }

        repo.saveGoal(
            goal.copy(
                targetRoleId = role?.id, field = role?.field.orEmpty(), readback = readback.joinToString("\n"),
                resumeSummary = resumeSummary, intakeDone = false,
            )
        )
        questionCache.remove(studentId)
        return IntakeResult(readback, role, usedAi)
    }

    private fun matchRoleOffline(text: String, roles: List<RoleEntity>): RoleEntity? {
        val words = text.lowercase()
        return roles.map { role ->
            val keys = (role.keywords.split(',') + role.title.lowercase().split(' '))
                .map { it.trim().lowercase() }.filter { it.length > 2 }
            role to keys.count { Regex("\\b${Regex.escape(it)}").containsMatchIn(words) }
        }.filter { it.second > 0 }.maxByOrNull { it.second }?.first
    }

    private fun offlineReadback(text: String, role: RoleEntity?): String {
        val t = text.lowercase()
        return when {
            role != null -> "You want to work as a ${role.title.lowercase()}"
            "not sure" in t || "don't know" in t || "dont know" in t || t.isBlank() -> "You're not sure yet, and that's fine"
            else -> "You told me what you're aiming for"
        }
    }

    // ==== S2: question picker ===================================================

    suspend fun nextQuestion(studentId: Long): NextQuestion {
        val ctx = repo.context(studentId)
        questionCache[studentId]?.let { (sig, q) -> if (sig == ctx.signature) return NextQuestion.Ask(q) }

        val answered = ctx.log.size
        if (answered >= MAX_QUESTIONS) return finish(ctx)

        // Nothing can be judged until the kind of work is known.
        if (ctx.role == null) {
            if (ctx.log.any { it.attributeKey == FIELD_KEY }) return finish(ctx)
            val q = fieldQuestion(ctx)
            questionCache[studentId] = ctx.signature to q
            return NextQuestion.Ask(q)
        }

        val candidates = QuestionPicker.rank(ctx).filter { it.gain >= QuestionPicker.GAIN_THRESHOLD }
        val top = candidates.firstOrNull() ?: return finish(ctx)
        val estimate = (answered + candidates.size).coerceIn(answered + 1, MAX_QUESTIONS)
        val q = requirementQuestion(ctx, top.req, top.reason, answered + 1, estimate)
        questionCache[studentId] = ctx.signature to q
        return NextQuestion.Ask(q)
    }

    private suspend fun finish(ctx: StudentContext): NextQuestion {
        ctx.goal?.let { if (!it.intakeDone) repo.saveGoal(it.copy(intakeDone = true)) }
        return NextQuestion.Done
    }

    private suspend fun fieldQuestion(ctx: StudentContext): Question {
        val fields = ctx.allRoles.map { it.field }.filter { it.isNotBlank() }.distinct().take(4)
        val options = fields.map { AnswerOption(label = it, field = it, confidence = 1f) } +
            AnswerOption(label = "Something else", freeText = true)
        val dontKnow = AnswerOption(label = "I don't know yet", status = ReqStatus.UNKNOWN, confidence = 0f, dontKnow = true)
        val estimate = 4
        return try {
            val r = ai.json(Prompts.fieldQuestionSystem(ctx.student.language), Prompts.fieldQuestionUser(ctx, fields))
            Question(
                attributeKey = FIELD_KEY,
                text = r.optString("question").ifBlank { "What kind of work are you picturing?" },
                helper = r.optString("helper").ifBlank { "Pick whatever is closest. You don't need the exact job title." },
                banner = "",
                whyAsking = r.optString("why_asking").ifBlank { FIELD_WHY },
                options = options, dontKnowOption = dontKnow, number = 1, estimatedTotal = estimate, personalised = true,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Field question offline: ${e.message}")
            Question(
                FIELD_KEY, "What kind of work are you picturing?",
                "Pick whatever is closest. You don't need the exact job title.", "", FIELD_WHY,
                options, dontKnow, 1, estimate, false,
            )
        }
    }

    private suspend fun requirementQuestion(ctx: StudentContext, req: RequirementEntity, reason: String, number: Int, estimate: Int): Question {
        val previous = ctx.log.lastOrNull { !it.skipped }
        return try {
            val r = ai.json(Prompts.requirementQuestionSystem(ctx.student.language), Prompts.requirementQuestionUser(ctx, req, reason))
            val options = r.optJSONArray("options").objects().mapNotNull { o ->
                val status = o.optString("status").toStatus() ?: return@mapNotNull null
                if (status == ReqStatus.LATER) return@mapNotNull null
                val label = o.optString("label").trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                AnswerOption(label, status, o.optDouble("confidence", 0.7).toFloat().coerceIn(0f, 1f), o.optString("evidence").take(160))
            }.take(4)
            require(options.size >= 2 && options.any { it.status == ReqStatus.HAVE } && options.any { it.status != ReqStatus.HAVE })
            val withNotSure = if (options.any { it.status == ReqStatus.UNKNOWN }) options
            else options + AnswerOption("I'm not sure", ReqStatus.UNKNOWN, 0.2f, "You weren't sure about this yet.")
            Question(
                attributeKey = "req:${req.id}",
                text = r.optString("question").ifBlank { offlineQuestionText(req) },
                helper = r.optString("helper"),
                banner = if (previous == null) "" else r.optString("banner"),
                whyAsking = r.optString("why_asking").ifBlank { req.whyNeeded.ifBlank { reason } },
                options = withNotSure, dontKnowOption = null, number = number, estimatedTotal = estimate, personalised = true,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Question for ${req.name} offline: ${e.message}")
            offlineRequirementQuestion(ctx, req, reason, number, estimate)
        }
    }

    private fun offlineQuestionText(req: RequirementEntity): String {
        val name = req.name.trim().replaceFirstChar { it.lowercase() }
        return when (req.kind) {
            RequirementKind.SKILL -> "Have you learned $name?"
            RequirementKind.DOCUMENT -> "Do you have $name?"
            RequirementKind.ARTEFACT -> if (name.startsWith("build") || name.startsWith("write")) "Have you done this: $name?" else "Do you have $name?"
            RequirementKind.EXPERIENCE -> "Have you done $name?"
        }
    }

    private fun offlineRequirementQuestion(ctx: StudentContext, req: RequirementEntity, reason: String, number: Int, estimate: Int): Question {
        val previous = ctx.log.lastOrNull { !it.skipped }
        val short = req.name.trim().replaceFirstChar { it.lowercase() }
        val banner = previous?.let { "You said \"${it.answer.lowercase()}\", so next I'll ask about $short." }.orEmpty()
        return Question(
            attributeKey = "req:${req.id}",
            text = offlineQuestionText(req),
            helper = listOf(req.whyNeeded, "It's fine if you don't.").filter { it.isNotBlank() }.joinToString(" "),
            banner = banner,
            whyAsking = listOf(reason, req.whyNeeded).filter { it.isNotBlank() }.joinToString(" "),
            options = listOf(
                AnswerOption("Yes, I have this", ReqStatus.HAVE, 0.9f, "You said you have ${short}."),
                AnswerOption("I've started", ReqStatus.PARTIAL, 0.75f, "You said you've started on ${short}."),
                AnswerOption("No, not yet", ReqStatus.MISSING, 0.9f, "You said you don't have ${short} yet."),
                AnswerOption("I'm not sure what counts", ReqStatus.UNKNOWN, 0.2f, "You weren't sure what counts."),
            ),
            dontKnowOption = null, number = number, estimatedTotal = estimate, personalised = false,
        )
    }

    /** Records an answer. [freeText] is what the student typed for "Something else". */
    suspend fun answer(studentId: Long, question: Question, option: AnswerOption, freeText: String = "", source: Source = Source.STUDENT) {
        questionCache.remove(studentId)
        val ctx = repo.context(studentId)
        if (question.attributeKey == FIELD_KEY) {
            val goal = ctx.goal ?: GoalEntity(studentId, "")
            var role: RoleEntity? = null
            var field = ""
            when {
                option.dontKnow -> Unit
                option.freeText -> {
                    field = freeText.trim()
                    role = matchRoleOffline(freeText, ctx.allRoles)
                }
                else -> {
                    field = option.field.orEmpty()
                    // Keep the role the intake found if it is in this field.
                    role = ctx.allRoles.firstOrNull { it.id == goal.targetRoleId && it.field == field }
                        ?: ctx.allRoles.firstOrNull { it.field == field }
                }
            }
            repo.log(
                QuestionLogEntity(
                    studentId = studentId, attributeKey = FIELD_KEY, question = question.text,
                    answer = if (option.freeText) freeText.ifBlank { option.label } else option.label,
                    saidDontKnow = option.dontKnow, prevEvidence = goal.targetRoleId?.toString().orEmpty(),
                )
            )
            repo.saveGoal(goal.copy(targetRoleId = role?.id, field = field))
            return
        }

        val reqId = question.attributeKey.removePrefix("req:").toLongOrNull() ?: return
        val prev = ctx.statuses[reqId]
        repo.log(
            QuestionLogEntity(
                studentId = studentId, attributeKey = question.attributeKey, question = question.text,
                answer = option.label, saidDontKnow = option.dontKnow,
                prevStatus = prev?.status, prevConfidence = prev?.confidence, prevEvidence = prev?.evidence, prevSource = prev?.source,
            )
        )
        val status = option.status ?: ReqStatus.UNKNOWN
        repo.setStatus(studentId, reqId, status, option.confidence, option.evidence.ifBlank { "You answered: ${option.label}." }, source)
    }

    suspend fun skip(studentId: Long, question: Question) {
        questionCache.remove(studentId)
        repo.log(QuestionLogEntity(studentId = studentId, attributeKey = question.attributeKey, question = question.text, answer = "Skipped", skipped = true))
    }

    /** Undoes the last answer. Returns false when there was nothing to undo. */
    suspend fun undo(studentId: Long): Boolean {
        questionCache.remove(studentId)
        val last = repo.questionLog(studentId).lastOrNull() ?: return false
        repo.deleteLog(last)
        if (last.attributeKey == FIELD_KEY) {
            repo.goal(studentId)?.let { repo.saveGoal(it.copy(targetRoleId = last.prevEvidence?.toLongOrNull(), intakeDone = false)) }
        } else if (!last.skipped) {
            val reqId = last.attributeKey.removePrefix("req:").toLongOrNull() ?: return true
            val previous = last.prevStatus?.let {
                ReqStatusEntity(studentId, reqId, it, last.prevConfidence ?: 0.5f, last.prevEvidence.orEmpty(), last.prevSource ?: Source.STUDENT)
            }
            repo.restoreStatus(studentId, reqId, previous)
        }
        repo.goal(studentId)?.let { if (it.intakeDone) repo.saveGoal(it.copy(intakeDone = false)) }
        return true
    }

    // ==== S3/S4 ================================================================

    suspend fun analyse(studentId: Long): GapResult = GapReasoner.analyse(repo.context(studentId))

    suspend fun analyseForRole(studentId: Long, roleId: Long): GapResult = GapReasoner.analyse(repo.contextForRole(studentId, roleId))

    /** "I've done this": one more thing known, so the engine can run again. */
    suspend fun markDone(studentId: Long, reqId: Long, source: Source = Source.STUDENT) {
        questionCache.remove(studentId)
        val evidence = if (source == Source.CASEWORKER) "Your mentor marked this done." else "You marked this done."
        repo.setStatus(studentId, reqId, ReqStatus.HAVE, if (source == Source.CASEWORKER) 1f else 0.85f, evidence, source)
        repo.goal(studentId)?.let { repo.saveGoal(it.copy(intakeDone = false)) }
    }

    fun invalidate(studentId: Long) {
        questionCache.remove(studentId)
    }

    // ==== Guidance ==============================================================

    suspend fun howTo(studentId: Long, req: RequirementEntity): HowToGuide {
        val ctx = repo.context(studentId)
        return try {
            val r = ai.json(Prompts.howToSystem(ctx.student.language), Prompts.howToUser(ctx, req))
            val steps = r.optJSONArray("steps").strings().take(6)
            require(steps.isNotEmpty())
            HowToGuide(steps, r.optString("tip"), true)
        } catch (e: Exception) {
            Log.w(TAG, "How-to offline: ${e.message}")
            val steps = req.howTo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (req.whatYouNeed.isNotBlank()) steps.add(0, "Get these ready: ${req.whatYouNeedList().joinToString(", ")}.")
            if (req.whereToGetHelp.isNotBlank()) steps += "If you get stuck: ${req.whereToGetHelp}."
            if (steps.isEmpty()) steps += "Ask your Barabari mentor how to start on this."
            HowToGuide(steps, "One step at a time is enough.", false)
        }
    }

    data class RoleDraft(
        val field: String,
        val description: String,
        val keywords: String,
        val requirements: List<DraftRequirement>,
    )

    data class DraftRequirement(
        val name: String, val kind: RequirementKind, val priority: Priority, val why: String,
        val time: String, val need: List<String>, val how: String, val dependsOnNames: List<String>,
    )

    /** Drafts a role's requirements for an NGO worker to review. Throws if OpenAI is unavailable. */
    suspend fun draftRole(title: String, notes: String): RoleDraft {
        val r = ai.json(Prompts.ROLE_DRAFT_SYSTEM, Prompts.roleDraftUser(title, notes))
        val reqs = r.optJSONArray("requirements").objects().mapNotNull { o ->
            val name = o.optString("name").trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            DraftRequirement(
                name = name,
                kind = runCatching { RequirementKind.valueOf(o.optString("kind").uppercase()) }.getOrDefault(RequirementKind.SKILL),
                priority = if (o.optString("priority").equals("LATER", true)) Priority.LATER else Priority.CORE,
                why = o.optString("why"), time = o.optString("time"),
                need = o.optJSONArray("need").strings(), how = o.optString("how"),
                dependsOnNames = o.optJSONArray("depends_on").strings(),
            )
        }
        return RoleDraft(r.optString("field"), r.optString("description"), r.optString("keywords"), reqs)
    }

    companion object {
        private const val TAG = "NextStepEngine"
        const val FIELD_KEY = "field"
        const val MAX_QUESTIONS = 6
        private const val FIELD_WHY = "Nothing else can be judged until we know the kind of work you want. " +
            "It's fine to say you don't know yet; a mentor can help you choose."
    }
}

private fun RequirementEntity.isResume() = Regex("\\b(resume|cv)\\b", RegexOption.IGNORE_CASE).containsMatchIn(name)

private fun String.toStatus(): ReqStatus? = runCatching { ReqStatus.valueOf(trim().uppercase()) }.getOrNull()

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).trim().takeIf { s -> s.isNotEmpty() } }

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
