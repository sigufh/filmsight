package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AutoAdjustContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { /* TODO: 实现自动调整 */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A84FF)
            )
        ) {
            Text("自动调整", color = Color.White)
        }
    }
}
