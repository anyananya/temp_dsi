package org.dsiders.nextstep.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dsiders.nextstep.ui.theme.Jakarta
import org.dsiders.nextstep.ui.theme.Palette

val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(12.dp)

@Composable
fun NsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Palette.Card,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(CardShape).clickable(onClick = onClick) else Modifier),
        shape = CardShape, color = color, shadowElevation = 1.dp,
    ) { Column(Modifier.padding(padding), content = content) }
}

/** A card with a coloured bar down its left edge, as used for status rows in the Figma. */
@Composable
fun AccentCard(
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Palette.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(CardShape).clickable(onClick = onClick) else Modifier),
        shape = CardShape, color = color, shadowElevation = 1.dp,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).weight(1f), content = content)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp), shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Orange, contentColor = Color.White,
            disabledContainerColor = Palette.Orange.copy(alpha = 0.45f), disabledContentColor = Color.White,
        ),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            icon?.let { Icon(it, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp), shape = ButtonShape,
        border = BorderStroke(1.dp, Palette.Navy),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Card, contentColor = Palette.Navy),
    ) {
        icon?.let { Icon(it, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Palette.Navy, underline: Boolean = true) {
    Text(
        text, modifier = modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        color = color, style = MaterialTheme.typography.bodyMedium.copy(
            textDecoration = if (underline) TextDecoration.Underline else null, fontWeight = FontWeight.Medium,
        ),
    )
}

/** Outlined answer button from frames 3 and 4. */
@Composable
fun AnswerRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, tinted: Boolean = false, enabled: Boolean = true, tag: String? = null) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(ButtonShape).clickable(enabled = enabled, onClick = onClick),
        shape = ButtonShape, color = if (tinted) Palette.Lavender.copy(alpha = 0.7f) else Palette.Card,
        border = if (tinted) null else BorderStroke(if (tag != null) 2.dp else 1.2.dp, Palette.Navy),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, tint = Palette.Navy, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                tag?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Palette.OrangeDeep) }
                Text(text, style = MaterialTheme.typography.bodyLarge, color = Palette.Navy)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.Navy)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Palette.TextMuted) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, background: Color = Palette.Chip, content: Color = Palette.Navy, onClick: (() -> Unit)? = null, leading: ImageVector? = null) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { Icon(it, null, tint = content, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, color = content, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A selectable white chip with a soft shadow, like the starter chips on frame 1. */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50), color = if (selected) Palette.Navy else Palette.Card, shadowElevation = 1.dp,
    ) {
        Text(
            text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) Color.White else Palette.Navy, style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun CountBadge(count: Int, background: Color, content: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text("$count", color = content, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun GroupHeader(title: String, dot: Color, count: Int?, badgeBg: Color, badgeFg: Color, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Dot(dot)
        Spacer(Modifier.width(10.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Palette.Navy)
        count?.let { CountBadge(it, badgeBg, badgeFg) }
        trailing()
    }
}

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp, background: Color = Palette.Peach, content: Color = Palette.Navy) {
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(initials, color = content, fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.36f).sp)
    }
}

/** The thin segmented progress bar above the question cards. */
@Composable
fun SegmentProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val color = when {
                i < current - 1 -> Palette.Navy
                i == current - 1 -> Palette.Navy.copy(alpha = 0.55f)
                else -> Palette.Divider
            }
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Palette.BannerAmber)
            .border(BorderStroke(0.dp, Color.Transparent)),
    ) {
        Box(Modifier.width(3.dp).heightIn(min = 48.dp).background(Palette.BannerAmberEdge))
        Row(Modifier.padding(12.dp).weight(1f), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Lightbulb, null, tint = Palette.BannerAmberEdge, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Palette.Navy)
            onClose?.let {
                IconButton(onClick = it, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Dismiss", tint = Palette.TextMuted, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun BackRow(onBack: () -> Unit, modifier: Modifier = Modifier, title: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.Navy) }
        title?.let { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = Palette.Navy) }
            ?: Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text, modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, color = Palette.Navy,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
fun FieldInput(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    placeholder: String? = null, singleLine: Boolean = true, minLines: Int = 1,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null, supportingText: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = placeholder?.let { { Text(it, color = Palette.Grey) } },
        singleLine = singleLine, minLines = minLines, visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions, trailingIcon = trailingIcon, shape = ButtonShape,
        supportingText = supportingText?.let { { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Palette.Card, unfocusedContainerColor = Palette.Card,
            focusedBorderColor = Palette.Navy, unfocusedBorderColor = Palette.Divider,
            focusedLabelColor = Palette.Navy,
        ),
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = TextAlign.Center)
        action?.let { Spacer(Modifier.height(16.dp)); it() }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Palette.Navy, onClick: (() -> Unit)? = null, trailing: ImageVector? = null) {
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
        trailing?.let { Spacer(Modifier.width(8.dp)); Icon(it, null, tint = Palette.Navy, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun RowDivider() = Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Palette.Divider))

@Composable
fun LoadingBlock(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Palette.Navy, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = TextAlign.Center)
    }
}
