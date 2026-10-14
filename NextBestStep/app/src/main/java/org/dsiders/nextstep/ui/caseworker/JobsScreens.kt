package org.dsiders.nextstep.ui.caseworker

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.OpeningEntity
import org.dsiders.nextstep.data.Priority
import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.RequirementKind
import org.dsiders.nextstep.data.RoleEntity
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.ui.components.BackRow
import org.dsiders.nextstep.ui.components.ChoiceChip
import org.dsiders.nextstep.ui.components.EmptyState
import org.dsiders.nextstep.ui.components.FieldInput
import org.dsiders.nextstep.ui.components.LoadingBlock
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.Pill
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.SecondaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.student.ChoiceDialog
import org.dsiders.nextstep.ui.theme.Palette

// ---------------------------------------------------------------------------
// Jobs tab: roles (what a goal needs) and openings (real vacancies)
// ---------------------------------------------------------------------------

@Composable
fun JobsScreen(container: AppContainer, onRole: (Long) -> Unit, onOpening: (Long) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())
    val openings by remember { container.repo.observeOpenings() }.collectAsState(initial = emptyList())
    val reqCounts = remember { mutableStateOf<Map<Long, Pair<Int, Int>>>(emptyMap()) }
    LaunchedEffect(roles) {
        reqCounts.value = roles.associate { r ->
            val reqs = container.repo.requirements(r.id)
            r.id to (reqs.count { it.priority == Priority.CORE } to reqs.count { it.priority == Priority.LATER })
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Text("Jobs", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (tab == 0) "Roles students can aim for, and what each one needs. The app checks every student against these."
                else "Real openings. Students see them on their home screen with what they still need.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted,
            )
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val toReview = roles.count { !it.reviewed }
            ChoiceChip(if (toReview > 0) "Roles (${roles.size}) · $toReview to review" else "Roles (${roles.size})", tab == 0, { tab = 0 })
            ChoiceChip("Openings (${openings.size})", tab == 1, { tab = 1 })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (tab == 0) {
                if (roles.isEmpty()) EmptyState("No roles yet", "Add the first role students can aim for.")
                roles.sortedBy { it.reviewed }.forEach { r ->
                    val (core, later) = reqCounts.value[r.id] ?: (0 to 0)
                    NsCard(onClick = { onRole(r.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleMedium, color = Palette.Navy)
                                Text(r.field, style = MaterialTheme.typography.bodySmall, color = Palette.Text)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!r.reviewed) Pill("Needs review", background = Palette.AmberSoft, content = Palette.AmberText)
                                    Pill("$core steps")
                                    if (later > 0) Pill("$later for later", background = Palette.GreySoft, content = Palette.TextMuted)
                                    val n = openings.count { it.roleId == r.id && it.active }
                                    if (n > 0) Pill("$n open", background = Palette.GreenSoft, content = Palette.Green)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
                        }
                    }
                }
            } else {
                if (openings.isEmpty()) EmptyState("No openings yet", "Add a job, internship or apprenticeship and link it to a role.")
                openings.forEach { o ->
                    NsCard(onClick = { onOpening(o.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.title, style = MaterialTheme.typography.titleMedium, color = Palette.Navy)
                                Text(o.organisation, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Pill(roles.firstOrNull { it.id == o.roleId }?.title ?: "No role")
                                    if (o.active) Pill("Open", background = Palette.GreenSoft, content = Palette.Green)
                                    else Pill("Closed", background = Palette.GreySoft, content = Palette.TextMuted)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Box(Modifier.padding(16.dp)) {
            PrimaryButton(if (tab == 0) "Add a role" else "Add an opening", { if (tab == 0) onRole(0) else onOpening(0) },
                icon = Icons.Filled.Add, enabled = tab == 0 || roles.isNotEmpty())
        }
    }
}

// ---------------------------------------------------------------------------
// Role editor
// ---------------------------------------------------------------------------

/** A requirement being edited. [key] is the real id, or negative for one not saved yet. */
private data class ReqDraft(
    val key: Long,
    val name: String = "",
    val actionTitle: String = "",
    val kind: RequirementKind = RequirementKind.SKILL,
    val priority: Priority = Priority.CORE,
    val why: String = "",
    val time: String = "",
    val need: String = "",
    val where: String = "",
    val how: String = "",
    val hint: String = "",
    val dependsOn: Set<Long> = emptySet(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleEditorScreen(container: AppContainer, me: UserEntity, roleId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(roleId == 0L) }
    var original by remember { mutableStateOf<RoleEntity?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var field by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var keywords by rememberSaveable { mutableStateOf("") }
    var reviewed by rememberSaveable { mutableStateOf(true) }
    val drafts = remember { mutableStateListOf<ReqDraft>() }
    var nextTempKey by remember { mutableStateOf(-1L) }
    var editing by remember { mutableStateOf<ReqDraft?>(null) }
    var drafting by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())

    LaunchedEffect(roleId) {
        if (roleId != 0L) {
            val role = container.repo.role(roleId)
            original = role
            if (role != null) {
                title = role.title; field = role.field; description = role.description; keywords = role.keywords
                reviewed = role.reviewed
                drafts.clear()
                container.repo.requirements(roleId).forEach { r ->
                    drafts += ReqDraft(r.id, r.name, r.actionTitle, r.kind, r.priority, r.whyNeeded, r.timeEstimate, r.whatYouNeed,
                        r.whereToGetHelp, r.howTo, r.questionHint, r.dependsOnIds().toSet())
                }
            }
            loaded = true
        }
    }
    if (!loaded) { LoadingBlock("Loading role…"); return }

    fun newKey(): Long = nextTempKey.also { nextTempKey -= 1 }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
            BackRow(onDone, title = if (roleId == 0L) "New role" else "Edit role") {
                if (roleId != 0L) IconButton({ confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete role", tint = Palette.AmberText) }
            }
            if (original?.reviewed == false) {
                NsCard(color = Palette.AmberSoft) {
                    Text("Drafted by AI for a student's goal", style = MaterialTheme.typography.titleSmall, color = Palette.AmberText)
                    Text(
                        "No one at Barabari has checked this yet, so students aiming for it are always offered a mentor. " +
                            "Check each step, add your centre's places and times, then switch on Reviewed.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Text,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldInput(title, { title = it }, "Role title, e.g. Data analyst")
                FieldInput(field, { field = it }, "Kind of work, e.g. Working with data and numbers",
                    supportingText = "Students pick this in the first question. Reuse an existing one to group roles.")
                val fields = roles.map { it.field }.filter { it.isNotBlank() && it != field }.distinct()
                if (fields.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    fields.forEach { f -> Pill(f, onClick = { field = f }) }
                }
                FieldInput(description, { description = it }, "One-line description", singleLine = false)
                FieldInput(keywords, { keywords = it }, "Words students might use", singleLine = false,
                    supportingText = "Comma separated. Used to recognise this goal when there's no internet.")
            }

            Spacer(Modifier.height(14.dp))
            NsCard(color = Palette.Chip) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Palette.Navy, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Draft the steps with AI", style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
                }
                Text(
                    "Suggests what this role needs. Nothing is saved until you check it and press Save. Add your centre's own places and times.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.NavyMuted,
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(if (drafting) "Drafting…" else "Suggest steps", {
                    drafting = true; aiError = null
                    scope.launch {
                        try {
                            val d = container.engine.draftRole(title, description)
                            if (field.isBlank()) field = d.field
                            if (description.isBlank()) description = d.description
                            if (keywords.isBlank()) keywords = d.keywords
                            val existing = drafts.map { it.name.lowercase() }.toSet()
                            val keyByName = drafts.associate { it.name.lowercase() to it.key }.toMutableMap()
                            d.requirements.filter { it.name.lowercase() !in existing }.forEach { r ->
                                val key = newKey()
                                drafts += ReqDraft(
                                    key, r.name, "", r.kind, r.priority, r.why, r.time, r.need.joinToString("\n"), "", r.how, "",
                                    r.dependsOnNames.mapNotNull { keyByName[it.lowercase()] }.toSet(),
                                )
                                keyByName[r.name.lowercase()] = key
                            }
                        } catch (e: Exception) {
                            aiError = e.message ?: "Couldn't reach OpenAI."
                        }
                        drafting = false
                    }
                }, enabled = title.isNotBlank() && !drafting && container.ai.isConfigured)
                if (!container.ai.isConfigured) Text("Needs an OpenAI key in local.properties.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                aiError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.AmberText) }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Steps, in the order a student should do them")
            Spacer(Modifier.height(8.dp))
            if (drafts.isEmpty()) Text("No steps yet.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            drafts.forEachIndexed { index, d ->
                Box(Modifier.padding(vertical = 4.dp)) {
                    NsCard(onClick = { editing = d }, padding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name.ifBlank { "Untitled step" }, style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    Pill(d.kind.name.lowercase().replaceFirstChar { it.uppercase() })
                                    if (d.priority == Priority.LATER) Pill("Later", background = Palette.GreySoft, content = Palette.TextMuted)
                                    if (d.time.isNotBlank()) Pill(d.time, background = Palette.GreySoft, content = Palette.Text)
                                }
                                if (d.dependsOn.isNotEmpty()) Text(
                                    "After: " + d.dependsOn.mapNotNull { k -> drafts.firstOrNull { it.key == k }?.name }.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted,
                                )
                            }
                            Column {
                                IconButton({ if (index > 0) { drafts.removeAt(index); drafts.add(index - 1, d) } }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = Palette.Navy)
                                }
                                IconButton({ if (index < drafts.lastIndex) { drafts.removeAt(index); drafts.add(index + 1, d) } }, enabled = index < drafts.lastIndex, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.KeyboardArrowDown, "Move down", tint = Palette.Navy)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            TextLink("+ Add a step", { editing = ReqDraft(newKey()) })
            Spacer(Modifier.height(16.dp))
            NsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reviewed by Barabari", style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
                        Text("Only reviewed roles are offered in the first question and give results without a mentor check.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    }
                    Switch(reviewed, { reviewed = it }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Green))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(16.dp)) {
            PrimaryButton("Save role", {
                saving = true
                scope.launch {
                    val base = original ?: RoleEntity(title = "", field = "", createdBy = me.id)
                    val id = container.repo.saveRole(base.copy(title = title.trim(), field = field.trim(), description = description.trim(),
                        keywords = keywords.trim(), reviewed = reviewed))
                    // First pass: save every step and learn the real ids of new ones.
                    val idByKey = mutableMapOf<Long, Long>()
                    drafts.forEachIndexed { i, d ->
                        val entity = RequirementEntity(
                            id = if (d.key > 0) d.key else 0, roleId = id, name = d.name.trim(), actionTitle = d.actionTitle.trim(),
                            kind = d.kind, priority = d.priority, orderIndex = i, whyNeeded = d.why.trim(), timeEstimate = d.time.trim(),
                            whatYouNeed = d.need.trim(), whereToGetHelp = d.where.trim(), howTo = d.how.trim(), questionHint = d.hint.trim(),
                        )
                        idByKey[d.key] = container.repo.saveRequirement(entity)
                    }
                    // Second pass: wire up prerequisites now that every step has an id.
                    drafts.forEachIndexed { i, d ->
                        if (d.dependsOn.isNotEmpty()) {
                            val reqId = idByKey.getValue(d.key)
                            container.repo.db.requirements().byId(reqId)?.let { saved ->
                                container.repo.saveRequirement(saved.copy(dependsOn = d.dependsOn.mapNotNull { idByKey[it] }.joinToString(","), orderIndex = i))
                            }
                        }
                    }
                    // Steps removed in the editor.
                    container.repo.requirements(id).filter { it.id !in idByKey.values }.forEach { container.repo.deleteRequirement(it) }
                    saving = false
                    onDone()
                }
            }, enabled = title.isNotBlank() && field.isNotBlank() && drafts.all { it.name.isNotBlank() }, loading = saving)
        }
    }

    editing?.let { d ->
        RequirementDialog(
            draft = d, others = drafts.filter { it.key != d.key },
            onDismiss = { editing = null },
            onDelete = if (drafts.any { it.key == d.key }) ({
                drafts.removeAll { it.key == d.key }
                drafts.replaceAll { it.copy(dependsOn = it.dependsOn - d.key) }
                editing = null
            }) else null,
            onSave = { updated ->
                val i = drafts.indexOfFirst { it.key == updated.key }
                if (i >= 0) drafts[i] = updated else drafts += updated
                editing = null
            },
        )
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this role?") },
        text = { Text("Its steps and openings are deleted too. Students aiming for it will be asked to pick a direction again.") },
        confirmButton = {
            TextButton({
                scope.launch { original?.let { container.repo.deleteRole(it) }; confirmDelete = false; onDone() }
            }) { Text("Delete", color = Palette.AmberText) }
        },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequirementDialog(draft: ReqDraft, others: List<ReqDraft>, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (ReqDraft) -> Unit) {
    var d by remember(draft.key) { mutableStateOf(draft) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Palette.Page) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackRow(onDismiss, title = if (draft.name.isBlank()) "New step" else "Edit step") {
                        onDelete?.let { IconButton(it) { Icon(Icons.Filled.Delete, "Delete step", tint = Palette.AmberText) } }
                    }
                    FieldInput(d.name, { d = d.copy(name = it) }, "Name, e.g. A resume")
                    FieldInput(d.actionTitle, { d = d.copy(actionTitle = it) }, "As an action (optional)", placeholder = "e.g. Write your first resume")
                    SectionLabel("Kind")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RequirementKind.entries.forEach { k ->
                            ChoiceChip(k.name.lowercase().replaceFirstChar { it.uppercase() }, d.kind == k, { d = d.copy(kind = k) })
                        }
                    }
                    SectionLabel("When")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("Needed for a first job", d.priority == Priority.CORE, { d = d.copy(priority = Priority.CORE) })
                        ChoiceChip("Later", d.priority == Priority.LATER, { d = d.copy(priority = Priority.LATER) })
                    }
                    FieldInput(d.why, { d = d.copy(why = it) }, "Why the role needs it", singleLine = false)
                    FieldInput(d.time, { d = d.copy(time = it) }, "How long, e.g. About 2 hours")
                    FieldInput(d.need, { d = d.copy(need = it) }, "What to bring or have ready (one per line)", singleLine = false, minLines = 2)
                    FieldInput(d.where, { d = d.copy(where = it) }, "Where to get help", singleLine = false,
                        placeholder = "e.g. Barabari resume clinic, Kukatpally centre, Saturdays 11 AM")
                    FieldInput(d.how, { d = d.copy(how = it) }, "How to do it (your centre's notes)", singleLine = false, minLines = 2)
                    FieldInput(d.hint, { d = d.copy(hint = it) }, "Note for how to ask about it (optional)", singleLine = false)
                    if (others.isNotEmpty()) {
                        SectionLabel("Must be done first")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            others.forEach { o ->
                                val sel = o.key in d.dependsOn
                                ChoiceChip(o.name.ifBlank { "Untitled" }, sel, { d = d.copy(dependsOn = if (sel) d.dependsOn - o.key else d.dependsOn + o.key) })
                            }
                        }
                    }
                }
                Box(Modifier.padding(16.dp)) { PrimaryButton("Done", { onSave(d) }, enabled = d.name.isNotBlank()) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Opening editor
// ---------------------------------------------------------------------------

@Composable
fun OpeningEditorScreen(container: AppContainer, me: UserEntity, openingId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())
    var loaded by remember { mutableStateOf(openingId == 0L) }
    var original by remember { mutableStateOf<OpeningEntity?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var organisation by rememberSaveable { mutableStateOf("") }
    var roleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var location by rememberSaveable { mutableStateOf("") }
    var deadline by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var howToApply by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    var pickRole by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(openingId) {
        if (openingId != 0L) {
            container.repo.opening(openingId)?.let { o ->
                original = o
                title = o.title; organisation = o.organisation; roleId = o.roleId; location = o.location
                deadline = o.deadline; description = o.description; howToApply = o.howToApply; active = o.active
            }
            loaded = true
        }
    }
    if (!loaded) { LoadingBlock("Loading opening…"); return }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BackRow(onDone, title = if (openingId == 0L) "New opening" else "Edit opening") {
                if (openingId != 0L) IconButton({ confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete opening", tint = Palette.AmberText) }
            }
            FieldInput(title, { title = it }, "Title, e.g. Data trainee")
            FieldInput(organisation, { organisation = it }, "Organisation")
            NsCard(onClick = { pickRole = true }) {
                SectionLabel("Linked role")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(roles.firstOrNull { it.id == roleId }?.title ?: "Choose the role this opening is for",
                        style = MaterialTheme.typography.bodyLarge, color = Palette.Navy, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
                }
                Text("Students see what they still need for this role.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
            FieldInput(location, { location = it }, "Location, e.g. Hyderabad or Remote")
            FieldInput(deadline, { deadline = it }, "Apply by, e.g. 30 Nov")
            FieldInput(description, { description = it }, "About the opening", singleLine = false, minLines = 2)
            FieldInput(howToApply, { howToApply = it }, "How to apply", singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Open for applications", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(active, { active = it }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Green))
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(16.dp)) {
            PrimaryButton("Save opening", {
                scope.launch {
                    val base = original ?: OpeningEntity(roleId = roleId!!, title = "", organisation = "", createdBy = me.id)
                    container.repo.saveOpening(
                        base.copy(roleId = roleId!!, title = title.trim(), organisation = organisation.trim(), location = location.trim(),
                            deadline = deadline.trim(), description = description.trim(), howToApply = howToApply.trim(), active = active)
                    )
                    onDone()
                }
            }, enabled = title.isNotBlank() && organisation.isNotBlank() && roleId != null)
        }
    }
    if (pickRole) ChoiceDialog("Linked role", roles.map { it.title to it.id }, roleId, { pickRole = false }) { roleId = it; pickRole = false }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this opening?") },
        confirmButton = { TextButton({ scope.launch { original?.let { container.repo.deleteOpening(it) }; confirmDelete = false; onDone() } }) { Text("Delete", color = Palette.AmberText) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep") } },
    )
}
