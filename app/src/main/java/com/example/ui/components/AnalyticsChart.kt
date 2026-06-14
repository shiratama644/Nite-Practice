package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DailyProgressPoint(
    val dateString: String, // e.g., "06/12"
    val avgResponseTimeMs: Float,
    val accuracyPercentage: Float, // 0 to 100
    val totalAttempts: Int
)

@Composable
fun AnalyticsChart(
    dataPoints: List<DailyProgressPoint>,
    showSpeed: Boolean, // true for response time, false for accuracy
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "データがまだありません。練習を開始しましょう！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    val textMeasurer = rememberTextMeasurer()
    val isDark = isSystemInDarkTheme()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        val width = size.width
        val height = size.height
        
        // Boundaries for drawing area
        val paddingLeft = 46.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 16.dp.toPx()
        val paddingBottom = 28.dp.toPx()
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        // Extract values based on mode
        val rawValues = dataPoints.map { if (showSpeed) it.avgResponseTimeMs / 1000f else it.accuracyPercentage }
        
        val minValue = 0f
        val maxValue = if (showSpeed) {
            val maxRaw = rawValues.maxOrNull() ?: 3.0f
            maxOf(3.0f, (maxRaw * 1.2f)) // standardise to at least 3 seconds max Y
        } else {
            100f // 100% accuracy
        }

        // Draw Y Axis Gridlines (4 levels)
        val gridCount = 4
        for (i in 0..gridCount) {
            val ratio = i.toFloat() / gridCount
            val y = paddingTop + chartHeight * (1f - ratio)
            
            // Draw horizontal dotted gridline
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            // Draw Y label helper Text
            val labelValue = minValue + ratio * (maxValue - minValue)
            val labelText = if (showSpeed) String.format("%.1fs", labelValue) else String.format("%.0f%%", labelValue)
            
            // Render text
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = TextStyle(
                    color = onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 10.sp
                ),
                topLeft = Offset(4.dp.toPx(), y - 7.dp.toPx()) // shift left and center vertically
            )
        }

        // Create line points coordinates
        val points = mutableListOf<Offset>()
        dataPoints.forEachIndexed { idx, _ ->
            val value = rawValues[idx]
            
            // Calculate horizontal offset
            val x = if (dataPoints.size == 1) {
                paddingLeft + chartWidth / 2f
            } else {
                paddingLeft + idx.toFloat() / (dataPoints.size - 1) * chartWidth
            }
            
            // Calculate vertical offset
            val valRatio = if (maxValue == minValue) 1f else (value - minValue) / (maxValue - minValue)
            val y = paddingTop + chartHeight * (1f - valRatio.coerceIn(0f, 1f))
            
            points.add(Offset(x, y))
        }

        // Draw Area Fill (feathers down with gradient)
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                if (points.size == 1) {
                    moveTo(paddingLeft, paddingTop + chartHeight)
                    lineTo(points[0].x, points[0].y)
                    lineTo(width - paddingRight, paddingTop + chartHeight)
                } else {
                    moveTo(points[0].x, paddingTop + chartHeight)
                    for (pt in points) {
                        lineTo(pt.x, pt.y)
                    }
                    lineTo(points.last().x, paddingTop + chartHeight)
                }
                close()
            }
            
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.35f),
                    primaryColor.copy(alpha = 0.00f)
                ),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
            
            drawPath(path = fillPath, brush = gradientBrush)
        }

        // Draw Line
        if (points.size > 1) {
            val linePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else if (points.size == 1) {
            // If just 1 point, draw a simple dot
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = points[0]
            )
        }

        // Draw Dots and Data Point Highlights
        points.forEachIndexed { idx, pt ->
            // Outer glowing ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.4f),
                radius = 6.dp.toPx(),
                center = pt
            )
            // Inner solid dot
            drawCircle(
                color = primaryColor,
                radius = 3.5f.dp.toPx(),
                center = pt
            )
            
            // Render X values (dates) at coordinate bottom
            val xLabel = dataPoints[idx].dateString
            val measuredX = textMeasurer.measure(xLabel)
            
            drawText(
                textMeasurer = textMeasurer,
                text = xLabel,
                style = TextStyle(
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                ),
                topLeft = Offset(pt.x - measuredX.size.width / 2f, paddingTop + chartHeight + 6.dp.toPx())
            )
        }
    }
}
