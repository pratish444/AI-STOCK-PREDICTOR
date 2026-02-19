package com.example.stocktracker.components.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

fun Modifier.shimmerEffect(
    durationMillis: Int = 1000,
    color: Color = Color.LightGray
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = 0.1f),
                color.copy(alpha = 0.3f),
                color.copy(alpha = 0.1f)
            ),
            start = Offset(translateAnim.value - 500, 0f),
            end = Offset(translateAnim.value, 0f)
        )
    )
}

fun Modifier.pulseAnimation(
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale = infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

fun Modifier.floatAnimation(
    offsetY: Float = 10f,
    durationMillis: Int = 2000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = offsetY,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    graphicsLayer {
        translationY = offset.value
    }
}

@Composable
fun FadeInAnimation(
    visible: Boolean,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val animation = remember {
        Animatable(0f)
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis.toLong())
            animation.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        } else {
            animation.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animation.value
            translationY = (1f - animation.value) * 20
        }
    ) {
        content()
    }
}

@Composable
fun SlideUpAnimation(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        content()
    }
}

@Composable
fun PressableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isPressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        content(isPressed)
    }
}

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier
) {
    var currentValue by remember { mutableStateOf(0) }

    LaunchedEffect(targetValue) {
        animate(
            initialValue = currentValue.toFloat(),
            targetValue = targetValue.toFloat(),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        ) { value, _ ->
            currentValue = value.toInt()
        }
    }

    androidx.compose.material3.Text(
        text = currentValue.toString(),
        modifier = modifier
    )
}

@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .shimmerEffect()
    )
}

@Composable
fun <T> StaggeredList(
    items: List<T>,
    key: ((T) -> Any)? = null,
    content: @Composable (T) -> Unit
) {
    items.forEachIndexed { index, item ->
        val visible = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(index * 50L)
            visible.value = true
        }

        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
        ) {
            content(item)
        }
    }
}