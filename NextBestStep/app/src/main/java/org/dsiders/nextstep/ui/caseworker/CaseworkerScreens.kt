package org.dsiders.nextstep.ui.caseworker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.ActivityType
import org.dsiders.nextstep.data.HelpRequestEntity
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.RequestStatus
import org.dsiders.nextstep.data.Source
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.data.UserRole
import org.dsiders.nextstep.engine.GapItem
import org.dsiders.nextstep.engine.GapReasoner
import org.dsiders.nextstep.ui.components.AccentCard
import org.dsiders.nextstep.ui.components.Avatar
import org.dsiders.nextstep.ui.components.BackRow
import org.dsiders.nextstep.ui.components.ChoiceChip
import org.dsiders.nextstep.ui.components.Dot
import org.dsiders.nextstep.ui.components.EmptyState
import org.dsiders.nextstep.ui.components.FieldInput
import org.dsiders.nextstep.ui.components.LoadingBlock
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.Pill
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.RowDivider
import org.dsiders.nextstep.ui.components.SecondaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.Share
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.components.firstName
import org.dsiders.nextstep.ui.components.nextStepMessage
import org.dsiders.nextstep.ui.components.shortName
import org.dsiders.nextstep.ui.components.startOfToday
import org.dsiders.nextstep.ui.components.timeAgo
import org.dsiders.nextstep.ui.student.ChoiceDialog
import org.dsiders.nextstep.ui.student.EditTextDialog
import org.dsiders.nextstep.ui.student.LANGUAGES
import org.dsiders.nextstep.ui.student.SettingRow
import org.dsiders.nextstep.ui.student.rememberGap
import org.dsiders.nextstep.ui.theme.Palette

// ---------------------------------------------------------------------------
// Frame 9: My students
// ---------------------------------------------------------------------------

data class StudentRow(
    val user: UserEntity,
    val goalLine: String,
    val statusLine: String,
    val statusColor: Color,
    val lastUpdate: Long,
    val request: HelpRequestEntity?,
    val waitingOnStudent: Boolean,
)

