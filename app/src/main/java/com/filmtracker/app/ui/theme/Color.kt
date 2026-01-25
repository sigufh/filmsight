package com.filmtracker.app.ui.theme

import androidx.compose.ui.graphics.Color

// 专业摄影应用配色方案（原有深色主题）
val FilmTrackerDark = Color(0xFF1A1A1A)
val FilmTrackerSurface = Color(0xFF2C2C2C)
val FilmTrackerPrimary = Color(0xFFFFB800)  // 胶片金色
val FilmTrackerSecondary = Color(0xFF6B6B6B)
val FilmTrackerAccent = Color(0xFF4A90E2)

val FilmTrackerOnDark = Color(0xFFFFFFFF)
val FilmTrackerOnSurface = Color(0xFFE0E0E0)

// 胶卷仿拍 Ins 风格配色方案（新增）
// 主色调：暖调米白 + 焦糖橘 + 薄荷绿 + 奶灰蓝 + 墨黑
val FilmWarmBeige = Color(0xFFF5F1E8)        // 暖调米白（底色）
val FilmCaramelOrange = Color(0xFFE89B5A)    // 焦糖橘（主视觉）
val FilmMintGreen = Color(0xFF9FD8CB)        // 薄荷绿（辅助）
val FilmMilkyBlue = Color(0xFFB8C5D6)        // 奶灰蓝（功能）
val FilmInkBlack = Color(0xFF1A1A1A)         // 墨黑（文字）

// 辅助色
val FilmLightGray = Color(0xFFE5E5E5)        // 浅灰（分隔线）
val FilmDarkGray = Color(0xFF4A4A4A)         // 深灰（次要文字）
val FilmWhite = Color(0xFFFFFFFF)            // 纯白（高光）

// 半透明色（用于毛玻璃效果）
val FilmWhiteGlass = Color(0xCCFFFFFF)       // 80% 白色
val FilmBeigeGlass = Color(0xCCF5F1E8)       // 80% 米白色

// 胶片质感色
val FilmSprocketGray = Color(0x1A000000)     // 10% 黑色（齿孔纹理）
val FilmGrainOverlay = Color(0x0D000000)     // 5% 黑色（颗粒叠加）

// =============================================================================
// M3 DARK THEME - Complete Color Palette (Professional editing mode)
// Harmonized with FilmTracker warm film tones
// =============================================================================

// Primary colors (Film Gold)
val DarkPrimary = Color(0xFFFFB800)               // Primary color
val DarkOnPrimary = Color(0xFF3D2E00)             // On primary text
val DarkPrimaryContainer = Color(0xFF584400)      // Dark gold container
val DarkOnPrimaryContainer = Color(0xFFFFDF9E)    // Light gold text

// Secondary colors (Warm Gray)
val DarkSecondary = Color(0xFFD5C4A1)             // Secondary color
val DarkOnSecondary = Color(0xFF383016)           // On secondary text
val DarkSecondaryContainer = Color(0xFF50462A)    // Dark gray container
val DarkOnSecondaryContainer = Color(0xFFF2E0BB)  // Light gray text

// Tertiary colors (Blue Accent)
val DarkTertiary = Color(0xFF4A90E2)              // Blue accent
val DarkOnTertiary = Color(0xFF002B4D)            // Dark blue text
val DarkTertiaryContainer = Color(0xFF004A77)     // Blue container
val DarkOnTertiaryContainer = Color(0xFFCAE6FF)   // Light blue text

// Error colors
val DarkError = Color(0xFFFFB4AB)                 // Error color
val DarkOnError = Color(0xFF690005)               // Error text
val DarkErrorContainer = Color(0xFF93000A)        // Error container
val DarkOnErrorContainer = Color(0xFFFFDAD6)      // Error container text

// Background and Surface colors
val DarkBackground = Color(0xFF1A1A1A)            // Background
val DarkOnBackground = Color(0xFFE8E1D9)          // On background text
val DarkSurface = Color(0xFF1A1A1A)               // Surface
val DarkOnSurface = Color(0xFFE8E1D9)             // On surface text

// Surface variants
val DarkSurfaceVariant = Color(0xFF4D4639)        // Surface variant
val DarkOnSurfaceVariant = Color(0xFFD0C5B4)      // Surface variant text

