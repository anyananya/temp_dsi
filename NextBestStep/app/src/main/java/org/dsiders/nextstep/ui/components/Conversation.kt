package org.dsiders.nextstep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.dsiders.nextstep.data.MessageEntity
import org.dsiders.nextstep.ui.theme.Palette

/**
 * A help conversation. [viewerIsStudent] decides which side is "mine" (right, navy).
 * [otherName] labels the other person's messages.
 */
@Composable
fun MessageThread(messages: List<MessageEntity>, viewerIsStudent: Boolean, otherName: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (messages.isEmpty()) {
            Text("No messages yet.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
        }
        messages.forEach { m ->
            val mine = m.fromStudent == viewerIsStudent
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                Column(
                    Modifier.widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(14.dp, 14.dp, if (mine) 4.dp else 14.dp, if (mine) 14.dp else 4.dp))
                        .background(if (mine) Palette.Navy else Palette.Lavender)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (!mine) Text(otherName, style = MaterialTheme.typography.labelMedium, color = Palette.Navy)
                    Text(m.text, style = MaterialTheme.typography.bodyMedium, color = if (mine) Palette.Card else Palette.Text)
                    Text(
                        timeAgo(m.at), style = MaterialTheme.typography.bodySmall,
                        color = if (mine) Palette.Card.copy(alpha = 0.7f) else Palette.TextMuted,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

/** Text box with a send button; clears itself after sending. */
@Composable
fun MessageComposer(placeholder: String, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { FieldInput(text, { text = it }, placeholder, singleLine = false) }
        Spacer(Modifier.padding(start = 4.dp))
        IconButton(
            onClick = { onSend(text.trim()); text = "" },
            enabled = text.isNotBlank(),
        ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (text.isNotBlank()) Palette.Orange else Palette.Grey) }
    }
}
