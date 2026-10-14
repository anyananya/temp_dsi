package org.dsiders.nextstep.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dsiders.nextstep.data.RequirementEntity
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.ui.theme.Palette
import java.util.Calendar

fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    val mins = (now - millis) / 60_000
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m ago"
        mins < 24 * 60 -> "${mins / 60}h ago"
        else -> "${mins / (24 * 60)}d"
    }
}

fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun firstName(name: String) = name.trim().substringBefore(' ')

/** "Priya Komati" -> "Priya K." as on the caseworker list. */
fun shortName(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotBlank() }
    return if (parts.size < 2) name else "${parts.first()} ${parts.last().first()}."
}

/** The next step as a message a student can keep on their phone. */
fun nextStepMessage(student: UserEntity, req: RequirementEntity, goalTitle: String?): String = buildString {
    appendLine("Namaste ${firstName(student.fullName)}, here is your next step${goalTitle?.let { " towards $it" } ?: ""}:")
    appendLine()
    appendLine("*${req.name}*")
    if (req.whyNeeded.isNotBlank()) appendLine(req.whyNeeded)
    if (req.whatYouNeed.isNotBlank()) {
        appendLine()
        appendLine("What you'll need:")
        req.whatYouNeedList().forEach { appendLine("• $it") }
    }
    if (req.timeEstimate.isNotBlank()) appendLine("\nHow long: ${req.timeEstimate}")
    if (req.whereToGetHelp.isNotBlank()) appendLine("Where to get help: ${req.whereToGetHelp}")
    appendLine()
    append("— Barabari Collective")
}

object Share {
    /** Opens WhatsApp to [phone] with [text]; falls back to the share sheet. */
    fun whatsApp(context: Context, phone: String, text: String) {
        val digits = phone.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it }
        val intent = if (digits.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(text)}"))
        } else {
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Send next step")
            )
        }
    }

    fun text(context: Context, text: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share"))
    }

    private var printView: WebView? = null

    /** Prints [message] as a one-page instruction card through Android's print service. */
    fun print(context: Context, title: String, message: String) {
        val html = """
            <html><body style="font-family:sans-serif;padding:24px;color:#2C3468">
            <h2 style="color:#D97B43;margin:0 0 4px">Your next step</h2>
            <pre style="font-family:sans-serif;font-size:15px;white-space:pre-wrap">${message.replace("<", "&lt;").replace("*", "")}</pre>
            </body></html>
        """.trimIndent()
        val webView = WebView(context)
        printView = webView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (manager == null) {
                    Toast.makeText(context, "Printing isn't available on this phone", Toast.LENGTH_SHORT).show()
                } else {
                    manager.print(title, view.createPrintDocumentAdapter(title), PrintAttributes.Builder().build())
                }
                printView = null
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}

/**
 * The handover card from frame 5: a named, available person instead of a
 * confident guess. Shows nothing about availability that isn't true.
 */
@Composable
fun MentorCard(
    mentor: UserEntity?,
    headline: String,
    body: String,
    /** True when a conversation is already open; the button then adds to it. */
    requestSent: Boolean,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccentCard(accent = Palette.Navy, modifier = modifier) {
        SectionLabel(headline)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
        Spacer(Modifier.height(12.dp))
        if (mentor != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(mentor.fullName, size = 40.dp, background = Palette.Lavender)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(shortName(mentor.fullName), style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
                    val langs = mentor.languages.split(',').map { it.trim() }.filter { it.isNotEmpty() && !it.equals("English", true) }
                    Text(
                        listOfNotNull("Mentor, ${mentor.centre.removeSuffix(" centre")}".takeIf { mentor.centre.isNotBlank() } ?: "Mentor",
                            langs.firstOrNull()?.let { "speaks $it" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Dot(if (mentor.available) Palette.Green else Palette.Grey)
                    Text(if (mentor.available) "Online" else "Away", style = MaterialTheme.typography.labelMedium,
                        color = if (mentor.available) Palette.Green else Palette.Grey)
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                if (requestSent) "Send ${firstName(mentor.fullName)} another message" else "Message ${firstName(mentor.fullName)}",
                onMessage, icon = Icons.AutoMirrored.Filled.Chat,
            )
        } else {
            Text("No mentor is online right now. Your message will wait for the next one.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            Spacer(Modifier.height(12.dp))
            SecondaryButton(if (requestSent) "Add to your message" else "Leave a message", onMessage, icon = Icons.AutoMirrored.Filled.Chat)
        }
    }
}

/** Picks the mentor to offer: the student's own if available, else one who speaks their language. */
fun pickMentor(student: UserEntity, caseworkers: List<UserEntity>, topicHint: String? = null): UserEntity? {
    val available = caseworkers.filter { it.available }
    available.firstOrNull { it.id == student.mentorId }?.let { return it }
    val speaks = available.filter { cw -> cw.languages.split(',').any { it.trim().equals(student.language, true) } }
    val byTopic = topicHint?.let { hint -> speaks.firstOrNull { cw -> cw.topics.split(',').map { it.trim().removeSuffix("s") }.filter { it.length > 2 }.any { hint.contains(it, true) } } }
    return byTopic ?: speaks.firstOrNull() ?: available.firstOrNull()
}
