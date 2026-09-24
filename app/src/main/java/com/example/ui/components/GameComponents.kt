package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.DistanceWarmth
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun RangeVisualizer(
    currentMin: Int,
    currentMax: Int,
    absoluteMin: Int,
    absoluteMax: Int,
    modifier: Modifier = Modifier
) {
    val totalSpan = (absoluteMax - absoluteMin).coerceAtLeast(1).toFloat()
    val remainingSpan = (currentMax - currentMin + 1).coerceAtLeast(1)
    val leftFraction = ((currentMin - absoluteMin).toFloat() / totalSpan).coerceIn(0f, 1f)
    val rightFraction = ((currentMax - absoluteMin).toFloat() / totalSpan).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Possible Range",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$remainingSpan possible values",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Visual bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                // Active remaining range slice
                val sliceStart = (leftFraction * 100).toInt()
                val sliceEnd = (rightFraction * 100).toInt()

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startPx = size.width * leftFraction
                    val endPx = size.width * rightFraction
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E676),
                                Color(0xFFFFD600),
                                Color(0xFFFF5252)
                            )
                        ),
                        topLeft = Offset(startPx, 0f),
                        size = androidx.compose.ui.geometry.Size(
                            width = (endPx - startPx).coerceAtLeast(8f),
                            height = size.height
                        )
                    )
                }
            }

            // Min and Max Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RangeBadge(label = "MIN", value = currentMin, color = Color(0xFF00E676))
                Text(
                    text = "Secret is between",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                RangeBadge(label = "MAX", value = currentMax, color = Color(0xFFFF5252))
            }
        }
    }
}

@Composable
private fun RangeBadge(label: String, value: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StreakBanner(streak: Int, modifier: Modifier = Modifier) {
    if (streak <= 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "fire_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_scale"
    )

    Surface(
        modifier = modifier.testTag("streak_banner"),
        color = Color(0xFFFF6D00).copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Win Streak",
                tint = Color(0xFFFF6D00),
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
            )
            Text(
                text = "$streak WIN STREAK",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFAB00)
            )
        }
    }
}

@Composable
fun WarmthChip(warmth: DistanceWarmth, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(warmth.colorHex).copy(alpha = 0.22f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(warmth.colorHex).copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = warmth.emoji, fontSize = 16.sp)
            Text(
                text = warmth.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(warmth.colorHex)
            )
        }
    }
}

@Composable
fun NumericKeypad(
    currentInput: String,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onQuickStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Quick step row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickStepButton(text = "-10", onClick = { onQuickStep(-10) }, modifier = Modifier.weight(1f))
            QuickStepButton(text = "-1", onClick = { onQuickStep(-1) }, modifier = Modifier.weight(1f))
            QuickStepButton(text = "+1", onClick = { onQuickStep(1) }, modifier = Modifier.weight(1f))
            QuickStepButton(text = "+10", onClick = { onQuickStep(10) }, modifier = Modifier.weight(1f))
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "DEL")
        )

        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (key in row) {
                    when (key) {
                        "C" -> KeypadButton(
                            content = {
                                Text(
                                    "CLEAR",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = onClear,
                            modifier = Modifier.weight(1f).testTag("keypad_clear")
                        )
                        "DEL" -> KeypadButton(
                            content = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = onBackspace,
                            modifier = Modifier.weight(1f).testTag("keypad_del")
                        )
                        else -> KeypadButton(
                            content = {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = { onDigitClick(key) },
                            modifier = Modifier.weight(1f).testTag("keypad_$key")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStepButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (e: Exception) {
                    // Ignore
                }
                onClick()
            }),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun KeypadButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (e: Exception) {
                    // Ignore
                }
                onClick()
            }),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Color
)

@Composable
fun ConfettiCelebration(modifier: Modifier = Modifier) {
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            val colors = listOf(
                Color(0xFFFFD600),
                Color(0xFFFF1744),
                Color(0xFF00E676),
                Color(0xFF00B0FF),
                Color(0xFFE040FB),
                Color(0xFFFF9100)
            )
            for (i in 0..60) {
                add(
                    ConfettiParticle(
                        x = Random.nextFloat(),
                        y = -0.1f - Random.nextFloat() * 0.4f,
                        vx = (Random.nextFloat() - 0.5f) * 0.015f,
                        vy = 0.015f + Random.nextFloat() * 0.025f,
                        size = 8f + Random.nextFloat() * 12f,
                        color = colors[Random.nextInt(colors.size)]
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            for (p in particles) {
                p.x += p.vx
                p.y += p.vy
                if (p.y > 1.1f) {
                    p.y = -0.1f
                    p.x = Random.nextFloat()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        for (p in particles) {
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
