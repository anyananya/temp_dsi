package org.dsiders.nextstep.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole { STUDENT, CASEWORKER }

/** Where a requirement stands for one student. */
enum class ReqStatus { HAVE, PARTIAL, MISSING, UNKNOWN, LATER }

/** Who last set a requirement status. A caseworker override always wins. */
enum class Source { SYSTEM, STUDENT, RESUME, CASEWORKER }

enum class Priority { CORE, LATER }

enum class RequirementKind { SKILL, DOCUMENT, ARTEFACT, EXPERIENCE }

enum class RequestStatus { NEW, SEEN, RESOLVED }

/**
 * One table for both roles. Student-only and caseworker-only columns are
 * simply left at their defaults for the other role.
 */
@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: UserRole,
    val fullName: String,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Student
    val college: String = "",
    val course: String = "",
    val year: String = "",
    val district: String = "",
    val language: String = "English",
    val mentorId: Long? = null,
    val addedByCaseworkerId: Long? = null,
    // Caseworker
    val centre: String = "",
    val available: Boolean = true,
    val languages: String = "English",
    val topics: String = "",
    val workingHours: String = "10 AM – 6 PM",
    val notificationsOn: Boolean = true,
)

/** A target role ("job") an NGO worker defines, e.g. Data analyst. */
@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** The "kind of work" bucket shown in the first question, e.g. "Working with data and numbers". */
    val field: String,
    val description: String = "",
    /** Comma separated words used for offline matching of a goal sentence. */
    val keywords: String = "",
    val createdBy: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * A node in the requirement graph: something a role needs.
 * [dependsOn] holds comma separated requirement ids that must be done first.
 */
@Entity(tableName = "requirements", indices = [Index("roleId")])
data class RequirementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    val name: String,
    /** How the step reads as an action, e.g. "Write your first resume". Optional. */
    val actionTitle: String = "",
    val kind: RequirementKind = RequirementKind.SKILL,
    val priority: Priority = Priority.CORE,
    val orderIndex: Int = 0,
    val whyNeeded: String = "",
    val timeEstimate: String = "",
    /** One item per line: what to bring or have ready. */
    val whatYouNeed: String = "",
    val whereToGetHelp: String = "",
    val howTo: String = "",
    val dependsOn: String = "",
    /** Optional wording hint for the question about this requirement. */
    val questionHint: String = "",
) {
    fun dependsOnIds(): List<Long> = dependsOn.split(',').mapNotNull { it.trim().toLongOrNull() }
    fun whatYouNeedList(): List<String> = whatYouNeed.lines().map { it.trim() }.filter { it.isNotEmpty() }

    /** The step as something to do: the NGO's wording if given, otherwise built from the kind. */
    fun stepTitle(): String {
        if (actionTitle.isNotBlank()) return actionTitle.trim()
        val n = name.trim().replaceFirstChar { it.lowercase() }
        return when (kind) {
            RequirementKind.SKILL -> "Learn $n"
            RequirementKind.DOCUMENT -> "Get $n"
            RequirementKind.ARTEFACT -> if (n.startsWith("build") || n.startsWith("write") || n.startsWith("make")) name.trim() else "Make $n"
            RequirementKind.EXPERIENCE -> "Get $n"
        }.replaceFirstChar { it.uppercase() }
    }
}

/** A real opening (job, internship, apprenticeship) linked to a role. */
@Entity(tableName = "openings", indices = [Index("roleId")])
data class OpeningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    val title: String,
    val organisation: String,
    val location: String = "",
    val deadline: String = "",
    val description: String = "",
    val howToApply: String = "",
    val active: Boolean = true,
    val createdBy: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** The student's current goal and where the intake has got to. */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val studentId: Long,
    val goalText: String,
    val targetRoleId: Long? = null,
    val field: String = "",
    /** Lines shown with a tick on the "Here's what I understood" screen. */
    val readback: String = "",
    val resumeSummary: String = "",
    val intakeDone: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** A plain fact the system holds about a student ("Studying: B.Sc CS"). */
@Entity(tableName = "facts", indices = [Index("studentId")])
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val label: String,
    val value: String,
    val source: Source = Source.STUDENT,
)

@Entity(tableName = "req_status", primaryKeys = ["studentId", "requirementId"])
data class ReqStatusEntity(
    val studentId: Long,
    val requirementId: Long,
    val status: ReqStatus,
    val confidence: Float,
    /** Short reason in the student's terms, e.g. "You said you've used Excel in college." */
    val evidence: String = "",
    val source: Source = Source.SYSTEM,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Every question asked, with what was there before so "Back" can undo it. */
@Entity(tableName = "question_log", indices = [Index("studentId")])
data class QuestionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    /** "field" for the kind-of-work question, otherwise "req:<id>". */
    val attributeKey: String,
    val question: String,
    val answer: String,
    val skipped: Boolean = false,
    val saidDontKnow: Boolean = false,
    val prevStatus: ReqStatus? = null,
    val prevConfidence: Float? = null,
    val prevEvidence: String? = null,
    val prevSource: Source? = null,
    val askedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "help_requests", indices = [Index("studentId"), Index("caseworkerId")])
data class HelpRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val caseworkerId: Long?,
    /** One-line reason shown on the caseworker's list, e.g. "Stuck: no resume yet". */
    val reason: String,
    val message: String = "",
    val status: RequestStatus = RequestStatus.NEW,
    val reply: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val repliedAt: Long? = null,
)

@Entity(tableName = "notes", indices = [Index("studentId")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val caseworkerId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ActivityType { STUDENT_SEEN, NEXT_STEP_SHARED, REPLIED }

/** Caseworker activity, used for the "Today" numbers on the caseworker profile. */
@Entity(tableName = "activity", indices = [Index("caseworkerId")])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseworkerId: Long,
    val studentId: Long,
    val type: ActivityType,
    val durationSec: Long = 0,
    val at: Long = System.currentTimeMillis(),
)
