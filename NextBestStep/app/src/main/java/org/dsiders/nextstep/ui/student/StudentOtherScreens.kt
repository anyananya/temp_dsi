package org.dsiders.nextstep.ui.student

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.FactEntity
import org.dsiders.nextstep.data.RequestStatus
import org.dsiders.nextstep.data.ReqStatus
import org.dsiders.nextstep.data.Source
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.engine.GapResult
import org.dsiders.nextstep.ui.components.AccentCard
import org.dsiders.nextstep.ui.components.Avatar
import org.dsiders.nextstep.ui.components.BackRow
import org.dsiders.nextstep.ui.components.CountBadge
import org.dsiders.nextstep.ui.components.FieldInput
import org.dsiders.nextstep.ui.components.KeyValueRow
import org.dsiders.nextstep.ui.components.LoadingBlock
import org.dsiders.nextstep.ui.components.MentorCard
import org.dsiders.nextstep.ui.components.MessageComposer
import org.dsiders.nextstep.ui.components.MessageThread
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.Pill
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.RowDivider
import org.dsiders.nextstep.ui.components.SecondaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.Share
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.components.firstName
import org.dsiders.nextstep.ui.components.pickMentor
import org.dsiders.nextstep.ui.theme.Palette

// ---------------------------------------------------------------------------
// Help tab
// ---------------------------------------------------------------------------

