package com.ubimatic.payunpaids.ui.theme

import androidx.compose.ui.graphics.Color

// Palette mirrors SnapAndMail for visual consistency across the suite
val Accent = Color(0xFFE8793A)
val AccentDim = Color(0x26E8793A)
val Background = Color(0xFF0F0F0F)
val Surface = Color(0xFF1A1A1A)
val Surface2 = Color(0xFF242424)
val Surface3 = Color(0xFF2E2E2E)
val TextPrimary = Color(0xFFF0EDE8)
val TextSecond = Color(0xFF9A9690)
val TextHint = Color(0xFF5A5652)
val Success = Color(0xFF4CAF82)
val Danger = Color(0xFFE85C5C)
val BorderWeak = Color(0x14FFFFFF)
val BorderMid = Color(0x24FFFFFF)

// Backwards-compatible aliases so existing code keeps compiling while we migrate
val Amber = Accent
val AmberDark = Color(0xFF1A0F00)
val DarkBg = Background
val DarkSurface = Surface
val DarkBorder = BorderWeak
val DarkBorderLight = BorderMid
val TextSecondary = TextSecond
val TextMuted = TextHint
val Green = Success
val WarningBg = Color(0xFF2A1800)
val WarningText = Accent
val ErrorRed = Danger
