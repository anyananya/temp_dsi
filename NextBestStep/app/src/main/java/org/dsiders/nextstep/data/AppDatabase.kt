package org.dsiders.nextstep.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    ],
    version = 1,
    exportSchema = true,
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

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nextstep.db").build()
    }
}
