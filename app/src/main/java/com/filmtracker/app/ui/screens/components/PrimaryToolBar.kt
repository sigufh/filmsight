package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.ui.screens.models.PrimaryTool

@Composable
fun PrimaryToolBar(
    selectedTool: PrimaryTool?,
    onToolSelected: (PrimaryTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PrimaryTool.entries.forEach { tool ->
            PrimaryToolButton(
                tool = tool,
                isSelected = selectedTool == tool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
private fun PrimaryToolButton(
    tool: PrimaryTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .padding(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = if (isSelected) Color(0xFF0A84FF) else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0A84FF) else Color.White
        )
    }
}
