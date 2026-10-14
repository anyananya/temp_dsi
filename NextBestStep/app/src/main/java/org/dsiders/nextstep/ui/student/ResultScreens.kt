package org.dsiders.nextstep.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.Source
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.data.UserRole
import org.dsiders.nextstep.engine.GapItem
import org.dsiders.nextstep.engine.GapResult
import org.dsiders.nextstep.engine.HowToGuide
import org.dsiders.nextstep.engine.StepState
import org.dsiders.nextstep.ui.components.AccentCard
import org.dsiders.nextstep.ui.components.BackRow
import org.dsiders.nextstep.ui.components.Dot
import org.dsiders.nextstep.ui.components.EmptyState
import org.dsiders.nextstep.ui.components.GroupHeader
import org.dsiders.nextstep.ui.components.LoadingBlock
import org.dsiders.nextstep.ui.components.MentorCard
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.Pill
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.RowDivider
import org.dsiders.nextstep.ui.components.SecondaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.components.pickMentor
import org.dsiders.nextstep.ui.theme.Palette

/** Recomputes the gap analysis whenever anything about the student changes. */
@Composable
fun rememberGap(container: AppContainer, studentId: Long): GapResult? {
    val trigger = remember(studentId) {
        combine(container.repo.observeGoal(studentId), container.repo.observeStatuses(studentId), container.repo.observeQuestionLog(studentId)) { g, s, l -> Triple(g, s, l) }
    }.collectAsState(initial = null)
    var gap by remember(studentId) { mutableStateOf<GapResult?>(null) }
    LaunchedEffect(trigger.value) { if (trigger.value != null) gap = container.engine.analyse(studentId) }
    return gap
}

// ---------------------------------------------------------------------------
// Frame 5: Where you stand
// ---------------------------------------------------------------------------

