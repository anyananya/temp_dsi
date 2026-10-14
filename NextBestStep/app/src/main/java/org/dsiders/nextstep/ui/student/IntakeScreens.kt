package org.dsiders.nextstep.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.engine.AnswerOption
import org.dsiders.nextstep.engine.NextQuestion
import org.dsiders.nextstep.engine.Question
import org.dsiders.nextstep.ui.components.AnswerRow
import org.dsiders.nextstep.ui.components.FieldInput
import org.dsiders.nextstep.ui.components.InfoBanner
import org.dsiders.nextstep.ui.components.LoadingBlock
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.SegmentProgress
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.theme.Palette

// ---------------------------------------------------------------------------
// Frame 2: Reading it back
// ---------------------------------------------------------------------------

class ReadbackViewModel(private val c: AppContainer, private val studentId: Long) : ViewModel() {
    data class State(
        val goalText: String = "",
        val understood: List<String> = emptyList(),
        val parsing: Boolean = true,
        val picking: Boolean = false,
        val offline: Boolean = false,
        val result: NextQuestion? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val goal = c.repo.goal(studentId)
            _state.value = State(goalText = goal?.goalText.orEmpty())
            val started = System.currentTimeMillis()
            val intake = c.engine.runIntake(studentId, c.pendingResume.remove(studentId))
            _state.value = _state.value.copy(understood = intake.readback, parsing = false, picking = true, offline = !intake.usedAi)
            val next = c.engine.nextQuestion(studentId)
            // Let the student actually see that their sentence was read.
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < 1800) delay(1800 - elapsed)
            _state.value = _state.value.copy(picking = false, result = next)
        }
    }
}

@Composable
fun ReadbackScreen(container: AppContainer, studentId: Long, onQuestion: () -> Unit, onDone: () -> Unit) {
    val vm: ReadbackViewModel = viewModel(key = "readback-$studentId") { ReadbackViewModel(container, studentId) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.result) {
        when (state.result) {
            is NextQuestion.Ask -> { delay(400); onQuestion() }
            NextQuestion.Done -> { delay(400); onDone() }
            null -> Unit
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        SectionLabel("Here's what I understood", modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), color = Palette.TextMuted)
        Text(
            "“${state.goalText}”", modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        if (state.parsing) {
            ProgressLine("Reading what you wrote", done = false)
        } else {
            state.understood.forEach { ProgressLine(it, done = true) }
        }
        if (!state.parsing) ProgressLine("Working out what to ask you", done = !state.picking, bold = state.picking)
        Spacer(Modifier.height(48.dp))
        Text(
            if (state.offline && !state.parsing) "Working offline, so the questions will be simpler. This takes a few seconds."
            else "This takes a few seconds.",
            modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProgressLine(text: String, done: Boolean, bold: Boolean = false) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (done) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Palette.Green), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            CircularProgressIndicator(Modifier.size(20.dp), color = Palette.Navy, strokeWidth = 2.dp, trackColor = Palette.Divider)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
            color = if (bold) Palette.Navy else Palette.Text,
        )
    }
}

// ---------------------------------------------------------------------------
// Frames 3 and 4: one question at a time
// ---------------------------------------------------------------------------

class QuestionViewModel(private val c: AppContainer, private val studentId: Long) : ViewModel() {
    sealed interface UiState {
        data object Loading : UiState
        data class Asking(val question: Question) : UiState
        data object Done : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()
    private val _exit = MutableStateFlow(false)
    val exit = _exit.asStateFlow()

    init { load() }

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val next = c.engine.nextQuestion(studentId)) {
                is NextQuestion.Ask -> UiState.Asking(next.question)
                NextQuestion.Done -> UiState.Done
            }
        }
    }

    fun answer(q: Question, option: AnswerOption, freeText: String = "") {
        viewModelScope.launch {
            _state.value = UiState.Loading
            c.engine.answer(studentId, q, option, freeText)
            load()
        }
    }

    fun skip(q: Question) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            c.engine.skip(studentId, q)
            load()
        }
    }

    fun back() {
        viewModelScope.launch {
            if (c.engine.undo(studentId)) load() else _exit.value = true
        }
    }
}

