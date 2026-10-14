package org.dsiders.nextstep.ui.student

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.OpeningEntity
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.engine.GapResult
import org.dsiders.nextstep.engine.ResumeReader
import org.dsiders.nextstep.ui.components.AccentCard
import org.dsiders.nextstep.ui.components.ChoiceChip
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.Pill
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.components.firstName
import org.dsiders.nextstep.ui.theme.Palette

/** Frame 1: goal entry. Once a goal exists, it also shows where the student is up to. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentHomeScreen(
    container: AppContainer,
    me: UserEntity,
    onStartIntake: () -> Unit,
    onContinueQuestions: () -> Unit,
    onStanding: () -> Unit,
    onNextStep: () -> Unit,
    onOpening: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Wrapped so "still loading" (null) is distinguishable from "no goal yet" (Loaded(null)).
    val goalState by remember { container.repo.observeGoal(me.id).map { Loaded(it) } }.collectAsState(initial = null)
    val goal = goalState?.value
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())
    val openings by remember { container.repo.observeActiveOpenings() }.collectAsState(initial = emptyList())
    val statuses by remember { container.repo.observeStatuses(me.id) }.collectAsState(initial = emptyList())
    val gap by produceState<GapResult?>(null, goal, statuses) {
        value = if (goal?.targetRoleId != null) container.engine.analyse(me.id) else null
    }

    var editing by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var attachment by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            attachment = uri
            attachmentName = ResumeReader.displayName(context, uri)
        }
    }

    if (goalState == null) return
    val hasGoal = goal?.goalText?.isNotBlank() == true
    val showEntry = editing || !hasGoal

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text("Namaste, ${firstName(me.fullName)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        Spacer(Modifier.height(6.dp))

        if (showEntry) {
            Text("What do you want to become?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text("One sentence is enough. I'll figure out the rest.", style = MaterialTheme.typography.bodyLarge, color = Palette.TextMuted)
            Spacer(Modifier.height(20.dp))

            NsCard {
                BasicTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.Text),
                    cursorBrush = SolidColor(Palette.Navy),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(
                            "I want to become a data analyst but I don't know where to start",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic), color = Palette.Grey,
                        )
                        inner()
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (attachmentName == null) {
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AttachFile, null, tint = Palette.Navy, modifier = Modifier.size(16.dp))
                        TextLink("Attach file", { picker.launch(arrayOf("application/pdf", "image/*", "text/plain")) }, underline = false)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(attachmentName!!, leading = Icons.Filled.AttachFile, modifier = Modifier.weight(1f, fill = false))
                        IconButton({ attachment = null; attachmentName = null }) { Icon(Icons.Filled.Close, "Remove file", tint = Palette.TextMuted) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.GreySoft.copy(alpha = 0.7f)).padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Description, null, tint = Palette.NavyMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "If you have a resume or any supporting documents that might be helpful, please attach them here.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Text,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Or start with one of these", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                roles.take(3).forEach { role ->
                    val sentence = "I want to become a ${role.title.lowercase()}"
                    ChoiceChip(role.title, text == sentence, { text = sentence })
                }
                val unsure = "I'm not sure yet what I want to do"
                ChoiceChip("Not sure yet", text == unsure, { text = unsure })
            }
            Spacer(Modifier.height(32.dp))
            PrimaryButton(
                "Help me figure it out",
                onClick = {
                    busy = true
                    scope.launch {
                        container.repo.startGoal(me.id, text)
                        container.pendingResume.remove(me.id)
                        attachment?.let { uri ->
                            runCatching { ResumeReader.read(context, uri) }.getOrNull()?.let { container.pendingResume[me.id] = it }
                        }
                        container.engine.invalidate(me.id)
                        busy = false
                        editing = false
                        onStartIntake()
                    }
                },
                enabled = text.isNotBlank(), loading = busy,
            )
            if (hasGoal) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextLink("Keep my current goal", { editing = false })
                }
            }
        } else {
            val g = goal!!
            val role = roles.firstOrNull { it.id == g.targetRoleId }
            Text(
                if (role != null) "You're working towards" else "Let's find your direction",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            AccentCard(Palette.OrangeDeep, onClick = if (g.intakeDone) onStanding else onContinueQuestions) {
                SectionLabel("What I want to become", color = Palette.OrangeDeep)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(role?.title ?: "Not sure yet", style = MaterialTheme.typography.titleLarge, color = Palette.Navy)
                        Text("\"${g.goalText}\"", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 2)
                        gap?.let { if (it.path.isNotEmpty()) Text("${it.doneCount} of ${it.path.size} steps done", style = MaterialTheme.typography.bodySmall, color = Palette.Text) }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                !g.intakeDone -> PrimaryButton("Pick up where you left off", onContinueQuestions)
                gap?.nextStep != null -> {
                    NsCard(onClick = onNextStep) {
                        SectionLabel("Your next step", color = Palette.Orange)
                        Spacer(Modifier.height(4.dp))
                        Text(gap!!.nextStep!!.stepTitle(), style = MaterialTheme.typography.titleLarge, color = Palette.Navy)
                        if (gap!!.nextStep!!.timeEstimate.isNotBlank()) {
                            Text(gap!!.nextStep!!.timeEstimate, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Show me what to do next", onNextStep)
                }
                else -> PrimaryButton("See where I stand", onStanding)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextLink("I want something different", { text = ""; editing = true })
            }
        }

        val relevant = openings.filter { o -> goal?.targetRoleId == null || o.roleId == goal?.targetRoleId }
            .ifEmpty { openings }
        if (relevant.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionLabel(if (goal?.targetRoleId != null && relevant.any { it.roleId == goal?.targetRoleId }) "Openings for you" else "Openings from Barabari")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                relevant.take(4).forEach { OpeningCard(it, roles.firstOrNull { r -> r.id == it.roleId }?.title, onClick = { onOpening(it.id) }) }
            }
        }
    }
}

@Composable
fun OpeningCard(opening: OpeningEntity, roleTitle: String?, onClick: () -> Unit) {
    NsCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(opening.title, style = MaterialTheme.typography.titleMedium, color = Palette.Navy)
                Text(opening.organisation, style = MaterialTheme.typography.bodySmall, color = Palette.Text)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (opening.location.isNotBlank()) Pill(opening.location, leading = Icons.Outlined.LocationOn)
                    if (opening.deadline.isNotBlank()) Pill("Apply by ${opening.deadline}", background = Palette.AmberSoft, content = Palette.AmberText)
                }
                roleTitle?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("For: $it", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
        }
    }
}

private data class Loaded<T>(val value: T)
