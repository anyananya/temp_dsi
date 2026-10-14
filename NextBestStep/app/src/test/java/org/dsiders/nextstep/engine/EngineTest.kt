package org.dsiders.nextstep.engine

import org.dsiders.nextstep.data.GoalEntity
import org.dsiders.nextstep.data.Priority
import org.dsiders.nextstep.data.QuestionLogEntity
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.ReqStatusEntity
import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.RequirementKind
import org.dsiders.nextstep.data.RoleEntity
import org.dsiders.nextstep.data.Source
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.data.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The worked example from the Sprint 1 document, run through the engine. */
class EngineTest {
    private val role = RoleEntity(id = 1, title = "Data analyst", field = "Working with data and numbers")
    private val sheets = RequirementEntity(id = 10, roleId = 1, name = "The basics of spreadsheets", orderIndex = 0)
    private val resume = RequirementEntity(id = 11, roleId = 1, name = "A resume", kind = RequirementKind.ARTEFACT, orderIndex = 1)
    private val sql = RequirementEntity(id = 12, roleId = 1, name = "Basic SQL", orderIndex = 2, dependsOn = "10")
    private val project = RequirementEntity(id = 13, roleId = 1, name = "Build one small project", orderIndex = 3, dependsOn = "12")
    private val python = RequirementEntity(id = 14, roleId = 1, name = "Python", orderIndex = 4, priority = Priority.LATER)
    private val reqs = listOf(sheets, resume, sql, project, python)

    private val priya = UserEntity(id = 5, role = UserRole.STUDENT, fullName = "Priya Komati", username = "priya", passwordHash = "", salt = "")

    private fun ctx(
        statuses: List<ReqStatusEntity> = emptyList(),
        log: List<QuestionLogEntity> = emptyList(),
        role: RoleEntity? = this.role,
    ) = StudentContext(
        student = priya,
        goal = GoalEntity(5, "I want to become a data analyst", role?.id),
        role = role,
        requirements = if (role == null) emptyList() else reqs,
        statuses = statuses.associateBy { it.requirementId },
        facts = emptyList(),
        log = log,
        allRoles = listOf(this.role),
    )

    private fun st(req: RequirementEntity, status: ReqStatus, conf: Float = 0.9f, source: Source = Source.STUDENT) =
        ReqStatusEntity(5, req.id, status, conf, "because", source)

    private fun asked(req: RequirementEntity, dontKnow: Boolean = false) =
        QuestionLogEntity(studentId = 5, attributeKey = "req:${req.id}", question = "?", answer = "a", saidDontKnow = dontKnow)

    @Test fun firstQuestionIsAboutTheStepThatDecidesTheNextStep() {
        val top = QuestionPicker.rank(ctx()).first()
        assertEquals(sheets.id, top.req.id)
        assertEquals("This decides your very next step.", top.reason)
    }

    @Test fun answerMovesTheQuestionToTheNextUnknown() {
        val c = ctx(listOf(st(sheets, ReqStatus.HAVE)), listOf(asked(sheets)))
        assertEquals(resume.id, QuestionPicker.rank(c).first().req.id)
    }

    @Test fun questionsStopOnceTheNextStepIsKnown() {
        val c = ctx(
            listOf(st(sheets, ReqStatus.HAVE), st(resume, ReqStatus.MISSING)),
            listOf(asked(sheets), asked(resume)),
        )
        val worthAsking = QuestionPicker.rank(c).filter { it.gain >= QuestionPicker.GAIN_THRESHOLD }
        assertTrue("Expected no more questions, got $worthAsking", worthAsking.isEmpty())
    }

    @Test fun laterItemsAreNeverAsked() {
        assertFalse(QuestionPicker.rank(ctx()).any { it.req.id == python.id })
    }

    @Test fun priyasResultMatchesTheWorkedExample() {
        val gap = GapReasoner.analyse(ctx(listOf(st(sheets, ReqStatus.HAVE), st(resume, ReqStatus.MISSING))))
        assertEquals(listOf(sheets.id), gap.have.map { it.requirement.id })
        assertEquals(listOf(resume.id, sql.id, project.id), gap.missing.map { it.requirement.id })
        assertEquals(listOf(python.id), gap.later.map { it.requirement.id })
        assertEquals(resume.id, gap.nextStep?.id)
        assertEquals(StepState.DONE, gap.path.first { it.requirement.id == sheets.id }.state)
        assertEquals(StepState.CURRENT, gap.path.first { it.requirement.id == resume.id }.state)
        assertFalse(gap.needsHandover)
        assertEquals("Stuck: no resume yet", gap.handoverReason)
    }

    @Test fun nextStepRespectsPrerequisites() {
        // Resume done but spreadsheets unknown: SQL can't be next, because it needs spreadsheets first.
        val gap = GapReasoner.analyse(ctx(listOf(st(resume, ReqStatus.HAVE), st(sql, ReqStatus.MISSING))))
        assertEquals(sheets.id, gap.nextStep?.id)
    }

    @Test fun prerequisitesComeFirstEvenIfOrderedLater() {
        val a = RequirementEntity(id = 1, roleId = 1, name = "A", orderIndex = 0, dependsOn = "2")
        val b = RequirementEntity(id = 2, roleId = 1, name = "B", orderIndex = 1)
        assertEquals(listOf(2L, 1L), GapReasoner.topoOrder(listOf(a, b)).map { it.id })
    }

    @Test fun cyclesDoNotHang() {
        val a = RequirementEntity(id = 1, roleId = 1, name = "A", dependsOn = "2")
        val b = RequirementEntity(id = 2, roleId = 1, name = "B", dependsOn = "1")
        assertEquals(2, GapReasoner.topoOrder(listOf(a, b)).size)
    }

    @Test fun unsureAnswersHandOverToAPerson() {
        val gap = GapReasoner.analyse(ctx(listOf(st(sheets, ReqStatus.UNKNOWN, 0.2f)), listOf(asked(sheets))))
        assertTrue(gap.needsHandover)
        assertTrue(gap.confidence < GapReasoner.CONFIDENCE_THRESHOLD)
    }

    @Test fun saidIDontKnowAlwaysOffersAPerson() {
        val gap = GapReasoner.analyse(
            ctx(listOf(st(sheets, ReqStatus.HAVE), st(resume, ReqStatus.MISSING)), listOf(asked(sheets), asked(resume, dontKnow = true)))
        )
        assertTrue(gap.needsHandover)
    }

    @Test fun noRoleMeansHandoverNotAGuess() {
        val gap = GapReasoner.analyse(ctx(role = null))
        assertNull(gap.nextStep)
        assertTrue(gap.needsHandover)
    }

    @Test fun caseworkerCanPushAStepToLater() {
        val gap = GapReasoner.analyse(
            ctx(listOf(st(sheets, ReqStatus.HAVE), st(resume, ReqStatus.LATER, 1f, Source.CASEWORKER)))
        )
        assertTrue(gap.later.any { it.requirement.id == resume.id })
        assertEquals(sql.id, gap.nextStep?.id)
    }

    @Test fun everythingDoneMeansReady() {
        val gap = GapReasoner.analyse(ctx(listOf(sheets, resume, sql, project).map { st(it, ReqStatus.HAVE) }))
        assertNull(gap.nextStep)
        assertTrue(gap.readyForRole)
    }

    @Test fun stepTitleFallsBackToTheKind() {
        assertEquals("Learn basic SQL", sql.stepTitle())
        assertEquals("Write your first resume", resume.copy(actionTitle = "Write your first resume").stepTitle())
        assertEquals("Build one small project", project.copy(kind = RequirementKind.ARTEFACT).stepTitle())
    }
}
