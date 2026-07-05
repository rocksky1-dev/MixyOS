package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

// Theme neon colors
val CyberCyan = Color(0xFF00F0FF)
val CyberPink = Color(0xFFFF007F)
val CyberPurple = Color(0xFF9D00FF)
val SpaceDark = Color(0xFF080C14)
val SpaceCardBg = Color(0x12FFFFFF)
val GlassBorder = Color(0x22FFFFFF)

@Composable
fun GlowBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // We animate positions to make the background spots drift like live cosmic dust
    val infiniteTransition = rememberInfiniteTransition(label = "NebulaDrift")
    val driftX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceDark)
            .drawBehind {
                // Top-right cyan glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.8f + driftX - 100f, size.height * 0.2f + driftY - 75f),
                        radius = size.width * 0.7f
                    )
                )

                // Bottom-left violet glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberPurple.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.2f - driftX + 100f, size.height * 0.8f - driftY + 75f),
                        radius = size.width * 0.8f
                    )
                )

                // Middle magenta spark
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberPink.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.width * 0.5f
                    )
                )
            }
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = SpaceCardBg,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowColor: Color = CyberCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CyberPurple, glowColor)
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun VoiceWaveforms(
    waveLevels: List<Float>,
    modifier: Modifier = Modifier,
    waveColor: Color = CyberPurple
) {
    // Render interactive pulsating voice waves spreading out from a central microphone
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveLevels.forEachIndexed { index, level ->
            // Use simple transitions to smooth out wave movements
            val animatedHeight by animateFloatAsState(
                targetValue = (level * 40f).coerceIn(4f, 45f),
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                label = "waveHeight_$index"
            )

            // Dynamic color shading based on wave intensity
            val shadeColor = remember(level) {
                if (level > 0.6f) CyberPink else if (level > 0.3f) waveColor else CyberCyan
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(shadeColor, shadeColor.copy(alpha = 0.3f))
                        )
                    )
            )
        }
    }
}
