package org.dsiders.nextstep.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.BuildConfig
import org.dsiders.nextstep.data.SeedData
import org.dsiders.nextstep.data.UserRole
import org.dsiders.nextstep.ui.components.FieldInput
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.TextLink
import org.dsiders.nextstep.ui.theme.Palette

@Composable
fun RoleSwitch(role: UserRole, onChange: (UserRole) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.Chip).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(UserRole.STUDENT to "Student", UserRole.CASEWORKER to "Caseworker").forEach { (r, label) ->
            val selected = r == role
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Palette.Navy else Color.Transparent)
                    .clickable { onChange(r) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (selected) Color.White else Palette.Navy, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, supporting: String? = null) {
    var visible by remember { mutableStateOf(false) }
    FieldInput(
        value, onChange, label,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Hide password" else "Show password", tint = Palette.TextMuted)
            }
        },
        supportingText = supporting,
    )
}

/** Frame 0: Log in. */
@Composable
fun LoginScreen(container: AppContainer, onCreateAccount: () -> Unit) {
    val scope = rememberCoroutineScope()
    var role by rememberSaveable { mutableStateOf(UserRole.STUDENT) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome back", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("Log in to pick up where you left off.", style = MaterialTheme.typography.bodyLarge, color = Palette.TextMuted)
        Spacer(Modifier.height(24.dp))
        RoleSwitch(role, { role = it; error = null })
        Spacer(Modifier.height(20.dp))
        FieldInput(username, { username = it; error = null }, "Username or ID",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        Spacer(Modifier.height(12.dp))
        PasswordField(password, { password = it; error = null }, "Password")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextLink("Forgot password?", { showForgot = true }, underline = false)
        }
        error?.let {
            Text(it, color = Palette.AmberText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "Log in",
            onClick = {
                busy = true
                scope.launch {
                    container.repo.login(username, password, role)
                        .onSuccess { container.session.signIn(it.id) }
                        .onFailure { error = it.message }
                    busy = false
                }
            },
            enabled = username.isNotBlank() && password.isNotBlank(), loading = busy,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("New here?", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            TextLink("Create an account", onCreateAccount)
        }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(24.dp))
            NsCard(color = Palette.Chip) {
                Text("Sample accounts (debug builds only)", style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Student: priya  ·  Caseworker: ravi\nPassword for both: ${SeedData.DEMO_PASSWORD}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.NavyMuted,
                )
            }
        }
    }

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            confirmButton = { TextButton({ showForgot = false }) { Text("OK") } },
            title = { Text("Forgot your password?") },
            text = {
                Text(
                    if (role == UserRole.STUDENT) "Ask your Barabari mentor at your centre. They can set a new password for you from your student page."
                    else "Ask your centre lead to reset it, or create a new caseworker account.",
                )
            },
        )
    }
}

/** Frame 0b: Create account. */
@Composable
fun RegisterScreen(container: AppContainer, onBackToLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    var role by rememberSaveable { mutableStateOf(UserRole.STUDENT) }
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    val problem = when {
        name.isBlank() || username.isBlank() -> null
        username.contains(' ') -> "Usernames can't have spaces."
        password.isNotEmpty() && password.length < 8 -> "Password needs at least 8 characters."
        confirm.isNotEmpty() && confirm != password -> "The two passwords don't match."
        else -> null
    }
    val canSubmit = name.isNotBlank() && username.isNotBlank() && password.length >= 8 && confirm == password && agreed && problem == null

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Create your account", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("Takes about a minute. You can change any detail later.", style = MaterialTheme.typography.bodyLarge, color = Palette.TextMuted)
        Spacer(Modifier.height(20.dp))
        Text("I am a", style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
        Spacer(Modifier.height(8.dp))
        RoleSwitch(role, { role = it })
        Spacer(Modifier.height(16.dp))
        FieldInput(name, { name = it }, "Full name")
        Spacer(Modifier.height(12.dp))
        FieldInput(username, { username = it.trim(); error = null }, "Username")
        Spacer(Modifier.height(12.dp))
        PasswordField(password, { password = it }, "Password", supporting = "At least 8 characters")
        Spacer(Modifier.height(4.dp))
        PasswordField(confirm, { confirm = it }, "Confirm password")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(agreed, { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = Palette.Navy))
            Text("I agree to the ", style = MaterialTheme.typography.bodyMedium)
            Text(
                "terms and privacy policy", style = MaterialTheme.typography.bodyMedium, color = Palette.Navy,
                modifier = Modifier.clickable { showTerms = true },
            )
        }
        (error ?: problem)?.let {
            Text(it, color = Palette.AmberText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "Create account",
            onClick = {
                busy = true
                scope.launch {
                    container.repo.register(role, name, username, password)
                        .onSuccess { container.session.signIn(it.id) }
                        .onFailure { error = it.message }
                    busy = false
                }
            },
            enabled = canSubmit, loading = busy,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            TextLink("Log in", onBackToLogin)
        }
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            confirmButton = { TextButton({ showTerms = false }) { Text("OK") } },
            title = { Text("Terms and privacy") },
            text = {
                Text(
                    "Your details are stored on this phone only. Barabari caseworkers who use this phone can see your goal, " +
                        "your answers and your steps so they can help you. When the app words a question or reads your goal, " +
                        "the text you wrote is sent to OpenAI to process. You can see, change or download everything from your profile.",
                    textAlign = TextAlign.Start,
                )
            },
        )
    }
}
