package org.dsiders.nextstep.engine

import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.RequirementEntity

/**
 * S2 question picker. Decides *what* to ask; the wording is personalised separately.
 *
 * Ranks what we don't know yet by how much an answer would change the next step.
 * The requirement that decides the very next step scores highest, and one that
 * other steps depend on gets a bonus. Questions stop once nothing scores above
 * [GAIN_THRESHOLD], which in practice is three or four taps.
 */
object QuestionPicker {
    const val GAIN_THRESHOLD = 0.5

    data class Candidate(val req: RequirementEntity, val gain: Double, val reason: String)

    fun rank(ctx: StudentContext): List<Candidate> {
        val core = GapReasoner.topoOrder(ctx.coreRequirements)
        if (core.isEmpty()) return emptyList()
        val asked = ctx.log.map { it.attributeKey }.toSet()
        val settled = { r: RequirementEntity ->
            val st = ctx.statuses[r.id]
            st != null && st.status != ReqStatus.UNKNOWN && st.confidence >= GapReasoner.SETTLED_CONFIDENCE
        }
        // Index of the first step the student hasn't clearly done: the one that decides the next step.
        val frontier = core.indexOfFirst { !(settled(it) && ctx.statusOf(it) == ReqStatus.HAVE) }
            .let { if (it < 0) core.size else it }
        return core.withIndex().mapNotNull { (i, req) ->
            if ("req:${req.id}" in asked || settled(req)) return@mapNotNull null
            val st = ctx.statuses[req.id]
            val uncertainty = if (st == null) 1.0 else (1.0 - st.confidence).coerceIn(0.0, 1.0)
            val position = if (i <= frontier) 1.0 else 0.35
            val dependents = core.count { req.id in it.dependsOnIds() }
            val gain = uncertainty * position * (1 + 0.2 * dependents)
            val reason = if (i <= frontier) "This decides your very next step." else "This helps plan the steps after that."
            Candidate(req, gain, reason)
        }.sortedByDescending { it.gain }
    }
}