// Surface containers (M3 tonal elevation)
val DarkSurfaceContainerLowest = Color(0xFF0F0F0F)   // Lowest elevation
val DarkSurfaceContainerLow = Color(0xFF1E1E1E)      // Low elevation
val DarkSurfaceContainer = Color(0xFF232323)         // Default elevation
val DarkSurfaceContainerHigh = Color(0xFF2D2D2D)     // High elevation
val DarkSurfaceContainerHighest = Color(0xFF383838)  // Highest elevation

// Surface dim and bright
val DarkSurfaceDim = Color(0xFF1A1A1A)            // Dimmed surface
val DarkSurfaceBright = Color(0xFF3F3F3F)         // Bright surface

// Outline colors
val DarkOutline = Color(0xFF9A8F80)               // Outline
val DarkOutlineVariant = Color(0xFF4D4639)        // Outline variant

// Inverse colors
val DarkInverseSurface = Color(0xFFE8E1D9)        // Inverse surface
val DarkInverseOnSurface = Color(0xFF322F2A)      // Inverse surface text
val DarkInversePrimary = Color(0xFF735C00)        // Inverse primary

// Special colors
val DarkScrim = Color(0xFF000000)                 // Scrim
val DarkSurfaceTint = Color(0xFFFFB800)           // Surface tint (same as primary)

// =============================================================================
// M3 LIGHT THEME (Vintage) - Complete Color Palette (Film vintage style)
// Harmonized with FilmTracker warm film tones
// =============================================================================

// Primary colors (Caramel Orange)
val VintagePrimary = Color(0xFF855400)            // Primary color
val VintageOnPrimary = Color(0xFFFFFFFF)          // On primary text
val VintagePrimaryContainer = Color(0xFFFFDDB3)   // Light orange container
val VintageOnPrimaryContainer = Color(0xFF2A1700) // Dark brown text

// Secondary colors (Mint Green)
val VintageSecondary = Color(0xFF4D6357)          // Secondary color
val VintageOnSecondary = Color(0xFFFFFFFF)        // On secondary text
val VintageSecondaryContainer = Color(0xFFD4F5ED) // Light mint container
val VintageOnSecondaryContainer = Color(0xFF002019) // Dark teal text

// Tertiary colors (Milky Blue)
val VintageTertiary = Color(0xFF4A6278)           // Tertiary color
val VintageOnTertiary = Color(0xFFFFFFFF)         // On tertiary text
val VintageTertiaryContainer = Color(0xFFDDE5F0)  // Light blue container
val VintageOnTertiaryContainer = Color(0xFF1A2633) // Dark blue text

// Error colors
val VintageError = Color(0xFFBA1A1A)              // Error color
val VintageOnError = Color(0xFFFFFFFF)            // Error text
val VintageErrorContainer = Color(0xFFFFDAD6)     // Error container
val VintageOnErrorContainer = Color(0xFF410002)   // Error container text

// Background and Surface colors
val VintageBackground = Color(0xFFFFF8F0)         // Background (warm white)
val VintageOnBackground = Color(0xFF1E1B16)       // On background text
val VintageSurface = Color(0xFFFFF8F0)            // Surface
val VintageOnSurface = Color(0xFF1E1B16)          // On surface text

// Surface variants
val VintageSurfaceVariant = Color(0xFFEBE1CF)     // Surface variant
val VintageOnSurfaceVariant = Color(0xFF4C4639)   // Surface variant text

// Surface containers (M3 tonal elevation)
val VintageSurfaceContainerLowest = Color(0xFFFFFFFF)  // Lowest elevation
val VintageSurfaceContainerLow = Color(0xFFFFF1E0)     // Low elevation
val VintageSurfaceContainer = Color(0xFFF9ECDB)        // Default elevation
val VintageSurfaceContainerHigh = Color(0xFFF3E6D5)    // High elevation
val VintageSurfaceContainerHighest = Color(0xFFEDE0D0) // Highest elevation

// Surface dim and bright
val VintageSurfaceDim = Color(0xFFE0D9D0)         // Dimmed surface
val VintageSurfaceBright = Color(0xFFFFF8F0)      // Bright surface

// Outline colors
val VintageOutline = Color(0xFF7E7667)            // Outline
val VintageOutlineVariant = Color(0xFFCFC5B4)     // Outline variant

// Inverse colors
val VintageInverseSurface = Color(0xFF34302A)     // Inverse surface
val VintageInverseOnSurface = Color(0xFFF8EFE6)   // Inverse surface text
val VintageInversePrimary = Color(0xFFFFB77C)     // Inverse primary

// Special colors
val VintageScrim = Color(0xFF000000)              // Scrim
val VintageSurfaceTint = Color(0xFFE89B5A)        // Surface tint (same as primary)
