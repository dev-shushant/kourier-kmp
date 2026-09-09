package dev.shushant.kourier.ui.triggers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.components.KourierBubbleImage
import dev.shushant.kourier.ui.theme.KourierTheme
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

private val BUBBLE_SIZE = 58.dp
private val EDGE_PADDING = 16.dp
private val TOP_PADDING = 60.dp
private val BOTTOM_PADDING = 60.dp

@Composable
fun FloatingDebugBubble(
    telemetry: TelemetryStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPositionChanged: ((x: Float, y: Float, size: Float) -> Unit)? = null
) {
    KourierTheme {
        val colors = LocalKourierColors.current
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        val bubbleSizePx = with(density) { BUBBLE_SIZE.toPx() }
        val edgePaddingPx = with(density) { EDGE_PADDING.toPx() }
        val topPaddingPx = with(density) { TOP_PADDING.toPx() }
        val bottomPaddingPx = with(density) { BOTTOM_PADDING.toPx() }

        // Continuous Infinite Animation around the perimeter/corners
        val infiniteTransition = rememberInfiniteTransition(label = "bubbleGlow")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val constraints = this
            val parentWidthPx = with(density) { constraints.maxWidth.toPx() }
            val parentHeightPx = with(density) { constraints.maxHeight.toPx() }

            val minX = edgePaddingPx
            val maxX = (parentWidthPx - bubbleSizePx - edgePaddingPx).coerceAtLeast(minX)
            val minY = topPaddingPx
            val maxY = (parentHeightPx - bubbleSizePx - bottomPaddingPx).coerceAtLeast(minY)

            val animOffsetX = remember { Animatable(maxX) }
            var offsetY by remember { mutableStateOf(topPaddingPx + 80f) }

            androidx.compose.runtime.LaunchedEffect(animOffsetX.value, offsetY, bubbleSizePx, density) {
                with(density) {
                    onPositionChanged?.invoke(
                        animOffsetX.value.toDp().value,
                        offsetY.coerceIn(minY, maxY).toDp().value,
                        bubbleSizePx.toDp().value
                    )
                }
            }

            val hasErrors = telemetry.errorCount > 0
            val isPending = telemetry.activeRequests > 0
            val glowColor = when {
                hasErrors -> colors.statusServerError
                isPending -> colors.statusPending
                else      -> Color(0xFF00E5FF)
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            animOffsetX.value.roundToInt(),
                            offsetY.coerceIn(minY, maxY).roundToInt()
                        )
                    }
                    .size(BUBBLE_SIZE)
                    .pointerInput(minX, maxX, minY, maxY) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var dragAccumX = 0f
                            var dragAccumY = 0f
                            var isDragging = false
                            val touchSlop = viewConfiguration.touchSlop
                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    if (!isDragging) {
                                        onClick()
                                    } else {
                                        val currentX = animOffsetX.value
                                        val middleX = (minX + maxX) / 2f
                                        val targetX = if (currentX < middleX) minX else maxX
                                        with(density) {
                                            onPositionChanged?.invoke(
                                                targetX.toDp().value,
                                                offsetY.coerceIn(minY, maxY).toDp().value,
                                                bubbleSizePx.toDp().value
                                            )
                                        }
                                        scope.launch {
                                            animOffsetX.animateTo(
                                                targetValue = targetX,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                    break
                                }

                                val dragAmount = change.positionChange()
                                dragAccumX += dragAmount.x
                                dragAccumY += dragAmount.y
                                val distance = hypot(dragAccumX, dragAccumY)

                                if (!isDragging && distance > touchSlop) {
                                    isDragging = true
                                }

                                if (isDragging) {
                                    change.consume()
                                    val newX = (animOffsetX.value + dragAmount.x).coerceIn(minX, maxX)
                                    val newY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                                    scope.launch {
                                        animOffsetX.snapTo(newX)
                                    }
                                    offsetY = newY
                                    with(density) {
                                        onPositionChanged?.invoke(
                                            newX.toDp().value,
                                            newY.toDp().value,
                                            bubbleSizePx.toDp().value
                                        )
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer Animated Pulsing Glow Ring
                Box(
                    modifier = Modifier
                        .size(BUBBLE_SIZE)
                        .scale(pulseScale)
                        .rotate(rotationAngle)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    glowColor.copy(alpha = 0.9f),
                                    glowColor.copy(alpha = 0.1f),
                                    glowColor.copy(alpha = 0.8f),
                                    glowColor.copy(alpha = 0.1f),
                                    glowColor.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                // Inner Main Circular Badge Container with Icon
                Box(
                    modifier = Modifier
                        .size(BUBBLE_SIZE - 4.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(colors.background)
                        .border(1.5.dp, glowColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    KourierBubbleImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Badge indicator (Errors / Pending)
                when {
                    hasErrors -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(colors.statusServerError)
                                .border(1.5.dp, Color.White, CircleShape)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (telemetry.errorCount > 99) "99+" else "${telemetry.errorCount}",
                                style = KourierTypography.badge,
                                color = Color.White
                            )
                        }
                    }
                    isPending -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(colors.statusPending)
                                .border(1.5.dp, Color.White, CircleShape)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${telemetry.activeRequests}",
                                style = KourierTypography.badge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
