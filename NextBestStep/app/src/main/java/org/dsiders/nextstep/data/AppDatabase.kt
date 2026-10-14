package org.dsiders.nextstep.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        RoleEntity::class,
        RequirementEntity::class,
        OpeningEntity::class,
        GoalEntity::class,
        FactEntity::class,
        ReqStatusEntity::class,
        QuestionLogEntity::class,
        HelpRequestEntity::class,
        NoteEntity::class,
        ActivityEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = AppDatabase.V1ToV2::class)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun users(): UserDao
    abstract fun roles(): RoleDao
    abstract fun requirements(): RequirementDao
    abstract fun openings(): OpeningDao
    abstract fun goals(): GoalDao
    abstract fun facts(): FactDao
    abstract fun reqStatus(): ReqStatusDao
    abstract fun questionLog(): QuestionLogDao
    abstract fun helpRequests(): HelpRequestDao
    abstract fun notes(): NoteDao
    abstract fun activity(): ActivityDao
    abstract fun messages(): MessageDao

    /** v2 adds message threads; copy each old one-message/one-reply request into a thread. */
    class V1ToV2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO messages (requestId, senderId, fromStudent, text, at) " +
                    "SELECT id, studentId, 1, message, createdAt FROM help_requests WHERE message != ''"
            )
            db.execSQL(
                "INSERT INTO messages (requestId, senderId, fromStudent, text, at) " +
                    "SELECT id, caseworkerId, 0, reply, COALESCE(repliedAt, createdAt) FROM help_requests WHERE reply != ''"
            )
        }
    }

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nextstep.db").build()
    }
}