private const val WAITING_DAYS = 3

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudentsViewModel(private val c: AppContainer) : ViewModel() {
    val rows = combine(
        c.repo.observeStudents(), c.repo.observeAllGoals(), c.repo.observeAllStatuses(), c.repo.observeRequests(), c.repo.observeRoles(),
    ) { students, goals, statuses, requests, roles -> Triple(students, goals, Triple(statuses, requests, roles)) }
        .mapLatest { (students, goals, rest) ->
            val (statuses, requests, roles) = rest
            students.map { s ->
                val goal = goals.firstOrNull { it.studentId == s.id }
                val gap = c.engine.analyse(s.id)
                val mine = statuses.filter { it.studentId == s.id }
                val last = maxOf(goal?.updatedAt ?: s.createdAt, mine.maxOfOrNull { it.updatedAt } ?: 0L)
                val req = requests.firstOrNull { it.studentId == s.id && it.status != RequestStatus.RESOLVED }
                val role = roles.firstOrNull { it.id == goal?.targetRoleId }
                val latest = mine.maxByOrNull { it.updatedAt }
                val latestReq = latest?.let { l -> gap.allItems.firstOrNull { it.requirement.id == l.requirementId } }
                val (line, color) = when {
                    req != null -> req.reason to Palette.AmberText
                    goal == null -> "Hasn't set a goal yet" to Palette.Grey
                    role == null -> "Needs help choosing a direction" to Palette.Grey
                    gap.readyForRole -> "Ready to apply" to Palette.Green
                    latest?.status == ReqStatus.HAVE && latestReq != null -> "Finished: ${latestReq.requirement.name.lowercase().removePrefix("a ").removePrefix("the ")}" to Palette.Green
                    gap.nextStep != null && latestReq?.status == ReqStatus.PARTIAL -> "On track, ${latestReq.requirement.stepTitle().replaceFirstChar { it.lowercase() }}" to Palette.Green
                    gap.nextStep != null -> "Missing: ${gap.nextStep.name.lowercase().removePrefix("a ").removePrefix("the ")}" to Palette.Amber
                    else -> "On track" to Palette.Green
                }
                val waiting = req == null && goal != null && !gap.readyForRole &&
                    System.currentTimeMillis() - last > WAITING_DAYS * 24 * 3_600_000L
                StudentRow(
                    user = s,
                    goalLine = goalLine(goal?.goalText, role?.title),
                    statusLine = if (waiting && color != Palette.Green) "Waiting on student" else line,
                    statusColor = if (waiting && color != Palette.Green) Palette.Grey else color,
                    lastUpdate = req?.createdAt ?: last, request = req, waitingOnStudent = waiting,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun goalLine(goalText: String?, roleTitle: String?): String {
        val t = goalText?.trim().orEmpty()
        return when {
            t.length in 1..40 && t.startsWith("I want to ", true) -> "Wants to " + t.substring(10)
            t.length in 1..40 && t.startsWith("I want ", true) -> "Wants " + t.substring(7)
            roleTitle != null -> "Wants to become ${if (roleTitle.first().lowercaseChar() in "aeiou") "an" else "a"} ${roleTitle.lowercase()}"
            t.isNotEmpty() -> "Not sure what they want yet"
            else -> "No goal yet"
        }
    }
}

private enum class StudentFilter { HELP, ALL, WAITING }

@Composable
fun StudentsScreen(container: AppContainer, me: UserEntity, onStudent: (Long) -> Unit, onAddStudent: () -> Unit, onProfile: () -> Unit) {
    val vm: StudentsViewModel = viewModel { StudentsViewModel(container) }
    val rows by vm.rows.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(StudentFilter.HELP) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("My students", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton({ searching = !searching; if (!searching) query = "" }) { Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, "Search", tint = Palette.Navy) }
            Box(Modifier.clickable(onClick = onProfile)) { Avatar(me.fullName, size = 38.dp, background = Palette.Lavender) }
        }
        if (searching) FieldInput(query, { query = it }, "Search by name", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

        val all = rows
        if (all == null) { LoadingBlock("Loading students…"); return@Column }
        val visible = all.filter { query.isBlank() || it.user.fullName.contains(query, true) }
        val help = visible.filter { it.request != null }.sortedByDescending { it.lastUpdate }
        val waiting = visible.filter { it.waitingOnStudent }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("Asked for help (${help.size})", filter == StudentFilter.HELP, { filter = StudentFilter.HELP })
            ChoiceChip("All (${visible.size})", filter == StudentFilter.ALL, { filter = StudentFilter.ALL })
            ChoiceChip("Waiting on student (${waiting.size})", filter == StudentFilter.WAITING, { filter = StudentFilter.WAITING })
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val rest = visible.filter { it.request == null }.sortedByDescending { it.lastUpdate }
            when (filter) {
                StudentFilter.WAITING -> {
                    if (waiting.isEmpty()) EmptyState("No one is waiting", "Students who haven't moved in $WAITING_DAYS days show up here.")
                    waiting.forEach { StudentListCard(it, highlight = false) { onStudent(it.user.id) } }
                }
                else -> {
                    if (help.isNotEmpty()) {
                        SectionLabel("Asked for help", modifier = Modifier.padding(vertical = 6.dp))
                        help.forEach { StudentListCard(it, highlight = true) { onStudent(it.user.id) } }
                        Spacer(Modifier.height(10.dp))
                    } else if (filter == StudentFilter.HELP) {
                        NsCard(color = Palette.GreenSoft) { Text("No one has asked for help right now.", color = Palette.Green, style = MaterialTheme.typography.bodyMedium) }
                        Spacer(Modifier.height(10.dp))
                    }
                    SectionLabel("Everyone else", modifier = Modifier.padding(vertical = 6.dp))
                    rest.forEach { StudentListCard(it, highlight = false) { onStudent(it.user.id) } }
                    if (visible.isEmpty()) EmptyState("No students yet", "Add a student to run their first session with them.")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.padding(16.dp)) { PrimaryButton("Add a student", onAddStudent, icon = Icons.Filled.Add) }
    }
}

@Composable
private fun StudentListCard(row: StudentRow, highlight: Boolean, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (highlight) { Avatar(row.user.fullName, size = 42.dp); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(shortName(row.user.fullName), style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                Text(row.goalLine, style = MaterialTheme.typography.bodyMedium, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(row.statusColor, 7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(row.statusLine, style = MaterialTheme.typography.bodySmall, color = if (row.statusColor == Palette.Amber) Palette.AmberText else row.statusColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(timeAgo(row.lastUpdate), style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.TextMuted, modifier = Modifier.size(18.dp))
        }
    }
    Box(Modifier.padding(vertical = 5.dp)) {
        if (highlight) AccentCard(Palette.Amber, onClick = onClick) { content() }
        else NsCard(onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)) { content() }
    }
}

// ---------------------------------------------------------------------------
// Requests tab
// ---------------------------------------------------------------------------

@Composable
fun RequestsScreen(container: AppContainer, me: UserEntity, onStudent: (Long) -> Unit) {
    val requests by remember { container.repo.observeRequests() }.collectAsState(initial = emptyList())
    val students by remember { container.repo.observeStudents() }.collectAsState(initial = emptyList())
    val mine = requests.filter { it.caseworkerId == null || it.caseworkerId == me.id }
    val others = requests.filter { it.caseworkerId != null && it.caseworkerId != me.id && it.status != RequestStatus.RESOLVED }
    val open = mine.filter { it.status != RequestStatus.RESOLVED }.sortedWith(compareBy({ it.status != RequestStatus.NEW }, { -it.createdAt }))
    val done = mine.filter { it.status == RequestStatus.RESOLVED }
    var showDone by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Requests", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Text("Students who asked for a person. Newest first.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(14.dp))
        if (open.isEmpty()) EmptyState("All caught up", "When the app isn't sure, or a student asks for someone, it shows up here.")
        open.forEach { r -> RequestCard(r, students.firstOrNull { it.id == r.studentId }) { onStudent(r.studentId) } }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("With other caseworkers (${others.size})")
            Spacer(Modifier.height(6.dp))
            others.forEach { r -> RequestCard(r, students.firstOrNull { it.id == r.studentId }) { onStudent(r.studentId) } }
        }
        if (done.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            TextLink(if (showDone) "Hide sorted (${done.size})" else "Show sorted (${done.size})", { showDone = !showDone })
            if (showDone) done.forEach { r -> RequestCard(r, students.firstOrNull { it.id == r.studentId }) { onStudent(r.studentId) } }
        }
    }
}

@Composable
private fun RequestCard(r: HelpRequestEntity, student: UserEntity?, onClick: () -> Unit) {
    Box(Modifier.padding(vertical = 5.dp)) {
        AccentCard(if (r.status == RequestStatus.NEW) Palette.Orange else Palette.Divider, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(student?.fullName ?: "?", size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(student?.let { shortName(it.fullName) } ?: "Student", style = MaterialTheme.typography.titleMedium)
                    Text(r.reason, style = MaterialTheme.typography.bodyMedium, color = Palette.AmberText)
                    if (r.message.isNotBlank()) Text("“${r.message}”", style = MaterialTheme.typography.bodySmall, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(timeAgo(r.createdAt), style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    Spacer(Modifier.height(4.dp))
                    when (r.status) {
                        RequestStatus.NEW -> Pill("New", background = Palette.OrangeSoft, content = Palette.OrangeDeep)
                        RequestStatus.SEEN -> Pill("Seen")
                        RequestStatus.RESOLVED -> Pill("Sorted", background = Palette.GreenSoft, content = Palette.Green)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Frame 10: Student detail
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentDetailScreen(
    container: AppContainer, me: UserEntity, studentId: Long,
    onRunQuestions: () -> Unit, onStartIntake: () -> Unit, onStanding: () -> Unit, onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val student by remember { container.repo.observeUser(studentId) }.collectAsState(initial = null)
    val goal by remember { container.repo.observeGoal(studentId) }.collectAsState(initial = null)
    val facts by remember { container.repo.observeFacts(studentId) }.collectAsState(initial = emptyList())
    val notes by remember { container.repo.observeNotes(studentId) }.collectAsState(initial = emptyList())
    val requests by remember { container.repo.observeRequestsFor(studentId) }.collectAsState(initial = emptyList())
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())
    val gap = rememberGap(container, studentId)
    val openedAt = remember { System.currentTimeMillis() }
    var elapsed by remember { mutableLongStateOf(0L) }
    var laterOpen by remember { mutableStateOf(false) }
    var addDetail by remember { mutableStateOf(false) }
    var addNote by remember { mutableStateOf(false) }
    var changeGoal by remember { mutableStateOf(false) }
    var resetPassword by remember { mutableStateOf(false) }
    var askGoal by remember { mutableStateOf(false) }
    var reply by rememberSaveable { mutableStateOf("") }
    val openRequest = requests.firstOrNull { it.status != RequestStatus.RESOLVED }

    LaunchedEffect(Unit) { while (true) { elapsed = (System.currentTimeMillis() - openedAt) / 1000; delay(1000) } }
    LaunchedEffect(openRequest?.id) { openRequest?.let { container.repo.markRequestSeen(it.id) } }
    // Count the visit even if the caseworker leaves with the system back button.
    DisposableEffect(Unit) {
        onDispose {
            val secs = (System.currentTimeMillis() - openedAt) / 1000
            if (secs >= 5) container.appScope.launch { container.repo.logActivity(me.id, studentId, ActivityType.STUDENT_SEEN, secs) }
        }
    }

    val s = student
    if (s == null || gap == null) { LoadingBlock("Loading student…"); return }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            BackRow(onBack, title = shortName(s.fullName)) {
                Pill("%d:%02d".format(elapsed / 60, elapsed % 60), leading = Icons.Outlined.Timer, background = Palette.GreySoft, content = Palette.Text)
            }
            NsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(s.fullName, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val sub = listOf(s.course, s.year.lowercase()).filter { it.isNotBlank() }.joinToString(", ")
                        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = Palette.Text)
                        Text(
                            gap.role?.let { "Wants to become ${if (it.title.first().lowercaseChar() in "aeiou") "an" else "a"} ${it.title.lowercase()}" }
                                ?: goal?.goalText?.let { "“$it”" } ?: "No goal yet",
                            style = MaterialTheme.typography.titleMedium, color = Palette.Navy,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(s.year, s.district).filter { it.isNotBlank() }.forEach { Pill(it) }
                facts.filter { f -> listOf("studying", "year", "district").none { it.equals(f.label, true) } }.forEach { f ->
                    Pill(if (f.value.length <= 14 && !f.value.equals("yes", true)) "${f.label}: ${f.value}" else f.label)
                }
                gap.nextStep?.let { Pill("● No ${it.name.lowercase().removePrefix("a ").removePrefix("the ")}", background = Palette.AmberSoft, content = Palette.AmberText) }
                Pill("+ Add detail", background = Palette.Card, onClick = { addDetail = true })
            }

            openRequest?.let { r ->
                Spacer(Modifier.height(14.dp))
                AccentCard(Palette.Orange, color = Palette.OrangeSoft.copy(alpha = 0.6f)) {
                    SectionLabel("Asked for help · ${timeAgo(r.createdAt)}", color = Palette.OrangeDeep)
                    Text(r.reason, style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
                    if (r.message.isNotBlank()) Text("“${r.message}”", style = MaterialTheme.typography.bodyMedium)
                    if (r.reply.isNotBlank()) Text("You replied: ${r.reply}", style = MaterialTheme.typography.bodySmall, color = Palette.Green)
                    Spacer(Modifier.height(8.dp))
                    FieldInput(reply, { reply = it }, "Reply to ${firstName(s.fullName)}", singleLine = false)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Send reply", {
                            scope.launch { container.repo.replyToRequest(r.id, me.id, reply, resolve = false); reply = "" }
                        }, modifier = Modifier.weight(1f), enabled = reply.isNotBlank())
                        SecondaryButton("Mark sorted", {
                            scope.launch { container.repo.replyToRequest(r.id, me.id, reply, resolve = true); reply = "" }
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (gap.role == null) {
                Spacer(Modifier.height(14.dp))
                NsCard {
                    Text("No target role yet", style = MaterialTheme.typography.titleMedium, color = Palette.Navy)
                    Text("Pick one with them, or run the questions together.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Choose a role with them", { changeGoal = true })
                }
            } else {
                Spacer(Modifier.height(16.dp))
                DetailGroup("Has", gap.have, Palette.Green, studentId, container, me)
                DetailGroup("Missing", gap.missing, Palette.Amber, studentId, container, me)
                if (gap.later.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Dot(Palette.Grey); Spacer(Modifier.width(8.dp))
                        Text("LATER (${gap.later.size})", style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
                    }
                    AccentCard(Palette.Grey, onClick = { laterOpen = !laterOpen }) {
                        if (!laterOpen) Text("Advanced skills, not urgent", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                        else gap.later.forEach { CompactRow(it, studentId, container, me) }
                    }
                }
                Text(
                    "Confidence ${"%.0f".format(gap.confidence * 100)}%" + if (gap.confidence < GapReasoner.CONFIDENCE_THRESHOLD) " · below the handover line" else "",
                    style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(top = 8.dp),
                )
            }

            gap.nextStep?.let { step ->
                Spacer(Modifier.height(16.dp))
                AccentCard(Palette.Orange, color = Palette.OrangeSoft.copy(alpha = 0.7f)) {
                    SectionLabel("Next step", color = Palette.OrangeDeep)
                    Text(
                        step.stepTitle() + (step.whereToGetHelp.substringBefore(',').takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium, color = Palette.Navy,
                    )
                    Spacer(Modifier.height(10.dp))
                    val message = nextStepMessage(s, step, gap.role?.title?.lowercase())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("WhatsApp", {
                            Share.whatsApp(context, s.phone, message)
                            scope.launch { container.repo.logActivity(me.id, studentId, ActivityType.NEXT_STEP_SHARED) }
                        }, modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.Chat)
                        SecondaryButton("Print", {
                            Share.print(context, "Next step for ${s.fullName}", message)
                            scope.launch { container.repo.logActivity(me.id, studentId, ActivityType.NEXT_STEP_SHARED) }
                        }, modifier = Modifier.weight(1f), icon = Icons.Filled.Print)
                    }
                    if (s.phone.isBlank()) Text("No phone saved. WhatsApp will ask who to send to.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            NsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("My notes", modifier = Modifier.weight(1f))
                    TextLink("Add a note", { addNote = true }, underline = false)
                }
                if (notes.isEmpty()) Text("Nothing yet", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                notes.forEach { n ->
                    Spacer(Modifier.height(6.dp))
                    Text(n.text, style = MaterialTheme.typography.bodyMedium)
                    Text(timeAgo(n.createdAt), style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("With the student")
            Spacer(Modifier.height(8.dp))
            NsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                ActionRow(if (goal == null) "Start their first session" else "Ask the questions together") { if (goal == null) askGoal = true else onRunQuestions() }
                RowDivider()
                ActionRow("Start over with a new goal") { askGoal = true }
                RowDivider()
                ActionRow("See what the student sees", onStanding)
                RowDivider()
                ActionRow("Change their target role") { changeGoal = true }
                RowDivider()
                ActionRow("Set a new password") { resetPassword = true }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(16.dp)) {
            PrimaryButton("Save and go back", onBack)
        }
    }

    if (addDetail) AddDetailDialog(onDismiss = { addDetail = false }) { label, value ->
        scope.launch { container.repo.upsertFact(studentId, label, value, Source.CASEWORKER) }
        addDetail = false
    }
    if (addNote) EditTextDialog("Add a note", "", onDismiss = { addNote = false }, label = "Note") { text ->
        scope.launch { container.repo.addNote(studentId, me.id, text) }
        addNote = false
    }
    if (askGoal) EditTextDialog("What do they want to become?", "", onDismiss = { askGoal = false }, label = "In their own words") { text ->
        scope.launch { container.repo.startGoal(studentId, text); container.engine.invalidate(studentId); onStartIntake() }
        askGoal = false
    }
    if (changeGoal) ChoiceDialog("Target role", roles.map { it.title to it.id }, goal?.targetRoleId, { changeGoal = false }) { rid ->
        scope.launch { container.repo.setTargetRole(studentId, rid); container.engine.invalidate(studentId) }
        changeGoal = false
    }
    if (resetPassword) EditTextDialog("New password for ${firstName(s.fullName)}", "", onDismiss = { resetPassword = false }, label = "At least 8 characters") { pw ->
        if (pw.length < 8) {
            Toast.makeText(context, "Use at least 8 characters", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                val salt = org.dsiders.nextstep.data.Passwords.newSalt()
                container.repo.updateUser(s.copy(salt = salt, passwordHash = org.dsiders.nextstep.data.Passwords.hash(pw, salt)))
                Toast.makeText(context, "Password changed. Tell ${firstName(s.fullName)} in person.", Toast.LENGTH_LONG).show()
            }
            resetPassword = false
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.Navy, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
    }
}

@Composable
private fun DetailGroup(title: String, items: List<GapItem>, color: Color, studentId: Long, container: AppContainer, me: UserEntity) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Dot(color); Spacer(Modifier.width(8.dp))
        Text("${title.uppercase()} (${items.size})", style = MaterialTheme.typography.labelSmall, color = if (color == Palette.Amber) Palette.AmberText else color)
    }
    items.forEach { item ->
        Box(Modifier.padding(vertical = 4.dp)) { AccentCard(color) { CompactRow(item, studentId, container, me) } }
    }
    Spacer(Modifier.height(6.dp))
}

/** One requirement row with an Override on it, as in frame 10. */
@Composable
private fun CompactRow(item: GapItem, studentId: Long, container: AppContainer, me: UserEntity) {
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.requirement.name, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
            Text(item.reason, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.source == Source.CASEWORKER) Text("Overridden by a caseworker", style = MaterialTheme.typography.bodySmall, color = Palette.Navy)
        }
        Box {
            TextButton({ menu = true }) { Text("Override", color = Palette.Navy, style = MaterialTheme.typography.titleSmall) }
            DropdownMenu(menu, { menu = false }) {
                fun set(status: ReqStatus, evidence: String) {
                    menu = false
                    scope.launch {
                        container.repo.setStatus(studentId, item.requirement.id, status, 1f, evidence, Source.CASEWORKER)
                        container.engine.invalidate(studentId)
                    }
                }
                DropdownMenuItem({ Text("They have this") }, { set(ReqStatus.HAVE, "Checked by ${firstName(me.fullName)}: you have this.") })
                DropdownMenuItem({ Text("They're missing this") }, { set(ReqStatus.MISSING, "Checked by ${firstName(me.fullName)}: still to do.") })
                DropdownMenuItem({ Text("They've started this") }, { set(ReqStatus.PARTIAL, "Checked by ${firstName(me.fullName)}: started.") })
                DropdownMenuItem({ Text("Not urgent for them") }, { set(ReqStatus.LATER, "Your mentor says this can wait.") })
                DropdownMenuItem({ Text("Clear, ask again") }, {
                    menu = false
                    scope.launch { container.repo.clearStatus(studentId, item.requirement.id); container.engine.invalidate(studentId) }
                })
            }
        }
    }
}

@Composable
private fun AddDetailDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a detail") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldInput(label, { label = it }, "What, e.g. Used Excel")
                FieldInput(value, { value = it }, "Detail, e.g. Yes, in college")
            }
        },
        confirmButton = { TextButton({ onSave(label.trim(), value.trim().ifBlank { "Yes" }) }, enabled = label.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------------
// Add a student (caseworker-led intake, as in the proposed workflow)
// ---------------------------------------------------------------------------

@Composable
fun AddStudentScreen(container: AppContainer, me: UserEntity, onCreated: (Long, Boolean) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var usernameEdited by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("barabari" + (1000..9999).random()) }
    var phone by rememberSaveable { mutableStateOf("") }
    var college by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("Final year") }
    var district by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("Telugu") }
    var goal by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pickLanguage by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack, title = "Add a student")
        Text("Fill in what you know now. Everything can be changed later.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldInput(name, {
                name = it
                if (!usernameEdited) username = it.trim().lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
            }, "Full name")
            FieldInput(username, { username = it.trim(); usernameEdited = true; error = null }, "Username for the student")
            FieldInput(password, { password = it }, "Starting password", supportingText = "Give this to the student. At least 8 characters.")
            FieldInput(phone, { phone = it }, "Phone (for WhatsApp)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            FieldInput(college, { college = it }, "College")
            FieldInput(course, { course = it }, "Course, e.g. B.Com")
            FieldInput(year, { year = it }, "Year")
            FieldInput(district, { district = it }, "District")
            NsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                SettingRow(Icons.Outlined.Language, "Language for questions", language) { pickLanguage = true }
            }
            FieldInput(goal, { goal = it }, "What do they want to become? (optional)", singleLine = false,
                supportingText = "If you fill this in, you'll go straight to the questions with them.")
        }
        error?.let { Text(it, color = Palette.AmberText, modifier = Modifier.padding(vertical = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Add student", {
            busy = true
            scope.launch {
                container.repo.register(UserRole.STUDENT, name, username, password, phone, college, course, year, district, addedBy = me.id)
                    .onSuccess { user ->
                        container.repo.updateUser(user.copy(language = language))
                        if (goal.isNotBlank()) container.repo.startGoal(user.id, goal)
                        onCreated(user.id, goal.isNotBlank())
                    }
                    .onFailure { error = it.message }
                busy = false
            }
        }, enabled = name.isNotBlank() && username.isNotBlank() && !username.contains(' ') && password.length >= 8, loading = busy)
        Spacer(Modifier.height(20.dp))
    }
    if (pickLanguage) ChoiceDialog("Language", LANGUAGES.map { it to it }, language, { pickLanguage = false }) { language = it; pickLanguage = false }
}

// ---------------------------------------------------------------------------
// Frame 11: Caseworker profile
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseworkerProfileScreen(container: AppContainer, me: UserEntity) {
    val scope = rememberCoroutineScope()
    val requests by remember { container.repo.observeRequests() }.collectAsState(initial = emptyList())
    val today by remember { container.repo.observeActivitySince(me.id, startOfToday()) }.collectAsState(initial = emptyList())
    var addTopic by remember { mutableStateOf(false) }
    var removeTopic by remember { mutableStateOf<String?>(null) }
    var editLanguages by remember { mutableStateOf(false) }
    var editHours by remember { mutableStateOf(false) }
    var editCentre by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val waiting = requests.count { it.status != RequestStatus.RESOLVED && (it.caseworkerId == null || it.caseworkerId == me.id) }
    val replied = requests.filter { it.caseworkerId == me.id && it.repliedAt != null }
    val replyMins = if (replied.isEmpty()) null else replied.map { (it.repliedAt!! - it.createdAt) / 60_000 }.average().toLong()
    val seen = today.filter { it.type == ActivityType.STUDENT_SEEN }
    val avgSecs = if (seen.isEmpty()) 0L else seen.map { it.durationSec }.average().toLong()
    val topics = me.topics.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(48.dp))
            Text("My profile", style = MaterialTheme.typography.titleLarge, color = Palette.Navy, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton({ showSettings = true }) { Icon(Icons.Filled.Settings, "Settings", tint = Palette.Navy) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(me.fullName, size = 64.dp, background = Palette.Lavender)
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(me.fullName, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Verified, null, tint = Palette.Green, modifier = Modifier.size(18.dp))
                }
                Text("Caseworker, Barabari Collective", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                Row(Modifier.clickable { editCentre = true }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Palette.TextMuted, modifier = Modifier.size(15.dp))
                    Text(me.centre.ifBlank { "Add your centre" }, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AccentCard(if (me.available) Palette.Green else Palette.Grey) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(if (me.available) Palette.Green else Palette.Grey)
                Spacer(Modifier.width(8.dp))
                Text("Available to help", style = MaterialTheme.typography.titleMedium, color = Palette.Navy, modifier = Modifier.weight(1f))
                Switch(
                    me.available, { on -> scope.launch { container.repo.updateUser(me.copy(available = on)) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Green, checkedThumbColor = Color.White),
                )
            }
            Text(
                if (me.available) "Students can message you when the app isn't sure." else "Students won't be offered you until you switch this on.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Text,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("$waiting waiting", background = Palette.AmberSoft, content = Palette.AmberText)
                Pill(replyMins?.let { "Replies in ${if (it < 60) "$it min" else "${it / 60} h"}" } ?: "No replies yet", background = Palette.GreySoft, content = Palette.Text)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Today")
        Spacer(Modifier.height(8.dp))
        NsCard {
            Row(Modifier.fillMaxWidth()) {
                Stat("${seen.map { it.studentId }.distinct().size}", "Students seen", Modifier.weight(1f))
                Stat("%d:%02d".format(avgSecs / 60, avgSecs % 60), "Average time", Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Palette.Page))
                Stat("${today.count { it.type == ActivityType.NEXT_STEP_SHARED }}", "Next steps given", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("What I help with")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { t -> Pill(t, onClick = { removeTopic = t }) }
            Pill("+ Add", background = Palette.Card, onClick = { addTopic = true })
        }

        Spacer(Modifier.height(18.dp))
        NsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            SettingRow(Icons.Outlined.Language, "Languages", me.languages) { editLanguages = true }
            RowDivider()
            SettingRow(Icons.Outlined.AccessTime, "Working hours", me.workingHours) { editHours = true }
            RowDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Notifications, null, tint = Palette.Navy, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Notifications", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(me.notificationsOn, { on -> scope.launch { container.repo.updateUser(me.copy(notificationsOn = on)) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Navy))
            }
        }
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Sign out", { container.session.signOut() }, icon = Icons.AutoMirrored.Filled.Logout)
        Spacer(Modifier.height(12.dp))
    }

    if (addTopic) EditTextDialog("Add a topic", "", onDismiss = { addTopic = false }, label = "e.g. Interview practice") { t ->
        scope.launch { container.repo.updateUser(me.copy(topics = (topics + t).distinct().joinToString(", "))) }
        addTopic = false
    }
    removeTopic?.let { t ->
        AlertDialog(
            onDismissRequest = { removeTopic = null },
            title = { Text("Remove “$t”?") },
            confirmButton = { TextButton({ scope.launch { container.repo.updateUser(me.copy(topics = (topics - t).joinToString(", "))) }; removeTopic = null }) { Text("Remove") } },
            dismissButton = { TextButton({ removeTopic = null }) { Text("Keep") } },
        )
    }
    if (editLanguages) EditTextDialog("Languages", me.languages, onDismiss = { editLanguages = false }, label = "Comma separated") { v ->
        scope.launch { container.repo.updateUser(me.copy(languages = v)) }; editLanguages = false
    }
    if (editHours) EditTextDialog("Working hours", me.workingHours, onDismiss = { editHours = false }) { v ->
        scope.launch { container.repo.updateUser(me.copy(workingHours = v)) }; editHours = false
    }
    if (editCentre) EditTextDialog("Your centre", me.centre, onDismiss = { editCentre = false }, label = "e.g. Kukatpally centre") { v ->
        scope.launch { container.repo.updateUser(me.copy(centre = v)) }; editCentre = false
    }
    if (showSettings) AiSettingsDialog(container) { showSettings = false }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Text, textAlign = TextAlign.Center)
    }
}

/** Shows whether OpenAI is set up and lets the caseworker test the connection. */
@Composable
fun AiSettingsDialog(container: AppContainer, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI connection") },
        text = {
            Column {
                if (container.ai.isConfigured) {
                    Text("Using OpenAI model ${container.ai.modelName}.", style = MaterialTheme.typography.bodyMedium)
                    Text("Questions are written for each student. Gaps are always checked against your roles, never guessed.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                } else {
                    Text("No OpenAI key in this build.", style = MaterialTheme.typography.bodyMedium)
                    Text("The app still works, with simpler standard questions. Add OPENAI_API_KEY to local.properties and rebuild to switch it on.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                }
                result?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.Navy) }
            }
        },
        confirmButton = {
            TextButton({
                testing = true
                scope.launch {
                    result = try {
                        container.ai.json("Reply with JSON {\"ok\": true}.", "ping")
                        "Connected. ✓"
                    } catch (e: Exception) { e.message ?: "Couldn't connect." }
                    testing = false
                }
            }, enabled = container.ai.isConfigured && !testing) { Text(if (testing) "Testing…" else "Test connection") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Close") } },
    )
}
