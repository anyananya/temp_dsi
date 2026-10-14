package org.dsiders.nextstep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import org.dsiders.nextstep.ui.AppRoot
import org.dsiders.nextstep.ui.theme.NextStepTheme
import org.dsiders.nextstep.ui.theme.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NextStepApp).container
        setContent {
            NextStepTheme {
                Box(Modifier.fillMaxSize().background(Palette.Page).safeDrawingPadding()) {
                    AppRoot(container)
                }
            }
        }
    }
}
