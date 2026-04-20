package com.ordemservico.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ordemservico.app.ui.navigation.AppNavigation
import com.ordemservico.app.ui.theme.OrdemServicoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrdemServicoTheme {
                AppNavigation()
            }
        }
    }
}
