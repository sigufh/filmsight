package com.filmtracker.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Material Design 3 spacing tokens for consistent spacing throughout the app.
 */
object Spacing {
    val xs = 4.dp   // Extra small
    val sm = 8.dp   // Small
    val md = 16.dp  // Medium/default
    val lg = 24.dp  // Large
    val xl = 32.dp  // Extra large
    val xxl = 48.dp // Extra extra large
}

/**
 * Standard icon sizes following Material Design 3 guidelines.
 */
object IconSize {
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
}

/**
 * Standard component sizes for consistent UI elements.
 */
object ComponentSize {
    val buttonHeight = 48.dp
    val toolbarHeight = 64.dp
    val bottomNavHeight = 80.dp
    val panelMinHeight = 200.dp
    val panelMaxHeight = 400.dp
    val cardMinHeight = 100.dp
}

/**
 * Corner radius values for rounded shapes following Material Design 3.
 */
object CornerRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 28.dp
    const val full = 50 // Percentage for pill shapes
}
