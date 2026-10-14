package org.dsiders.nextstep.engine

import org.dsiders.nextstep.data.FactEntity
import org.dsiders.nextstep.data.GoalEntity
import org.dsiders.nextstep.data.Priority
import org.dsiders.nextstep.data.QuestionLogEntity
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.ReqStatusEntity
import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.RoleEntity
import org.dsiders.nextstep.data.Source
import org.dsiders.nextstep.data.UserEntity

/** Everything the engine knows about one student at one moment. */
data class StudentContext(
    val student: UserEntity,
    val goal: GoalEntity?,
    val role: RoleEntity?,
    val requirements: List<RequirementEntity>,
    val statuses: Map<Long, ReqStatusEntity>,
    val facts: List<FactEntity>,
    val log: List<QuestionLogEntity>,
    val allRoles: List<RoleEntity>,
) {
    /** Requirements that matter now: core priority and not pushed to "later" for this student. */
    val coreRequirements: List<RequirementEntity>
        get() = requirements.filter { it.priority == Priority.CORE && statuses[it.id]?.status != ReqStatus.LATER }

    fun statusOf(req: RequirementEntity): ReqStatus = statuses[req.id]?.status ?: ReqStatus.UNKNOWN
    fun confidenceOf(req: RequirementEntity): Float = statuses[req.id]?.confidence ?: 0f

    val saidDontKnow: Boolean get() = log.any { it.saidDontKnow }
    val questionsAnswered: Int get() = log.count { !it.skipped }

    /** A short signature that changes whenever an answer or status changes. */
    val signature: String
        get() = "${goal?.targetRoleId}|${goal?.goalText?.hashCode()}|${log.size}|" +
            statuses.values.sortedBy { it.requirementId }.joinToString(",") { "${it.requirementId}:${it.status}:${it.confidence}" }
}

/** One tappable answer on a question card. */
data class AnswerOption(
    val label: String,
    val status: ReqStatus? = null,
    val confidence: Float = 0.5f,
    val evidence: String = "",
    /** For the kind-of-work question: the field this answer picks. */
    val field: String? = null,
    val dontKnow: Boolean = false,
    /** "Something else": the student types their own answer. */
    val freeText: Boolean = false,
    /** Best fit for what the student wrote; shown with a "Closest to what you said" tag. */
    val suggested: Boolean = false,
)

data class Question(
    val attributeKey: String,
    val text: String,
    val helper: String,
    /** Explains why this question follows the last one ("You said ..., so ..."). */
    val banner: String,
    val whyAsking: String,
    val options: List<AnswerOption>,
    /** Shown apart from the other answers, like "I don't know yet" in frame 3. */
    val dontKnowOption: AnswerOption?,
    val number: Int,
    val estimatedTotal: Int,
    val personalised: Boolean,
)

sealed interface NextQuestion {
    data class Ask(val question: Question) : NextQuestion
    data object Done : NextQuestion
}

enum class ItemGroup { HAVE, MISSING, LATER }

data class GapItem(
    val requirement: RequirementEntity,
    val status: ReqStatus,
    val group: ItemGroup,
    val reason: String,
    val confidence: Float,
    val source: Source?,
)

enum class StepState { DONE, CURRENT, UPCOMING }

data class PathStep(val requirement: RequirementEntity, val state: StepState)

data class GapResult(
    val role: RoleEntity?,
    val have: List<GapItem>,
    val missing: List<GapItem>,
    val later: List<GapItem>,
    val nextStep: RequirementEntity?,
    val path: List<PathStep>,
    val confidence: Float,
    val needsHandover: Boolean,
    /** One line for the caseworker list when this student asks for help. */
    val handoverReason: String,
    val questionsAsked: Int,
) {
    val allItems: List<GapItem> get() = have + missing + later

    /** Share of the role's core steps the student already has (0..1). */
    val readiness: Float get() = (have.size + missing.size).let { if (it == 0) 0f else have.size.toFloat() / it }

    /**
     * How sure the app is about [readiness] (0..1): the average confidence across every core
     * step, where a step nobody has asked about counts as a weak guess. Capped for roles
     * Barabari hasn't reviewed yet.
     */
    val assessmentConfidence: Float
        get() {
            val items = have + missing
            if (items.isEmpty()) return 0f
            val avg = items.map { it.confidence }.average().toFloat()
            return if (role?.reviewed == false) minOf(avg, GapReasoner.UNREVIEWED_CONFIDENCE_CAP) else avg
        }

    /** Core steps we have no answer for yet. */
    val unknownCount: Int get() = missing.count { it.status == org.dsiders.nextstep.data.ReqStatus.UNKNOWN }
    val readyForRole: Boolean get() = role != null && nextStep == null && missing.isEmpty()
    val doneCount: Int get() = path.count { it.state == StepState.DONE }
}

data class ResumeInput(val text: String = "", val imagesBase64Jpeg: List<String> = emptyList()) {
    val isEmpty: Boolean get() = text.isBlank() && imagesBase64Jpeg.isEmpty()
}

data class IntakeResult(val readback: List<String>, val role: RoleEntity?, val usedAi: Boolean)

data class HowToGuide(val steps: List<String>, val tip: String, val personalised: Boolean)
