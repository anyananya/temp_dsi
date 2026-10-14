package org.dsiders.nextstep.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.dsiders.nextstep.engine.StudentContext
import org.json.JSONArray
import org.json.JSONObject

class Repository(val db: AppDatabase) {

    // ---- Accounts ---------------------------------------------------------------

    suspend fun login(username: String, password: String, role: UserRole): Result<UserEntity> {
        val user = db.users().byUsername(username.trim())
            ?: return Result.failure(IllegalArgumentException("No account with that username or ID."))
        if (!Passwords.matches(password, user)) return Result.failure(IllegalArgumentException("That password doesn't match."))
        if (user.role != role) {
            val other = if (user.role == UserRole.STUDENT) "Student" else "Caseworker"
            return Result.failure(IllegalArgumentException("This is a $other account. Switch to $other above."))
        }
        return Result.success(user)
    }

    suspend fun register(
        role: UserRole, fullName: String, username: String, password: String,
        phone: String = "", college: String = "", course: String = "", year: String = "",
        district: String = "", addedBy: Long? = null, centre: String = "",
    ): Result<UserEntity> {
        val salt = Passwords.newSalt()
        val user = UserEntity(
            role = role, fullName = fullName.trim(), username = username.trim(),
            passwordHash = Passwords.hash(password, salt), salt = salt, phone = phone.trim(),
            college = college.trim(), course = course.trim(), year = year.trim(), district = district.trim(),
            addedByCaseworkerId = addedBy, mentorId = addedBy, centre = centre.trim(),
        )
        return try {
            val id = db.users().insert(user)
            Result.success(user.copy(id = id))
        } catch (e: SQLiteConstraintException) {
            Result.failure(IllegalArgumentException("That username is taken. Try another."))
        }
    }

    suspend fun user(id: Long) = db.users().byId(id)
    fun observeUser(id: Long) = db.users().observe(id)
    suspend fun updateUser(user: UserEntity) = db.users().update(user)
    fun observeStudents() = db.users().observeStudents()
    fun observeCaseworkers() = db.users().observeCaseworkers()
    suspend fun caseworkers() = db.users().caseworkers()

    // ---- Engine context ---------------------------------------------------------

    suspend fun context(studentId: Long): StudentContext {
        val student = db.users().byId(studentId) ?: error("Unknown student $studentId")
        val goal = db.goals().get(studentId)
        val role = goal?.targetRoleId?.let { db.roles().byId(it) }
        return StudentContext(
            student = student,
            goal = goal,
            role = role,
            requirements = role?.let { db.requirements().forRole(it.id) } ?: emptyList(),
            statuses = db.reqStatus().forStudent(studentId).associateBy { it.requirementId },
            facts = db.facts().forStudent(studentId),
            log = db.questionLog().forStudent(studentId),
            allRoles = db.roles().all(),
        )
    }

    /** Context for a role other than the student's goal (used to check an opening). */
    suspend fun contextForRole(studentId: Long, roleId: Long): StudentContext {
        val base = context(studentId)
        val role = db.roles().byId(roleId)
        return base.copy(role = role, requirements = role?.let { db.requirements().forRole(it.id) } ?: emptyList())
    }

    // ---- Goals, facts, statuses -------------------------------------------------

    fun observeGoal(studentId: Long) = db.goals().observe(studentId)
    fun observeAllGoals() = db.goals().observeAll()
    suspend fun goal(studentId: Long) = db.goals().get(studentId)
    suspend fun saveGoal(goal: GoalEntity) = db.goals().upsert(goal.copy(updatedAt = System.currentTimeMillis()))

    /** A new goal starts a fresh round of questions; what we know about the student stays. */
    suspend fun startGoal(studentId: Long, text: String) = db.withTransaction {
        db.questionLog().clearForStudent(studentId)
        db.goals().upsert(GoalEntity(studentId = studentId, goalText = text.trim()))
    }

