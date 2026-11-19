package com.tumba.ai.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark palette — obsidian with a restrained iris accent. The product's primary identity.
val TumbaBlack = Color(0xFF07080B)
val TumbaSurface = Color(0xFF101218)
val TumbaSurfaceRaised = Color(0xFF171A23)
val TumbaSurfaceLow = Color(0xFF0B0D12)
val TumbaSurfaceElevated = Color(0xFF1E2230)
val TumbaText = Color(0xFFF4F3F0)
val TumbaTextSecondary = Color(0xFF9B9CA8)
val TumbaAccent = Color(0xFFB9A8FF)
val TumbaAccentDeep = Color(0xFF7B68EE)
val TumbaAccentMint = Color(0xFF7BE7D2)
val TumbaOutline = Color(0xFF2A2E3B)
val TumbaOutlineSoft = Color(0xFF1A1D26)
val TumbaError = Color(0xFFF1A7A7)

// Light palette — warm ivory paper, still calm and low-contrast.
val PaperBackground = Color(0xFFF7F6F3)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceRaised = Color(0xFFEFEDE8)
val PaperSurfaceLow = Color(0xFFFBFAF7)
val PaperSurfaceElevated = Color(0xFFE7E4DD)
val PaperText = Color(0xFF121319)
val PaperTextSecondary = Color(0xFF6B6B76)
val PaperAccent = Color(0xFF5C4BD8)
val PaperAccentDeep = Color(0xFF4A3BC4)
val PaperAccentMint = Color(0xFF23B49A)
val PaperOutline = Color(0xFFE3E0D9)
val PaperOutlineSoft = Color(0xFFEDEBE5)
val PaperError = Color(0xFFB25454)

/**
 * Brand gradient used sparingly: the logo mark, the primary send action and the selected
 * conversation indicator. Everything else stays monochrome so the accent keeps its weight.
 */
val TumbaBrandGradient = Brush.linearGradient(
    colors = listOf(TumbaAccentDeep, TumbaAccent, TumbaAccentMint),
)
