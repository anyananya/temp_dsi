package org.dsiders.nextstep.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun byUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun byId(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observe(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE role = 'STUDENT' ORDER BY fullName")
    fun observeStudents(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'CASEWORKER' ORDER BY fullName")
    fun observeCaseworkers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'CASEWORKER' ORDER BY fullName")
    suspend fun caseworkers(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface RoleDao {
    @Upsert
    suspend fun upsert(role: RoleEntity): Long

    @Delete
    suspend fun delete(role: RoleEntity)

    @Query("SELECT * FROM roles ORDER BY title")
    fun observeAll(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM roles ORDER BY title")
    suspend fun all(): List<RoleEntity>

    @Query("SELECT * FROM roles WHERE id = :id")
    suspend fun byId(id: Long): RoleEntity?

    @Query("SELECT * FROM roles WHERE id = :id")
    fun observe(id: Long): Flow<RoleEntity?>
}

@Dao
interface RequirementDao {
    @Upsert
    suspend fun upsert(req: RequirementEntity): Long

    @Delete
    suspend fun delete(req: RequirementEntity)

    @Query("DELETE FROM requirements WHERE roleId = :roleId")
    suspend fun deleteForRole(roleId: Long)

    @Query("SELECT * FROM requirements WHERE roleId = :roleId ORDER BY orderIndex, id")
    suspend fun forRole(roleId: Long): List<RequirementEntity>

    @Query("SELECT * FROM requirements WHERE roleId = :roleId ORDER BY orderIndex, id")
    fun observeForRole(roleId: Long): Flow<List<RequirementEntity>>

    @Query("SELECT * FROM requirements WHERE id = :id")
    suspend fun byId(id: Long): RequirementEntity?
}

@Dao
interface OpeningDao {
    @Upsert
    suspend fun upsert(opening: OpeningEntity): Long

    @Delete
    suspend fun delete(opening: OpeningEntity)

    @Query("SELECT * FROM openings ORDER BY active DESC, createdAt DESC")
    fun observeAll(): Flow<List<OpeningEntity>>

    @Query("SELECT * FROM openings WHERE active = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<OpeningEntity>>

    @Query("SELECT * FROM openings WHERE id = :id")
    suspend fun byId(id: Long): OpeningEntity?

    @Query("DELETE FROM openings WHERE roleId = :roleId")
    suspend fun deleteForRole(roleId: Long)
}

@Dao
interface GoalDao {
    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE studentId = :studentId")
    suspend fun get(studentId: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE studentId = :studentId")
    fun observe(studentId: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET targetRoleId = NULL WHERE targetRoleId = :roleId")
    suspend fun clearRole(roleId: Long)
}

@Dao
interface FactDao {
    @Upsert
    suspend fun upsert(fact: FactEntity): Long

    @Delete
    suspend fun delete(fact: FactEntity)

    @Query("SELECT * FROM facts WHERE studentId = :studentId ORDER BY id")
    suspend fun forStudent(studentId: Long): List<FactEntity>

    @Query("SELECT * FROM facts WHERE studentId = :studentId ORDER BY id")
    fun observeForStudent(studentId: Long): Flow<List<FactEntity>>

    @Query("SELECT * FROM facts WHERE studentId = :studentId AND label = :label COLLATE NOCASE LIMIT 1")
    suspend fun byLabel(studentId: Long, label: String): FactEntity?
}

@Dao
interface ReqStatusDao {
    @Upsert
    suspend fun upsert(status: ReqStatusEntity)

    @Query("DELETE FROM req_status WHERE studentId = :studentId AND requirementId = :requirementId")
    suspend fun delete(studentId: Long, requirementId: Long)

    @Query("DELETE FROM req_status WHERE studentId = :studentId")
    suspend fun clearForStudent(studentId: Long)

    @Query("SELECT * FROM req_status WHERE studentId = :studentId")
    suspend fun forStudent(studentId: Long): List<ReqStatusEntity>

    @Query("SELECT * FROM req_status WHERE studentId = :studentId")
    fun observeForStudent(studentId: Long): Flow<List<ReqStatusEntity>>

    @Query("SELECT * FROM req_status")
    fun observeAll(): Flow<List<ReqStatusEntity>>

    @Query("SELECT * FROM req_status WHERE studentId = :studentId AND requirementId = :requirementId")
    suspend fun get(studentId: Long, requirementId: Long): ReqStatusEntity?
}

@Dao
interface QuestionLogDao {
    @Insert
    suspend fun insert(entry: QuestionLogEntity): Long

    @Delete
    suspend fun delete(entry: QuestionLogEntity)

    @Query("SELECT * FROM question_log WHERE studentId = :studentId ORDER BY id")
    suspend fun forStudent(studentId: Long): List<QuestionLogEntity>

    @Query("SELECT * FROM question_log WHERE studentId = :studentId ORDER BY id")
    fun observeForStudent(studentId: Long): Flow<List<QuestionLogEntity>>

    @Query("DELETE FROM question_log WHERE studentId = :studentId")
    suspend fun clearForStudent(studentId: Long)
}

@Dao
interface HelpRequestDao {
    @Upsert
    suspend fun upsert(request: HelpRequestEntity): Long

    @Query("SELECT * FROM help_requests WHERE id = :id")
    suspend fun byId(id: Long): HelpRequestEntity?

    @Query("SELECT * FROM help_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HelpRequestEntity>>

    @Query("SELECT * FROM help_requests WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun observeForStudent(studentId: Long): Flow<List<HelpRequestEntity>>

    @Query("SELECT * FROM help_requests WHERE studentId = :studentId AND status != 'RESOLVED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun openForStudent(studentId: Long): HelpRequestEntity?
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun observeForStudent(studentId: Long): Flow<List<NoteEntity>>
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Query("SELECT * FROM activity WHERE caseworkerId = :caseworkerId AND at >= :since")
    fun observeSince(caseworkerId: Long, since: Long): Flow<List<ActivityEntity>>
}

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE requestId = :requestId ORDER BY at, id")
    fun observeForRequest(requestId: Long): Flow<List<MessageEntity>>

    @Query("SELECT m.* FROM messages m JOIN help_requests r ON r.id = m.requestId WHERE r.studentId = :studentId ORDER BY m.at, m.id")
    fun observeForStudent(studentId: Long): Flow<List<MessageEntity>>
}
