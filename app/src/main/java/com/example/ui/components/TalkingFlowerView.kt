package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun TalkingFlowerView(
    isSpeaking: Boolean,
    phraseText: String,
    flowerColorHex: String = "#FFEB3B", // default Nintendo Yellow
    onTap: () -> Unit = {}
) {
    // 1. Blink Animation (Periodic eye blink)
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2000..6000).random().toLong())
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }

    // 2. Swaying Stem Animation (Slight continuous sway)
    val infiniteTransition = rememberInfiniteTransition(label = "Sway")
    val swayAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SwayAngle"
    )

    // 3. Petal Spin Animation (continuous rotate)
    val petalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PetalRotation"
    )

    // 4. Talking Mouth Animation
    val mouthYScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MouthScale"
    )

    // 5. Flower Bounce Animation on Tap/Speak
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            scaleAnim.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Interactive Tap handler
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2)) // Cute blue sky gradient
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        // Decorative Clouds in Sky Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Little decorative cloud
            Canvas(modifier = Modifier.size(60.dp, 30.dp).offset(20.dp, 15.dp)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.7f),
                    size = size,
                    cornerRadius = CornerRadius(15f, 15f)
                )
            }
            Canvas(modifier = Modifier.size(80.dp, 36.dp).align(Alignment.TopEnd).offset((-30).dp, 20.dp)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.7f),
                    size = size,
                    cornerRadius = CornerRadius(18f, 18f)
                )
            }
        }

        // The Flower Structure
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
        ) {
            // Main Render Canvas
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = 10.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f + 20f)
                val scale = scaleAnim.value

                // 1. Draw Stem (swaying using swayAngle)
                val stemPath = Path().apply {
                    moveTo(center.x, center.y + 10f)
                    // Quadratic curve for a natural organic sway
                    val endX = center.x + (swayAngle * 1.5f)
                    val endY = center.y + 90f
                    val controlX = center.x + (swayAngle * 0.7f)
                    val controlY = center.y + 50f
                    quadraticTo(controlX, controlY, endX, endY)
                }
                drawPath(
                    path = stemPath,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = 12f * scale)
                )

                // 2. Draw Leaves (on the stem)
                val leafX = center.x + (swayAngle * 1.1f)
                val leafY = center.y + 60f
                drawOval(
                    color = Color(0xFF81C784),
                    topLeft = Offset(leafX - 35f * scale, leafY - 10f * scale),
                    size = Size(30f * scale, 15f * scale)
                )
                drawOval(
                    color = Color(0xFF81C784),
                    topLeft = Offset(leafX + 5f * scale, leafY - 5f * scale),
                    size = Size(30f * scale, 15f * scale)
                )

                // 3. Draw Petals (Spun & Scaled)
                val flowerColor = run {
                    try {
                        Color(android.graphics.Color.parseColor(flowerColorHex))
                    } catch (e: Exception) {
                        Color(0xFFFFEB3B) // yellow fallback
                    }
                }
                val petalOutlineColor = flowerColor.copy(alpha = 0.7f)

                // Draw 8 round overlapping petals around the flower face center
                val petalCount = 8
                val petalRadius = 45f * scale
                val faceRadius = 48f * scale

                for (i in 0 until petalCount) {
                    val angleDeg = petalRotation + (i * (360f / petalCount))
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val petalOffsetX = (cos(angleRad) * faceRadius).toFloat()
                    val petalOffsetY = (sin(angleRad) * faceRadius).toFloat()

                    drawCircle(
                        color = flowerColor,
                        radius = petalRadius,
                        center = Offset(center.x + petalOffsetX, center.y + petalOffsetY)
                    )
                    drawCircle(
                        color = petalOutlineColor,
                        radius = petalRadius,
                        center = Offset(center.x + petalOffsetX, center.y + petalOffsetY),
                        style = Stroke(width = 3f)
                    )
                }

                // 4. Draw Face Container (The yellow/orange center)
                drawCircle(
                    color = Color(0xFFFFF176), // bright face color
                    radius = faceRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFBC02D), // face border
                    radius = faceRadius,
                    center = center,
                    style = Stroke(width = 4f * scale)
                )

                // 5. Draw Cute Eyes
                val eyeWidth = 10f * scale
                val eyeHeight = 16f * scale
                val leftEyeCenter = Offset(center.x - 18f * scale, center.y - 6f * scale)
                val rightEyeCenter = Offset(center.x + 18f * scale, center.y - 6f * scale)

                if (isBlinking) {
                    // Closed Eyes (arcs or flat lines)
                    drawLine(
                        color = Color.Black,
                        start = Offset(leftEyeCenter.x - 8f, leftEyeCenter.y),
                        end = Offset(leftEyeCenter.x + 8f, leftEyeCenter.y),
                        strokeWidth = 4f * scale
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(rightEyeCenter.x - 8f, rightEyeCenter.y),
                        end = Offset(rightEyeCenter.x + 8f, rightEyeCenter.y),
                        strokeWidth = 4f * scale
                    )
                } else {
                    // Big friendly black eyes
                    drawOval(
                        color = Color.Black,
                        topLeft = Offset(leftEyeCenter.x - eyeWidth / 2, leftEyeCenter.y - eyeHeight / 2),
                        size = Size(eyeWidth, eyeHeight)
                    )
                    drawOval(
                        color = Color.Black,
                        topLeft = Offset(rightEyeCenter.x - eyeWidth / 2, rightEyeCenter.y - eyeHeight / 2),
                        size = Size(eyeWidth, eyeHeight)
                    )

                    // Eye sparkles (little white dots)
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f * scale,
                        center = Offset(leftEyeCenter.x - 2f * scale, leftEyeCenter.y - 3f * scale)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f * scale,
                        center = Offset(rightEyeCenter.x - 2f * scale, rightEyeCenter.y - 3f * scale)
                    )
                }

                // 6. Draw Rosy Cheeks
                drawCircle(
                    color = Color(0xFFFF8A80).copy(alpha = 0.6f),
                    radius = 8f * scale,
                    center = Offset(center.x - 30f * scale, center.y + 4f * scale)
                )
                drawCircle(
                    color = Color(0xFFFF8A80).copy(alpha = 0.6f),
                    radius = 8f * scale,
                    center = Offset(center.x + 30f * scale, center.y + 4f * scale)
                )

                // 7. Draw Mouth (Animates based on isSpeaking)
                val mouthWidth = 14f * scale
                val mouthBaseHeight = 12f * scale
                val currentMouthHeight = if (isSpeaking) {
                    mouthBaseHeight * mouthYScale
                } else {
                    4f * scale // slight happy smile gap when quiet
                }

                val mouthTopY = center.y + 12f * scale

                // Mouth shape: Cute rounded black oval
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(center.x - mouthWidth / 2f, mouthTopY),
                    size = Size(mouthWidth, currentMouthHeight),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale)
                )

                // Draw a tiny cute pink tongue inside if speaking and mouth is open
                if (isSpeaking && currentMouthHeight > 6f * scale) {
                    drawRoundRect(
                        color = Color(0xFFF48FB1),
                        topLeft = Offset(center.x - 4f * scale, mouthTopY + currentMouthHeight - 4f * scale),
                        size = Size(8f * scale, 4f * scale),
                        cornerRadius = CornerRadius(2f * scale, 2f * scale)
                    )
                }

                // 8. Draw Orange Flower Pot at the very bottom
                val potTop = center.y + 85f
                val potWidth = 75f * scale
                val potHeight = 45f * scale

                // Rim of the pot
                drawRoundRect(
                    color = Color(0xFFFFB74D), // orange pot rim
                    topLeft = Offset(center.x - (potWidth + 10f) / 2f, potTop),
                    size = Size(potWidth + 10f, 10f * scale),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )

                // Body of the pot (slightly tapered down)
                val potPath = Path().apply {
                    moveTo(center.x - potWidth / 2f, potTop + 10f * scale)
                    lineTo(center.x + potWidth / 2f, potTop + 10f * scale)
                    lineTo(center.x + (potWidth - 12f) / 2f, potTop + potHeight)
                    lineTo(center.x - (potWidth - 12f) / 2f, potTop + potHeight)
                    close()
                }
                drawPath(
                    path = potPath,
                    color = Color(0xFFFF9800) // orange pot body
                )

                // Rim shadow line
                drawLine(
                    color = Color(0xFFE65100).copy(alpha = 0.4f),
                    start = Offset(center.x - potWidth / 2f, potTop + 10f * scale),
                    end = Offset(center.x + potWidth / 2f, potTop + 10f * scale),
                    strokeWidth = 2f
                )
            }
        }

        // 9. Speech Bubble overlaid at the top of the container
        if (phraseText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(16.dp))
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = phraseText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            }

            // Draw Speech Bubble pointer pointing down to the flower
            Canvas(
                modifier = Modifier
                    .size(20.dp, 12.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 66.dp) // align right below the bubble
            ) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path = path, color = Color.White)
            }
        }
    }
}