@Composable
fun StudentHelpScreen(container: AppContainer, me: UserEntity) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())
    val requests by remember { container.repo.observeRequestsFor(me.id) }.collectAsState(initial = emptyList())
    val messages by remember { container.repo.observeMessagesForStudent(me.id) }.collectAsState(initial = emptyList())
    val gap = rememberGap(container, me.id)
    var asking by remember { mutableStateOf(false) }
    val mentor = pickMentor(me, caseworkers, gap?.nextStep?.name)
    val open = requests.any { it.status != RequestStatus.RESOLVED }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Help", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Text("A Barabari mentor can look at your plan with you.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
        MentorCard(
            mentor = mentor, headline = "Your mentor",
            body = if (open) "Your conversation is below. You can keep writing until it's sorted." else "Stuck, unsure, or just want a second opinion? Send a message.",
            requestSent = open, onMessage = { asking = true },
        )
        if (mentor != null && mentor.phone.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            TextLink("Message on WhatsApp instead", {
                Share.whatsApp(context, mentor.phone, "Namaste ${firstName(mentor.fullName)}, this is ${me.fullName} from the Next Best Step app.")
            })
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("Your messages")
        Spacer(Modifier.height(8.dp))
        if (requests.isEmpty()) {
            Text("Nothing yet.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
        }
        requests.forEach { r ->
            val name = caseworkers.firstOrNull { it.id == r.caseworkerId }?.fullName?.let { firstName(it) } ?: "Mentor"
            val thread = messages.filter { it.requestId == r.id }
            NsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.reason, style = MaterialTheme.typography.titleSmall, color = Palette.Navy, modifier = Modifier.weight(1f))
                    when (r.status) {
                        RequestStatus.NEW -> Pill("Sent to $name", background = Palette.AmberSoft, content = Palette.AmberText)
                        RequestStatus.SEEN -> Pill("$name has seen it", background = Palette.Lavender)
                        RequestStatus.RESOLVED -> Pill("Sorted", background = Palette.GreenSoft, content = Palette.Green)
                    }
                }
                Spacer(Modifier.height(10.dp))
                MessageThread(thread, viewerIsStudent = true, otherName = name)
                if (r.status != RequestStatus.RESOLVED) {
                    Spacer(Modifier.height(10.dp))
                    MessageComposer("Write to $name", onSend = { text ->
                        scope.launch { container.repo.askForHelp(me.id, r.caseworkerId ?: mentor?.id, r.reason, text) }
                    })
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (asking) {
        AskMentorDialog(mentor?.fullName, onDismiss = { asking = false }, onSend = { msg ->
            scope.launch {
                container.repo.askForHelp(me.id, mentor?.id, gap?.handoverReason ?: "Asked to talk to someone", msg)
            }
            asking = false
        })
    }
}

// ---------------------------------------------------------------------------
// Frame 8: My profile
// ---------------------------------------------------------------------------

val LANGUAGES = listOf("English", "Telugu", "Hindi")

@Composable
fun StudentProfileScreen(container: AppContainer, me: UserEntity, onPath: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val facts by remember { container.repo.observeFacts(me.id) }.collectAsState(initial = emptyList())
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())
    val gap = rememberGap(container, me.id)
    var editProfile by remember { mutableStateOf(false) }
    var editFact by remember { mutableStateOf<FactEntity?>(null) }
    var editStatus by remember { mutableStateOf<org.dsiders.nextstep.engine.GapItem?>(null) }
    var pickLanguage by remember { mutableStateOf(false) }
    var pickMentorDialog by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val json = container.repo.exportStudent(me.id)
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
            Toast.makeText(context, "Saved your details", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(48.dp))
            Text("My profile", style = MaterialTheme.typography.titleLarge, color = Palette.Navy, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextLink("Edit", { editProfile = true }, underline = false)
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(me.fullName, size = 84.dp)
            Spacer(Modifier.height(10.dp))
            Text(me.fullName, style = MaterialTheme.typography.headlineSmall)
            val sub = listOf(me.course, me.year.lowercase()).filter { it.isNotBlank() }.joinToString(", ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            if (me.college.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = Palette.TextMuted, modifier = Modifier.size(15.dp))
                Text(me.college, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
        }
        Spacer(Modifier.height(16.dp))

        val g = gap
        AccentCard(Palette.OrangeDeep, onClick = onPath) {
            SectionLabel("What I want to become", color = Palette.OrangeDeep)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(g?.role?.title ?: "Not decided yet", style = MaterialTheme.typography.titleLarge, color = Palette.Navy)
                    if (g != null && g.path.isNotEmpty()) Text("${g.doneCount} of ${g.path.size} steps done", style = MaterialTheme.typography.bodySmall, color = Palette.Text)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("What I know about you")
        Spacer(Modifier.height(8.dp))
        NsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            val rows = mutableListOf<@Composable () -> Unit>()
            if (me.course.isNotBlank()) rows += { KeyValueRow("Studying", me.course, onClick = { editProfile = true }, trailing = Icons.Filled.Edit) }
            if (me.year.isNotBlank()) rows += { KeyValueRow("Year", me.year, onClick = { editProfile = true }, trailing = Icons.Filled.Edit) }
            if (me.district.isNotBlank()) rows += { KeyValueRow("District", me.district, onClick = { editProfile = true }, trailing = Icons.Filled.Edit) }
            facts.filter { f -> listOf("studying", "year", "district").none { it.equals(f.label, true) } }.forEach { f ->
                rows += { KeyValueRow(f.label, f.value, onClick = { editFact = f }, trailing = Icons.Filled.Edit) }
            }
            g?.let { gr ->
                (gr.have + gr.missing).forEach { item ->
                    val value = when (item.status) {
                        ReqStatus.HAVE -> "Have it"
                        ReqStatus.PARTIAL -> "Started"
                        ReqStatus.MISSING -> "Not yet"
                        else -> "Not sure"
                    }
                    rows += {
                        KeyValueRow(item.requirement.name, value,
                            valueColor = if (item.status == ReqStatus.HAVE) Palette.Green else Palette.Orange,
                            onClick = { editStatus = item }, trailing = Icons.Filled.Edit)
                    }
                }
            }
            if (rows.isEmpty()) Text("Nothing yet. Tap Edit to add your course and district.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            rows.forEachIndexed { i, row -> row(); if (i < rows.lastIndex) RowDivider() }
        }

        if (g != null && g.path.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("My steps")
            Spacer(Modifier.height(8.dp))
            NsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                StepCountRow(Icons.Filled.CheckCircle, Palette.Green, "Done", g.doneCount, Palette.GreenSoft, Palette.Green, onPath)
                RowDivider()
                StepCountRow(Icons.Outlined.AccessTime, Palette.NavyMuted, "Still to do", g.path.size - g.doneCount, Palette.Lavender, Palette.Navy, onPath)
            }
        }

        Spacer(Modifier.height(20.dp))
        NsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            SettingRow(Icons.Outlined.Language, "Language", me.language) { pickLanguage = true }
            RowDivider()
            SettingRow(Icons.Outlined.PersonPin, "My mentor", caseworkers.firstOrNull { it.id == me.mentorId }?.fullName?.let { org.dsiders.nextstep.ui.components.shortName(it) } ?: "Choose") { pickMentorDialog = true }
            RowDivider()
            SettingRow(Icons.Outlined.Visibility, "Who can see my details", "") { showPrivacy = true }
        }
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Download my details", { exporter.launch("my-details-${me.username}.json") }, icon = Icons.Filled.Download)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(Modifier.clickable { container.session.signOut() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Palette.TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sign out", color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (editProfile) EditStudentDialog(me, onDismiss = { editProfile = false }) { updated ->
        scope.launch { container.repo.updateUser(updated); container.engine.invalidate(me.id) }
        editProfile = false
    }
    editFact?.let { f ->
        EditTextDialog(f.label, f.value, onDismiss = { editFact = null }, onDelete = {
            scope.launch { container.repo.deleteFact(f) }; editFact = null
        }) { v -> scope.launch { container.repo.saveFact(f.copy(value = v, source = Source.STUDENT)) }; editFact = null }
    }
    editStatus?.let { item ->
        ChoiceDialog(
            title = item.requirement.name,
            options = listOf("I have it" to ReqStatus.HAVE, "I've started" to ReqStatus.PARTIAL, "Not yet" to ReqStatus.MISSING),
            selected = item.status,
            onDismiss = { editStatus = null },
        ) { status ->
            scope.launch {
                val evidence = when (status) {
                    ReqStatus.HAVE -> "You told us you have this."
                    ReqStatus.PARTIAL -> "You told us you've started this."
                    else -> "You told us you don't have this yet."
                }
                container.repo.setStatus(me.id, item.requirement.id, status, 0.85f, evidence, Source.STUDENT)
                container.engine.invalidate(me.id)
            }
            editStatus = null
        }
    }
    if (pickLanguage) ChoiceDialog("Language", LANGUAGES.map { it to it }, me.language, { pickLanguage = false }) { lang ->
        scope.launch { container.repo.updateUser(me.copy(language = lang)); container.engine.invalidate(me.id) }
        pickLanguage = false
    }
    if (pickMentorDialog) ChoiceDialog(
        "My mentor",
        caseworkers.map { "${it.fullName}${if (it.available) "" else " (away)"}" to it.id },
        me.mentorId, { pickMentorDialog = false },
    ) { id -> scope.launch { container.repo.updateUser(me.copy(mentorId = id)) }; pickMentorDialog = false }
    if (showPrivacy) AlertDialog(
        onDismissRequest = { showPrivacy = false },
        confirmButton = { TextButton({ showPrivacy = false }) { Text("OK") } },
        title = { Text("Who can see my details") },
        text = {
            Text(
                "• You.\n• Barabari caseworkers who sign in on this phone.\n\n" +
                    "Your details stay on this phone. When the app reads your goal or writes a question for you, " +
                    "the words you typed (not your password) are sent to OpenAI to be processed.",
            )
        },
    )
}

@Composable
private fun StepCountRow(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, label: String, count: Int, badgeBg: androidx.compose.ui.graphics.Color, badgeFg: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        CountBadge(count, badgeBg, badgeFg)
    }
}

