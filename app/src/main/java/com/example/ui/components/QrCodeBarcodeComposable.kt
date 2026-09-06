package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Draws a clean, crisp 1D Barcode (Code 128 style) using Jetpack Compose Canvas.
 */
@Composable
fun BarcodeView(
    code: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
    height: Dp = 50.dp,
    showText: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val width = size.width
                val canvasHeight = size.height

                // Generate pseudo-deterministic bar patterns based on code hash
                val seed = code.hashCode()
                val barCount = 45
                val barWidth = width / (barCount * 1.2f)
                var currentX = 0f

                // Quiet zone at start
                currentX += barWidth * 2

                for (i in 0 until barCount) {
                    val pseudoBit = ((seed shr (i % 31)) xor (i * 17) xor code.length) and 1 == 0
                    val isGuardBar = i < 3 || i > barCount - 4 || i == barCount / 2
                    val isBar = pseudoBit || isGuardBar

                    if (isBar) {
                        val barThickness = if ((i % 3 == 0) && !isGuardBar) barWidth * 1.8f else barWidth
                        drawRect(
                            color = barColor,
                            topLeft = Offset(currentX, 0f),
                            size = Size(barThickness, canvasHeight)
                        )
                        currentX += barThickness + (barWidth * 0.4f)
                    } else {
                        currentX += barWidth * 1.2f
                    }
                    if (currentX >= width - barWidth * 2) break
                }
            }

            if (showText) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = barColor,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Draws a clean 2D QR Code pattern with corner finder patterns using Compose Canvas.
 */
@Composable
fun QrCodeView(
    code: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = this.size.width
                val matrixSize = 17 // 17x17 grid
                val cellSize = canvasWidth / matrixSize

                val seed = abs(code.hashCode())

                // Draw background
                drawRect(color = backgroundColor, size = this.size)

                // 1. Draw Finder Patterns (3 corners: top-left, top-right, bottom-left)
                fun drawFinderPattern(startX: Int, startY: Int) {
                    // Outer 7x7 square
                    drawRect(
                        color = qrColor,
                        topLeft = Offset(startX * cellSize, startY * cellSize),
                        size = Size(7 * cellSize, 7 * cellSize)
                    )
                    // Inner 5x5 white square
                    drawRect(
                        color = backgroundColor,
                        topLeft = Offset((startX + 1) * cellSize, (startY + 1) * cellSize),
                        size = Size(5 * cellSize, 5 * cellSize)
                    )
                    // Center 3x3 square
                    drawRect(
                        color = qrColor,
                        topLeft = Offset((startX + 2) * cellSize, (startY + 2) * cellSize),
                        size = Size(3 * cellSize, 3 * cellSize)
                    )
                }

                drawFinderPattern(0, 0) // Top-Left
                drawFinderPattern(matrixSize - 7, 0) // Top-Right
                drawFinderPattern(0, matrixSize - 7) // Bottom-Left

                // 2. Draw Data Modules
                for (row in 0 until matrixSize) {
                    for (col in 0 until matrixSize) {
                        // Skip Finder patterns
                        val isTopLeft = row in 0..7 && col in 0..7
                        val isTopRight = row in 0..7 && col in (matrixSize - 8) until matrixSize
                        val isBottomLeft = row in (matrixSize - 8) until matrixSize && col in 0..7

                        if (!isTopLeft && !isTopRight && !isBottomLeft) {
                            val bitIndex = (row * matrixSize + col) % 31
                            val isModuleOn = (((seed shr bitIndex) xor (row * 7 + col * 13)) and 1) == 0
                            if (isModuleOn) {
                                drawRect(
                                    color = qrColor,
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = Size(cellSize, cellSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
