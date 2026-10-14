package org.dsiders.nextstep.engine

import org.dsiders.nextstep.data.Priority
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.RequirementEntity

/**
 * S3 gap reasoner + S4 confidence gate.
 *
 * Fully deterministic: it compares what the student has against the role's
 * requirement graph. The language model is never asked what someone is missing.
 */
object GapReasoner {
    /** Below this the result is shown with a mentor offer instead of on its own. */
    const val CONFIDENCE_THRESHOLD = 0.7f
    /** Confidence used for a requirement nobody has asked about yet. */
    private const val UNASKED_CONFIDENCE = 0.3f
    /** A status this unsure is treated as not yet known. */
    const val SETTLED_CONFIDENCE = 0.6f

    fun analyse(ctx: StudentContext): GapResult {
        val role = ctx.role
        if (role == null) {
            return GapResult(
                role = null, have = emptyList(), missing = emptyList(), later = emptyList(),
                nextStep = null, path = emptyList(), confidence = 0f, needsHandover = true,
                handoverReason = "Asked to talk to someone", questionsAsked = ctx.questionsAnswered,
            )
        }

        val core = topoOrder(ctx.coreRequirements)
        val have = mutableListOf<GapItem>()
        val missing = mutableListOf<GapItem>()
        val later = mutableListOf<GapItem>()

        for (req in ctx.requirements) {
            val st = ctx.statuses[req.id]
            val status = st?.status ?: ReqStatus.UNKNOWN
            val isLater = req.priority == Priority.LATER || status == ReqStatus.LATER
            val item = when {
                isLater -> GapItem(req, status, ItemGroup.LATER, req.whyNeeded.ifBlank { "You can leave this for later." }, 1f, st?.source)
                status == ReqStatus.HAVE -> GapItem(
                    req, status, ItemGroup.HAVE, st?.evidence?.ifBlank { null } ?: "Marked as done.", st?.confidence ?: 1f, st?.source,
                )
                else -> GapItem(req, status, ItemGroup.MISSING, missingReason(req.whyNeeded, status), st?.confidence ?: UNASKED_CONFIDENCE, st?.source)
            }
            when (item.group) {
                ItemGroup.HAVE -> have += item
                ItemGroup.MISSING -> missing += item
                ItemGroup.LATER -> later += item
            }
        }
        val order = core.withIndex().associate { it.value.id to it.index }
        missing.sortBy { order[it.requirement.id] ?: Int.MAX_VALUE }

        val haveIds = have.map { it.requirement.id }.toSet()
        val coreIds = core.map { it.id }.toSet()
        // Nearest reachable node: first unmet core requirement whose own prerequisites are met.
        val nextStep = core.firstOrNull { req ->
            req.id !in haveIds && req.dependsOnIds().filter { it in coreIds }.all { it in haveIds }
        } ?: core.firstOrNull { it.id !in haveIds }

        val path = core.map { req ->
            PathStep(
                req,
                when {
                    req.id in haveIds -> StepState.DONE
                    req.id == nextStep?.id -> StepState.CURRENT
                    else -> StepState.UPCOMING
                },
            )
        }

        // S4: confidence over the part of the picture that decides the next step.
        val decisive = if (nextStep == null) core else core.take(core.indexOf(nextStep) + 1)
        val confidence = if (decisive.isEmpty()) 1f else decisive.map { req ->
            val st = ctx.statuses[req.id]
            when {
                st == null -> UNASKED_CONFIDENCE
                st.status == ReqStatus.UNKNOWN -> minOf(st.confidence, UNASKED_CONFIDENCE)
                else -> st.confidence
            }
        }.average().toFloat()

        val lowConfidenceItem = missing.firstOrNull { it.confidence < SETTLED_CONFIDENCE }
        val needsHandover = confidence < CONFIDENCE_THRESHOLD || ctx.saidDontKnow
        val reason = when {
            ctx.saidDontKnow && nextStep == null -> "Asked to talk to someone"
            confidence < CONFIDENCE_THRESHOLD && lowConfidenceItem != null -> "Not sure about: ${lowConfidenceItem.requirement.name.lowercase()}"
            nextStep != null -> "Stuck: ${stuckPhrase(nextStep.name)}"
            else -> "Ready to apply, wants a check"
        }

        return GapResult(
            role = role, have = have, missing = missing, later = later, nextStep = nextStep, path = path,
            confidence = confidence, needsHandover = needsHandover, handoverReason = reason,
            questionsAsked = ctx.questionsAnswered,
        )
    }

    private fun missingReason(why: String, status: ReqStatus): String {
        val tail = when (status) {
            ReqStatus.MISSING -> "You don't have it yet."
            ReqStatus.PARTIAL -> "You've started; finishing it is the step."
            else -> "We're not sure yet whether you have this."
        }
        return listOf(why.trim(), tail).filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** "A resume" -> "no resume yet". */
    fun stuckPhrase(name: String): String {
        val n = name.trim().lowercase().removePrefix("a ").removePrefix("an ").removePrefix("the ")
        return "no $n yet"
    }

    /** Orders requirements so prerequisites come first, keeping the NGO's order otherwise. */
    fun topoOrder(reqs: List<RequirementEntity>): List<RequirementEntity> {
        val byId = reqs.associateBy { it.id }
        val sorted = reqs.sortedWith(compareBy({ it.orderIndex }, { it.id }))
        val placed = LinkedHashSet<Long>()
        val visiting = HashSet<Long>()
        fun visit(req: RequirementEntity) {
            if (req.id in placed || !visiting.add(req.id)) return // already placed, or a cycle
            req.dependsOnIds().mapNotNull { byId[it] }.sortedBy { it.orderIndex }.forEach { visit(it) }
            visiting.remove(req.id)
            placed += req.id
        }
        sorted.forEach { visit(it) }
        return placed.map { byId.getValue(it) }
    }
}
