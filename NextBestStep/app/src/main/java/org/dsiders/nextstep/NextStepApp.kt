package org.dsiders.nextstep

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dsiders.nextstep.ai.OpenAiClient
import org.dsiders.nextstep.data.AppDatabase
import org.dsiders.nextstep.data.Repository
import org.dsiders.nextstep.data.SeedData
import org.dsiders.nextstep.data.SessionStore
import org.dsiders.nextstep.engine.NextStepEngine
import org.dsiders.nextstep.engine.ResumeInput
import java.util.concurrent.ConcurrentHashMap

/** Hand-wired dependencies; small enough that a DI framework would only add weight. */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = AppDatabase.build(app)
    val repo = Repository(db)
    val ai = OpenAiClient()
    val engine = NextStepEngine(repo, ai)
    val session = SessionStore(app)

    /** A resume picked on the goal screen, waiting for the intake parser. */
    val pendingResume = ConcurrentHashMap<Long, ResumeInput>()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    init {
        appScope.launch {
            SeedData.seedIfEmpty(db)
            _ready.value = true
        }
    }
}

class NextStepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
