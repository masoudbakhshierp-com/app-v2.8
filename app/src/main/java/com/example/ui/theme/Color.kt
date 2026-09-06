package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Saba Logistics Design System Palette (Based on Reference Design)
val CleanGreenPrimary = Color(0xFF087A5A) // Deep professional brand green
val CleanGreenPrimaryDark = Color(0xFF056044) // Primary dark green
val CleanGreenPrimaryLight = Color(0xFFE7F7F1) // Very light mint green surface / active container
val CleanGreenAccent = Color(0xFF16865F) // Medium success green
val CleanGreenDarkHeader = Color(0xFF087A5A) // Header curved container

// Secondary / Warning & Pending Accents
val CleanOrangeAccent = Color(0xFFF2A51A) // Secondary amber accent
val CleanWarningBg = Color(0xFFFFF4D8) // Warning / Pending pill background
val CleanWarningText = Color(0xFFB45309) // Warm warning text

// Alert / Error
val CleanRedError = Color(0xFFD94B4B)
val CleanRedContainer = Color(0xFFFEE2E2)

// Light Surfaces & Canvas
val CleanLightBackground = Color(0xFFF7FAF9) // Clean mint-tinted light canvas
val CleanLightSurface = Color(0xFFFFFFFF) // Crisp white card & surface
val CleanLightSurfaceVariant = Color(0xFFEDF5F2)
val CleanLightOnSurface = Color(0xFF172321) // Deep slate text primary
val CleanLightOnSurfaceMuted = Color(0xFF687773) // Slate text secondary
val CleanLightOutline = Color(0xFFDDE8E4) // Border color
val CleanLightOutlineVariant = Color(0xFFE8F1EE)

// Dark Surfaces (Optional Dark Theme)
val CleanDarkBackground = Color(0xFF091714)
val CleanDarkSurface = Color(0xFF112520)
val CleanDarkSurfaceVariant = Color(0xFF1A332C)
val CleanDarkOnSurface = Color(0xFFEDF5F2)
val CleanDarkOutline = Color(0xFF26463D)
val CleanDarkOutlineVariant = Color(0xFF1C362F)

// Aliases for backwards compatibility with existing screen references
val CleanBluePrimary = CleanGreenPrimary
val CleanBluePrimaryLight = CleanGreenAccent
val CleanBlueOnPrimary = Color(0xFFFFFFFF)
val CleanBlueContainer = CleanGreenPrimaryLight
val CleanBlueOnContainer = CleanGreenPrimaryDark

val CleanTealAccent = CleanGreenAccent
val CleanTealContainer = CleanGreenPrimaryLight

val CleanPurpleAccent = CleanOrangeAccent
val CleanPurpleContainer = CleanWarningBg

val CleanRedAccent = CleanRedError

val EmeraldMint = CleanGreenPrimaryLight
val EmeraldDarkGreen = CleanGreenPrimary
val GoldLight = CleanWarningBg
val GoldAccent = CleanWarningText

// Semantic Status Colors
val StatusPendingBg = CleanWarningBg
val StatusPendingText = CleanWarningText

val StatusAssignedBg = CleanWarningBg
val StatusAssignedText = CleanWarningText

val StatusInspectionBg = CleanGreenPrimaryLight
val StatusInspectionText = CleanGreenPrimary

val StatusWorkshopBg = CleanGreenPrimaryLight
val StatusWorkshopText = CleanGreenPrimaryDark

val StatusSettledBg = CleanGreenPrimaryLight
val StatusSettledText = CleanGreenAccent