@Composable
fun StandingScreen(
    container: AppContainer, me: UserEntity, studentId: Long,
    onNext: () -> Unit, onBack: () -> Unit, onRestart: () -> Unit, onOpenHelp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gap = rememberGap(container, studentId)
    val student by remember(studentId) { container.repo.observeUser(studentId) }.collectAsState(initial = null)
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())
    val requests by remember(studentId) { container.repo.observeRequestsFor(studentId) }.collectAsState(initial = emptyList())
    val isStudent = me.role == UserRole.STUDENT
    var laterOpen by rememberSaveable { mutableStateOf(false) }
    var detail by remember { mutableStateOf<GapItem?>(null) }
    var askMessage by remember { mutableStateOf(false) }
    val requestOpen = requests.any { it.status != org.dsiders.nextstep.data.RequestStatus.RESOLVED }

    if (gap == null || student == null) { LoadingBlock("Working out where you stand…"); return }
    val g = gap
    val s = student!!
    val mentor = pickMentor(s, caseworkers, g.nextStep?.name)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            BackRow(onBack)
            Text("Where you stand", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                if (g.questionsAsked > 0) "Based on ${g.questionsAsked} question${if (g.questionsAsked == 1) "" else "s"}. Tap anything to see why."
                else "Based on what you've told us. Tap anything to see why.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Text, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(12.dp))

            if (g.role == null) {
                NsCard {
                    Text("Let's work this out together", style = MaterialTheme.typography.titleLarge, color = Palette.Navy)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "I don't want to guess about something this important. A mentor can help you pick a direction, " +
                            "and then I can show you the steps.",
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Text,
                    )
                }
            } else {
                GroupHeader("You already have this", Palette.Green, g.have.size, Palette.GreenSoft, Palette.Green)
                Spacer(Modifier.height(10.dp))
                if (g.have.isEmpty()) Text("Nothing yet, and that's normal at the start.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(start = 18.dp))
                g.have.forEach { ItemCard(it, Palette.Green) { detail = it }; Spacer(Modifier.height(10.dp)) }

                Spacer(Modifier.height(10.dp))
                GroupHeader(
                    if (g.missing.size == 1) "Missing one thing" else "Missing a few things",
                    Palette.Amber, g.missing.size, Palette.AmberSoft, Palette.AmberText,
                )
                Spacer(Modifier.height(10.dp))
                if (g.missing.isEmpty()) Text("Nothing missing for a first role.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(start = 18.dp))
                g.missing.forEach { ItemCard(it, Palette.Amber) { detail = it }; Spacer(Modifier.height(10.dp)) }

                if (g.later.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    NsCard(onClick = { laterOpen = !laterOpen }) {
                        GroupHeader("Not worth worrying about yet", Palette.Grey, g.later.size, Palette.GreySoft, Palette.TextMuted) {
                            Icon(if (laterOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Palette.TextMuted)
                        }
                        Text("Advanced things you can leave for later.", style = MaterialTheme.typography.bodySmall, color = Palette.Text, modifier = Modifier.padding(start = 18.dp, top = 4.dp))
                        AnimatedVisibility(laterOpen) {
                            Column(Modifier.padding(start = 18.dp, top = 8.dp)) {
                                g.later.forEach {
                                    Text("• ${it.requirement.name}", style = MaterialTheme.typography.bodyMedium, color = Palette.NavyMuted, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (isStudent && (g.needsHandover || g.role == null)) {
                Spacer(Modifier.height(16.dp))
                MentorCard(
                    mentor = mentor,
                    headline = "Not sure this is right?",
                    body = if (g.role == null) "A Barabari mentor can help you choose. It's what they're here for."
                    else "I'm only partly confident about this one. A Barabari mentor can look at it with you.",
                    requestSent = requestOpen,
                    onMessage = { askMessage = true },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.background(Palette.Page).padding(16.dp)) {
            when {
                g.role == null -> PrimaryButton("Describe my goal again", onRestart)
                g.nextStep != null -> PrimaryButton("Show me what to do next", onNext)
                else -> PrimaryButton(if (isStudent) "Ask my mentor about openings" else "Back to student", if (isStudent) onOpenHelp else onBack)
            }
        }
    }

    detail?.let { item ->
        ItemDetailDialog(item, isStudent, onDismiss = { detail = null }) { status ->
            scope.launch {
                container.repo.setStatus(
                    studentId, item.requirement.id, status, if (isStudent) 0.85f else 1f,
                    if (status == ReqStatus.HAVE) "You told us you have this." else "You told us you don't have this yet.",
                    if (isStudent) Source.STUDENT else Source.CASEWORKER,
                )
                container.engine.invalidate(studentId)
            }
            detail = null
        }
    }

    if (askMessage) {
        AskMentorDialog(
            mentorName = mentor?.fullName,
            onDismiss = { askMessage = false },
            onSend = { message ->
                scope.launch { container.repo.askForHelp(studentId, mentor?.id, g.handoverReason, message) }
                askMessage = false
            },
        )
    }
}

@Composable
private fun ItemCard(item: GapItem, accent: Color, onClick: () -> Unit) {
    AccentCard(accent, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.requirement.name, style = MaterialTheme.typography.titleMedium, color = Palette.Navy)
                Spacer(Modifier.height(2.dp))
                Text(item.reason, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                if (item.source == Source.CASEWORKER) {
                    Spacer(Modifier.height(4.dp))
                    Text("Checked by your mentor", style = MaterialTheme.typography.bodySmall, color = Palette.Green)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
        }
    }
}

@Composable
private fun ItemDetailDialog(item: GapItem, isStudent: Boolean, onDismiss: () -> Unit, onCorrect: (ReqStatus) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.requirement.name) },
        text = {
            Column {
                if (item.requirement.whyNeeded.isNotBlank()) {
                    SectionLabel("Why it matters")
                    Text(item.requirement.whyNeeded, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                }
                SectionLabel("Why it's in this group")
                Text(item.reason, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (isStudent) "Is this wrong? Tell us and the plan will change." else "Correct it for this student:",
                    style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted,
                )
            }
        },
        confirmButton = {
            if (item.status == ReqStatus.HAVE) TextButton({ onCorrect(ReqStatus.MISSING) }) { Text("I don't have this") }
            else TextButton({ onCorrect(ReqStatus.HAVE) }) { Text("I already have this") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Close") } },
    )
}

@Composable
fun AskMentorDialog(mentorName: String?, onDismiss: () -> Unit, onSend: (String) -> Unit, prefill: String = "") {
    var text by remember { mutableStateOf(prefill) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mentorName?.let { "Message ${it.substringBefore(' ')}" } ?: "Leave a message") },
        text = {
            Column {
                Text(
                    "They'll see your goal and your answers, so you only need to say what's on your mind.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted,
                )
                Spacer(Modifier.height(10.dp))
                org.dsiders.nextstep.ui.components.FieldInput(text, { text = it }, "Your message (optional)", singleLine = false, minLines = 3)
            }
        },
        confirmButton = { TextButton({ onSend(text) }) { Text("Send") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------------
// Frame 6: Your next step
// ---------------------------------------------------------------------------

@Composable
fun NextStepScreen(
    container: AppContainer, me: UserEntity, studentId: Long,
    onHowTo: (Long) -> Unit, onDoneRerun: () -> Unit, onPath: () -> Unit, onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gap = rememberGap(container, studentId)
    if (gap == null) { LoadingBlock("Finding your next step…"); return }
    val step = gap.nextStep
    if (step == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            BackRow(onBack)
            EmptyState(
                if (gap.role == null) "No next step yet" else "You've done every step",
                if (gap.role == null) "Tell us what you want to become first." else "Talk to your mentor about openings and applying.",
            )
        }
        return
    }
    val checked = remember(step.id) { mutableStateListOf<Int>() }
    val dependents = gap.path.count { step.id in it.requirement.dependsOnIds() }
    val subtitle = when {
        dependents > 0 -> "Do this first. ${if (dependents == 1) "The next step builds" else "$dependents later steps build"} on it."
        gap.role != null -> "This is the one thing standing between you and the next part of your path to ${gap.role.title.lowercase()}."
        else -> ""
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack)
        SectionLabel("Your next step", color = Palette.Orange, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(10.dp))
        NsCard(padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Palette.OrangeSoft), contentAlignment = Alignment.Center) {
                Icon(iconFor(step), null, tint = Palette.Orange, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(step.stepTitle(), style = MaterialTheme.typography.headlineMedium)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            }
            val needs = step.whatYouNeedList()
            if (needs.isNotEmpty()) {
                Spacer(Modifier.height(16.dp)); RowDividerFull(); Spacer(Modifier.height(14.dp))
                SectionLabel("What you'll need")
                Spacer(Modifier.height(8.dp))
                needs.forEachIndexed { i, need ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { if (i in checked) checked.remove(i) else checked.add(i) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (i in checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                            tint = if (i in checked) Palette.Green else Palette.Navy, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(need, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                    }
                }
            }
            if (step.timeEstimate.isNotBlank()) {
                Spacer(Modifier.height(12.dp)); RowDividerFull(); Spacer(Modifier.height(14.dp))
                SectionLabel("How long")
                Spacer(Modifier.height(8.dp))
                IconLine(Icons.Outlined.AccessTime, step.timeEstimate)
            }
            if (step.whereToGetHelp.isNotBlank()) {
                Spacer(Modifier.height(12.dp)); RowDividerFull(); Spacer(Modifier.height(14.dp))
                SectionLabel("Where to get help")
                Spacer(Modifier.height(8.dp))
                val parts = step.whereToGetHelp.split(",", limit = 2)
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Palette.Navy, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(parts[0].trim(), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                        if (parts.size > 1) Text(parts[1].trim(), style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Show me how", { onHowTo(step.id) })
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            "I've done this",
            onClick = {
                scope.launch {
                    container.engine.markDone(studentId, step.id, if (me.role == UserRole.CASEWORKER) Source.CASEWORKER else Source.STUDENT)
                    onDoneRerun()
                }
            },
            icon = Icons.Filled.Check,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { TextLink("See my full path", onPath) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RowDividerFull() = Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))

@Composable
private fun IconLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Palette.Navy, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
    }
}

private fun iconFor(req: RequirementEntity): ImageVector = when (req.kind) {
    org.dsiders.nextstep.data.RequirementKind.DOCUMENT, org.dsiders.nextstep.data.RequirementKind.ARTEFACT -> Icons.Outlined.Description
    else -> Icons.Outlined.School
}

// ---------------------------------------------------------------------------
// Frame 7: Your path
// ---------------------------------------------------------------------------

@Composable
fun PathScreen(
    container: AppContainer, studentId: Long,
    onNextStep: () -> Unit, onStanding: () -> Unit, onStart: () -> Unit, onBack: () -> Unit,
) {
    val gap = rememberGap(container, studentId)
    if (gap == null) { LoadingBlock("Loading your path…"); return }
    val role = gap.role
    if (role == null || gap.path.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            BackRow(onBack)
            EmptyState(
                "No path yet", "Tell us what you want to become and answer a few questions. Your steps will show up here.",
                action = { PrimaryButton("Start", onStart) },
            )
        }
        return
    }
    val remaining = gap.path.filter { it.state != StepState.DONE }
    val currentIndex = gap.path.indexOfFirst { it.state == StepState.CURRENT }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack)
        Text("Your path to ${role.field.lowercase().ifBlank { role.title.lowercase() }}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${gap.path.size + 1} steps.", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            if (currentIndex >= 0) {
                Text("  •  ", color = Palette.OrangeDeep)
                Text("You're on step ${currentIndex + 1}.", style = MaterialTheme.typography.bodyMedium, color = Palette.OrangeDeep)
            }
        }
        Spacer(Modifier.height(16.dp))

        gap.path.forEachIndexed { i, step ->
            PathRow(
                number = i + 1, title = step.requirement.stepTitle(),
                subtitle = step.requirement.timeEstimate.ifBlank { step.requirement.whyNeeded },
                state = step.state, faded = step.state == StepState.UPCOMING && i > currentIndex + 1,
                onClick = if (step.state == StepState.CURRENT) onNextStep else null,
            )
        }
        // The goal itself, pinned at the bottom.
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.padding(top = 18.dp).size(28.dp).clip(CircleShape).background(Palette.Navy), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Flag, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            NsCard(color = Palette.GreySoft.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 6.dp)) {
                SectionLabel("What you want", color = Palette.Navy)
                Text("Work as a ${role.title.lowercase()}", style = MaterialTheme.typography.titleLarge, color = Palette.Navy)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (remaining.isNotEmpty()) SecondaryButton("Back to my next step", onNextStep, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { TextLink("See where I stand", onStanding) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PathRow(number: Int, title: String, subtitle: String, state: StepState, faded: Boolean, onClick: (() -> Unit)?) {
    Row(Modifier.height(IntrinsicSize.Min).alpha(if (faded) 0.55f else 1f)) {
        Box(Modifier.width(36.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.padding(top = 30.dp).width(2.dp).fillMaxHeight().background(if (state == StepState.CURRENT) Palette.OrangeDeep else Palette.Divider))
            val (bg, fg, border) = when (state) {
                StepState.DONE -> Triple(Palette.Green, Color.White, null)
                StepState.CURRENT -> Triple(Palette.OrangeDeep, Color.White, null)
                StepState.UPCOMING -> Triple(Palette.Page, Palette.NavyMuted, Palette.NavyMuted)
            }
            Box(
                Modifier.padding(top = 18.dp).size(26.dp).clip(CircleShape).background(bg)
                    .then(if (border != null) Modifier.border(BorderStroke(1.5.dp, border), CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (state == StepState.DONE) Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(15.dp))
                else Text("$number", color = fg, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.width(10.dp))
        val content: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (state == StepState.CURRENT) {
                        Pill("YOU ARE HERE", background = Palette.Peach, content = Palette.OrangeDeep)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (state == StepState.DONE) {
                        Text("Done", style = MaterialTheme.typography.labelMedium, color = Palette.Green)
                    }
                    Text(title, style = if (state == StepState.CURRENT) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        color = if (state == StepState.UPCOMING) Palette.NavyMuted else Palette.Navy)
                    if (subtitle.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state == StepState.CURRENT) { Icon(Icons.Outlined.AccessTime, null, tint = Palette.Text, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    }
                }
                if (onClick != null) Box(Modifier.size(28.dp).clip(CircleShape).background(Palette.Peach), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.OrangeDeep, modifier = Modifier.size(18.dp))
                }
            }
        }
        Box(Modifier.weight(1f).padding(vertical = 6.dp)) {
            if (state == StepState.CURRENT) AccentCard(Palette.OrangeDeep, onClick = onClick) { content() }
            else NsCard(onClick = onClick, color = if (state == StepState.UPCOMING) Palette.Card.copy(alpha = 0.8f) else Palette.Card) { content() }
        }
    }
}

// ---------------------------------------------------------------------------
// "Show me how"
// ---------------------------------------------------------------------------

@Composable
fun HowToScreen(
    container: AppContainer, me: UserEntity, studentId: Long, reqId: Long,
    onDoneRerun: () -> Unit, onHelp: () -> Unit, onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var req by remember { mutableStateOf<RequirementEntity?>(null) }
    var guide by remember { mutableStateOf<HowToGuide?>(null) }
    LaunchedEffect(reqId) {
        val r = container.repo.db.requirements().byId(reqId)
        req = r
        if (r != null) guide = container.engine.howTo(studentId, r)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack)
        SectionLabel("How to do it", color = Palette.Orange, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(6.dp))
        Text(req?.stepTitle() ?: "", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
        val g = guide
        if (g == null) LoadingBlock("Writing this out for you…")
        else {
            NsCard {
                g.steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Palette.Lavender), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Palette.Navy)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(s, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.weight(1f))
                    }
                    if (i < g.steps.lastIndex) RowDivider()
                }
            }
            if (g.tip.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                NsCard(color = Palette.GreenSoft) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(Palette.Green)
                        Spacer(Modifier.width(10.dp))
                        Text(g.tip, style = MaterialTheme.typography.bodyMedium, color = Palette.Green)
                    }
                }
            }
            if (!g.personalised) {
                Spacer(Modifier.height(8.dp))
                Text("These are your centre's standard notes. Connect to the internet for steps written for you.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
        }
        Spacer(Modifier.height(20.dp))
        req?.let { r ->
            SecondaryButton("I've done this", {
                scope.launch {
                    container.engine.markDone(studentId, r.id, if (me.role == UserRole.CASEWORKER) Source.CASEWORKER else Source.STUDENT)
                    onDoneRerun()
                }
            }, icon = Icons.Filled.Check)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextLink(if (me.role == UserRole.STUDENT) "I'm stuck, ask a mentor" else "Back to student", onHelp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