    suspend fun setTargetRole(studentId: Long, roleId: Long) {
        val role = db.roles().byId(roleId) ?: return
        val goal = db.goals().get(studentId) ?: GoalEntity(studentId, "I want to work as a ${role.title.lowercase()}")
        db.withTransaction {
            db.questionLog().clearForStudent(studentId)
            db.goals().upsert(
                goal.copy(targetRoleId = role.id, field = role.field, readback = "You want to work as a ${role.title.lowercase()}",
                    intakeDone = false, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun observeFacts(studentId: Long) = db.facts().observeForStudent(studentId)

    suspend fun upsertFact(studentId: Long, label: String, value: String, source: Source) {
        val existing = db.facts().byLabel(studentId, label)
        if (existing != null && existing.source == Source.CASEWORKER && source != Source.CASEWORKER) return
        db.facts().upsert((existing ?: FactEntity(studentId = studentId, label = label, value = value)).copy(value = value, source = source))
    }

    suspend fun saveFact(fact: FactEntity) = db.facts().upsert(fact)
    suspend fun deleteFact(fact: FactEntity) = db.facts().delete(fact)

    fun observeStatuses(studentId: Long) = db.reqStatus().observeForStudent(studentId)
    fun observeAllStatuses() = db.reqStatus().observeAll()
    suspend fun status(studentId: Long, reqId: Long) = db.reqStatus().get(studentId, reqId)

    /**
     * Records a status. The same thing under another role (a resume is a resume)
     * gets the same answer, so a student is never asked twice. Caseworker overrides
     * are only ever replaced by another caseworker.
     */
    suspend fun setStatus(studentId: Long, reqId: Long, status: ReqStatus, confidence: Float, evidence: String, source: Source) {
        val req = db.requirements().byId(reqId) ?: return
        db.withTransaction {
            val targets = sameThingAs(req)
            for (target in targets) {
                val existing = db.reqStatus().get(studentId, target.id)
                if (existing?.source == Source.CASEWORKER && source != Source.CASEWORKER && target.id != reqId) continue
                // Pushing to "later" is a per-role judgement; don't copy it across roles.
                if (status == ReqStatus.LATER && target.id != reqId) continue
                db.reqStatus().upsert(ReqStatusEntity(studentId, target.id, status, confidence, evidence, source))
            }
        }
    }

    suspend fun restoreStatus(studentId: Long, reqId: Long, previous: ReqStatusEntity?) {
        if (previous == null) db.reqStatus().delete(studentId, reqId) else db.reqStatus().upsert(previous)
    }

    suspend fun clearStatus(studentId: Long, reqId: Long) = db.reqStatus().delete(studentId, reqId)

    private suspend fun sameThingAs(req: RequirementEntity): List<RequirementEntity> {
        val key = req.name.normalised()
        return db.roles().all().flatMap { db.requirements().forRole(it.id) }.filter { it.id == req.id || it.name.normalised() == key }
    }

    private fun String.normalised() = lowercase().replace(Regex("^(a|an|the)\\s+"), "").replace(Regex("[^a-z0-9 ]"), "").trim()

    // ---- Question log -----------------------------------------------------------

    suspend fun log(entry: QuestionLogEntity) = db.questionLog().insert(entry)
    suspend fun questionLog(studentId: Long) = db.questionLog().forStudent(studentId)
    fun observeQuestionLog(studentId: Long) = db.questionLog().observeForStudent(studentId)
    suspend fun deleteLog(entry: QuestionLogEntity) = db.questionLog().delete(entry)

    // ---- Roles, requirements, openings -----------------------------------------

    fun observeRoles() = db.roles().observeAll()
    suspend fun roles() = db.roles().all()
    suspend fun role(id: Long) = db.roles().byId(id)
    fun observeRole(id: Long) = db.roles().observe(id)
    suspend fun requirements(roleId: Long) = db.requirements().forRole(roleId)
    fun observeRequirements(roleId: Long) = db.requirements().observeForRole(roleId)
    suspend fun saveRole(role: RoleEntity): Long {
        val id = db.roles().upsert(role.copy(updatedAt = System.currentTimeMillis()))
        return if (role.id != 0L) role.id else id
    }
    suspend fun saveRequirement(req: RequirementEntity): Long {
        val id = db.requirements().upsert(req)
        return if (req.id != 0L) req.id else id
    }
    suspend fun deleteRequirement(req: RequirementEntity) = db.withTransaction {
        db.requirements().delete(req)
        // Drop the deleted node from other requirements' prerequisites.
        db.requirements().forRole(req.roleId).filter { req.id in it.dependsOnIds() }.forEach {
            db.requirements().upsert(it.copy(dependsOn = it.dependsOnIds().filter { d -> d != req.id }.joinToString(",")))
        }
    }
    suspend fun deleteRole(role: RoleEntity) = db.withTransaction {
        db.goals().clearRole(role.id)
        db.openings().deleteForRole(role.id)
        db.requirements().deleteForRole(role.id)
        db.roles().delete(role)
    }

    fun observeOpenings(): Flow<List<OpeningEntity>> = db.openings().observeAll()
    fun observeActiveOpenings(): Flow<List<OpeningEntity>> = db.openings().observeActive()
    suspend fun opening(id: Long) = db.openings().byId(id)
    suspend fun saveOpening(opening: OpeningEntity) = db.openings().upsert(opening)
    suspend fun deleteOpening(opening: OpeningEntity) = db.openings().delete(opening)

    // ---- Help requests, notes, activity ----------------------------------------

    fun observeRequests() = db.helpRequests().observeAll()
    fun observeRequestsFor(studentId: Long) = db.helpRequests().observeForStudent(studentId)
    suspend fun openRequest(studentId: Long) = db.helpRequests().openForStudent(studentId)

    suspend fun askForHelp(studentId: Long, caseworkerId: Long?, reason: String, message: String): Long {
        val open = db.helpRequests().openForStudent(studentId)
        val req = (open ?: HelpRequestEntity(studentId = studentId, caseworkerId = caseworkerId, reason = reason))
            .copy(caseworkerId = caseworkerId, reason = reason, message = message.trim(), status = RequestStatus.NEW, createdAt = System.currentTimeMillis())
        return db.helpRequests().upsert(req)
    }

    suspend fun markRequestSeen(id: Long) {
        val req = db.helpRequests().byId(id) ?: return
        if (req.status == RequestStatus.NEW) db.helpRequests().upsert(req.copy(status = RequestStatus.SEEN))
    }

    suspend fun replyToRequest(id: Long, caseworkerId: Long, reply: String, resolve: Boolean) {
        val req = db.helpRequests().byId(id) ?: return
        db.helpRequests().upsert(
            req.copy(
                reply = reply.trim().ifBlank { req.reply }, repliedAt = System.currentTimeMillis(), caseworkerId = caseworkerId,
                status = if (resolve) RequestStatus.RESOLVED else RequestStatus.SEEN,
            )
        )
        logActivity(caseworkerId, req.studentId, ActivityType.REPLIED)
    }

    fun observeNotes(studentId: Long) = db.notes().observeForStudent(studentId)
    suspend fun addNote(studentId: Long, caseworkerId: Long, text: String) = db.notes().insert(NoteEntity(studentId = studentId, caseworkerId = caseworkerId, text = text.trim()))
    suspend fun deleteNote(note: NoteEntity) = db.notes().delete(note)

    suspend fun logActivity(caseworkerId: Long, studentId: Long, type: ActivityType, durationSec: Long = 0) =
        db.activity().insert(ActivityEntity(caseworkerId = caseworkerId, studentId = studentId, type = type, durationSec = durationSec))

    fun observeActivitySince(caseworkerId: Long, since: Long) = db.activity().observeSince(caseworkerId, since)

    // ---- Export -----------------------------------------------------------------

    /** Everything held about a student, for "Download my details". */
    suspend fun exportStudent(studentId: Long): String {
        val ctx = context(studentId)
        val s = ctx.student
        val json = JSONObject()
            .put("name", s.fullName).put("username", s.username).put("phone", s.phone)
            .put("college", s.college).put("course", s.course).put("year", s.year)
            .put("district", s.district).put("language", s.language)
        ctx.goal?.let {
            json.put("goal", JSONObject().put("in_your_words", it.goalText).put("target_role", ctx.role?.title ?: JSONObject.NULL)
                .put("what_we_understood", it.readback).put("resume_summary", it.resumeSummary))
        }
        json.put("facts", JSONArray().apply { ctx.facts.forEach { put(JSONObject().put(it.label, it.value)) } })
        val allReqs = db.roles().all().flatMap { db.requirements().forRole(it.id) }.associateBy { it.id }
        json.put("what_we_know", JSONArray().apply {
            ctx.statuses.values.forEach { st ->
                put(JSONObject().put("item", allReqs[st.requirementId]?.name ?: "?").put("status", st.status.name)
                    .put("why", st.evidence).put("set_by", st.source.name))
            }
        })
        json.put("questions_answered", JSONArray().apply {
            ctx.log.forEach { put(JSONObject().put("question", it.question).put("answer", it.answer)) }
        })
        return json.toString(2)
    }
}