private fun iconForField(field: String): ImageVector {
    val f = field.lowercase()
    return when {
        "data" in f || "number" in f -> Icons.Filled.BarChart
        "web" in f || "app" in f || "code" in f || "software" in f -> Icons.Filled.Code
        "design" in f || "creative" in f || "art" in f -> Icons.Filled.Palette
        "government" in f || "govt" in f -> Icons.Filled.AccountBalance
        else -> Icons.Filled.Work
    }
}

@Composable
fun QuestionScreen(container: AppContainer, studentId: Long, onDone: () -> Unit, onExit: () -> Unit) {
    val vm: QuestionViewModel = viewModel(key = "question-$studentId") { QuestionViewModel(container, studentId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val exit by vm.exit.collectAsStateWithLifecycle()

    LaunchedEffect(state) { if (state is QuestionViewModel.UiState.Done) onDone() }
    LaunchedEffect(exit) { if (exit) onExit() }

    Column(Modifier.fillMaxSize()) {
        when (val s = state) {
            QuestionViewModel.UiState.Loading -> {
                Row(Modifier.padding(4.dp)) {
                    IconButton(vm::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.Navy) }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingBlock("Thinking about what to ask next…") }
            }
            is QuestionViewModel.UiState.Asking -> AnimatedContent(
                targetState = s.question, label = "question",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { q -> QuestionBody(q, onAnswer = vm::answer, onSkip = { vm.skip(q) }, onBack = vm::back) }
            QuestionViewModel.UiState.Done -> LoadingBlock("Putting it together…")
        }
    }
}

@Composable
private fun QuestionBody(
    q: Question,
    onAnswer: (Question, AnswerOption, String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    var showWhy by remember { mutableStateOf(false) }
    var bannerVisible by rememberSaveable(q.attributeKey) { mutableStateOf(true) }
    var freeTextMode by rememberSaveable(q.attributeKey) { mutableStateOf(false) }
    var freeText by rememberSaveable(q.attributeKey) { mutableStateOf("") }
    val isFieldQuestion = q.options.any { it.field != null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.Navy) }
            SegmentProgress(q.number, q.estimatedTotal, Modifier.weight(1f).padding(end = 8.dp))
        }
        Text(
            "Question ${q.number} of about ${q.estimatedTotal}", modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, textAlign = TextAlign.End,
        )
        Spacer(Modifier.height(16.dp))
        if (q.banner.isNotBlank() && bannerVisible) {
            InfoBanner(q.banner, onClose = { bannerVisible = false })
            Spacer(Modifier.height(16.dp))
        }
        NsCard(padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Text(q.text, style = MaterialTheme.typography.headlineMedium)
            if (q.helper.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(q.helper, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            }
        }
        Spacer(Modifier.height(20.dp))

        if (freeTextMode) {
            FieldInput(freeText, { freeText = it }, "In your own words", placeholder = "e.g. teaching, nursing, running a shop", singleLine = false, minLines = 2)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Continue", { onAnswer(q, q.options.first { it.freeText }, freeText) }, enabled = freeText.isNotBlank())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextLink("Pick from the list instead", { freeTextMode = false })
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                q.options.forEach { option ->
                    AnswerRow(
                        option.label,
                        onClick = { if (option.freeText) freeTextMode = true else onAnswer(q, option, "") },
                        icon = when {
                            option.freeText -> Icons.Filled.AutoAwesome
                            isFieldQuestion -> iconForField(option.label)
                            else -> null
                        },
                        tag = if (option.suggested) "Closest to what you said" else null,
                    )
                }
                q.dontKnowOption?.let { dk ->
                    Spacer(Modifier.height(4.dp))
                    AnswerRow(dk.label, onClick = { onAnswer(q, dk, "") }, icon = Icons.Outlined.Explore, tinted = true)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isFieldQuestion) TextLink("Why are you asking this?", { showWhy = true })
            else TextLink("Skip this question", onSkip)
            if (!isFieldQuestion) TextLink("Skip this question", onSkip, color = Palette.TextMuted, underline = false)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Palette.TextMuted, modifier = Modifier.size(16.dp))
                TextLink("Back", onBack, color = Palette.TextMuted, underline = false)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            confirmButton = { TextButton({ showWhy = false }) { Text("Got it") } },
            title = { Text("Why I'm asking") },
            text = { Text(q.whyAsking) },
        )
    }
}