@Composable
fun SettingRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Palette.Navy, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EditStudentDialog(user: UserEntity, onDismiss: () -> Unit, onSave: (UserEntity) -> Unit) {
    var name by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var college by remember { mutableStateOf(user.college) }
    var course by remember { mutableStateOf(user.course) }
    var year by remember { mutableStateOf(user.year) }
    var district by remember { mutableStateOf(user.district) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldInput(name, { name = it }, "Full name")
                FieldInput(phone, { phone = it }, "Phone (for WhatsApp)")
                FieldInput(college, { college = it }, "College")
                FieldInput(course, { course = it }, "Course, e.g. B.Sc Computer Science")
                FieldInput(year, { year = it }, "Year, e.g. Final year")
                FieldInput(district, { district = it }, "District")
            }
        },
        confirmButton = {
            TextButton({
                onSave(user.copy(fullName = name.trim().ifBlank { user.fullName }, phone = phone.trim(), college = college.trim(),
                    course = course.trim(), year = year.trim(), district = district.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun EditTextDialog(title: String, initial: String, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, label: String = title, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { FieldInput(value, { value = it }, label, singleLine = false) },
        confirmButton = { TextButton({ onSave(value.trim()) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = {
            Row {
                onDelete?.let { TextButton(it) { Text("Remove", color = Palette.AmberText) } }
                TextButton(onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun <T> ChoiceDialog(title: String, options: List<Pair<String, T>>, selected: T?, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(value == selected, { onPick(value) }, colors = RadioButtonDefaults.colors(selectedColor = Palette.Navy))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
    )
}

// ---------------------------------------------------------------------------
// Opening detail (student)
// ---------------------------------------------------------------------------

@Composable
fun OpeningDetailScreen(container: AppContainer, me: UserEntity, openingId: Long, onMadeGoal: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val goal by remember { container.repo.observeGoal(me.id) }.collectAsState(initial = null)
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())
    val opening by produceState<org.dsiders.nextstep.data.OpeningEntity?>(null, openingId) { value = container.repo.opening(openingId) }
    val role by produceState<org.dsiders.nextstep.data.RoleEntity?>(null, opening) { value = opening?.let { container.repo.role(it.roleId) } }
    var readiness by remember { mutableStateOf<GapResult?>(null) }
    var sent by remember { mutableStateOf(false) }
    LaunchedEffect(opening) { opening?.let { readiness = container.engine.analyseForRole(me.id, it.roleId) } }

    val o = opening
    if (o == null) { LoadingBlock("Loading…"); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack)
        SectionLabel("Opening", color = Palette.Orange, modifier = Modifier.padding(horizontal = 4.dp))
        Text(o.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Text(o.organisation, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (o.location.isNotBlank()) Pill(o.location, leading = Icons.Outlined.LocationOn)
            if (o.deadline.isNotBlank()) Pill("Apply by ${o.deadline}", background = Palette.AmberSoft, content = Palette.AmberText)
        }
        if (o.description.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            NsCard { Text(o.description, style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Are you ready for this?")
        Spacer(Modifier.height(8.dp))
        val r = readiness
        if (r == null) LoadingBlock("Checking…")
        else NsCard {
            Text(
                if (r.missing.isEmpty()) "You have everything this role asks for."
                else "You have ${r.have.size} of ${r.have.size + r.missing.size} things this role asks for.",
                style = MaterialTheme.typography.titleMedium, color = Palette.Navy,
            )
            Spacer(Modifier.height(8.dp))
            FitPills(r)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    r.role?.reviewed == false -> "This path hasn't been checked by Barabari yet, so treat it as a rough guide."
                    r.unknownCount > 0 -> "We're less sure because we haven't asked you about ${r.unknownCount} of these yet."
                    else -> "Based on what you've told us."
                },
                style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            r.have.forEach { Text("✓  ${it.requirement.name}", color = Palette.Green, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
            r.missing.forEach { Text("•  ${it.requirement.name}", color = Palette.AmberText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        if (o.howToApply.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("How to apply")
            Spacer(Modifier.height(6.dp))
            NsCard { Text(o.howToApply, style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.height(20.dp))
        if (goal?.targetRoleId != o.roleId && role != null) {
            PrimaryButton("Make ${role!!.title.lowercase()} my goal", {
                scope.launch {
                    container.repo.setTargetRole(me.id, o.roleId)
                    container.engine.invalidate(me.id)
                    onMadeGoal()
                }
            })
            Spacer(Modifier.height(10.dp))
        }
        SecondaryButton(if (sent) "Sent to your mentor" else "Ask my mentor about this", {
            scope.launch {
                val mentor = pickMentor(me, caseworkers, role?.title)
                container.repo.askForHelp(me.id, mentor?.id, "Asked about: ${o.title}", "I'm interested in ${o.title} at ${o.organisation}.")
                sent = true
            }
        }, enabled = !sent)
        Spacer(Modifier.height(16.dp))
    }
}
