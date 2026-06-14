package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Clef
import com.example.data.Note

@Composable
fun StaffCanvas(
    note: Note,
    modifier: Modifier = Modifier,
    staffColor: Color = if (isSystemInDarkTheme()) Color.White else Color.Black,
    noteColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val systemWarmColor = if (isDark) Color(0xFFE2E2E2) else Color(0xFF2E2E2E)
    
    // Dimension presets
    val lineSpacing = 16.dp
    val lineSpacingPx = with(density) { lineSpacing.toPx() }
    val staffHeightPx = lineSpacingPx * 4
    
    // Load clef vectors
    val treblePainter = rememberVectorPainter(ImageVector.vectorResource(id = R.drawable.ic_treble_clef))
    val bassPainter = rememberVectorPainter(ImageVector.vectorResource(id = R.drawable.ic_bass_clef))

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // Horizontal boundaries for staff lines
        val startX = 24.dp.toPx()
        val endX = width - 24.dp.toPx()
        
        // 1. Draw 5 Staff Lines
        // Offsets relative to center: -2, -1, 0, 1, 2
        for (i in -2..2) {
            val y = centerY + i * lineSpacingPx
            drawLine(
                color = staffColor.copy(alpha = 0.4f),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // 2. Draw Clef
        val clefWidth = 44.dp.toPx()
        val clefHeight = 100.dp.toPx()
        val clefX = startX + 16.dp.toPx()
        
        // Align clefs vertically to staff centers
        val clefY = if (note.clef == Clef.TREBLE) {
            centerY - clefHeight / 2f + 4.dp.toPx() // G-Clef slightly offset
        } else {
            centerY - clefHeight / 2f - 4.dp.toPx() // F-Clef slightly higher
        }
        
        val painter = if (note.clef == Clef.TREBLE) treblePainter else bassPainter
        
        // Translate and draw clef
        val clefSize = Size(clefWidth, clefHeight)
        drawContext.canvas.save()
        drawContext.transform.translate(clefX, clefY)
        with(painter) {
            draw(size = clefSize, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(systemWarmColor))
        }
        drawContext.canvas.restore()
        
        // 3. Draw Note Head & Stem
        // Calculate note head position based on staffStep
        // Note spacing works as: staffStep = 0 is B4 for Treble, D3 for Bass (middle line).
        // Positive steps go up, negative go down.
        // Each step is exactly half the line spacing.
        val noteY = centerY - (note.staffStep * (lineSpacingPx / 2f))
        val noteX = width * 0.58f // Placed distinctively after the clef
        
        val noteWidth = 22.dp.toPx()
        val noteHeight = 15.dp.toPx()
        
        // Draw Ledger Lines (if note is beyond standard staff bounds)
        // Staff bounds are from step -4 (Line 1) to step 4 (Line 5)
        // Ledger lines occur on even steps:
        // Below staff: step <= -6 leads to ledger lines at -6, -8, etc.
        // Above staff: step >= 6 leads to ledger lines at 6, 8, etc.
        val ledgerWidth = noteWidth * 1.8f
        if (note.staffStep <= -6) {
            // Draw ledger lines below
            val targetStep = if (note.staffStep % 2 == 0) note.staffStep else note.staffStep + 1
            for (step in -6 downTo targetStep step 2) {
                val ly = centerY - (step * (lineSpacingPx / 2f))
                drawLine(
                    color = staffColor.copy(alpha = 0.8f),
                    start = Offset(noteX - ledgerWidth / 2f, ly),
                    end = Offset(noteX + ledgerWidth / 2f, ly),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        } else if (note.staffStep >= 6) {
            // Draw ledger lines above
            val targetStep = if (note.staffStep % 2 == 0) note.staffStep else note.staffStep - 1
            for (step in 6..targetStep step 2) {
                val ly = centerY - (step * (lineSpacingPx / 2f))
                drawLine(
                    color = staffColor.copy(alpha = 0.8f),
                    start = Offset(noteX - ledgerWidth / 2f, ly),
                    end = Offset(noteX + ledgerWidth / 2f, ly),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        // Draw Note Stem
        // Stems go DOWN on the left side when note is on middle line or above (staffStep >= 0)
        // Stems go UP on the right side when note is below middle line (staffStep < 0)
        val stemLength = lineSpacingPx * 3.2f
        val stemThickness = 1.8f.dp.toPx()
        
        if (note.staffStep >= 0) {
            // Down stem (left side of head)
            val stemStartX = noteX - noteWidth / 2f + (stemThickness / 2f)
            val stemStartY = noteY
            val stemEndY = noteY + stemLength
            drawLine(
                color = noteColor,
                start = Offset(stemStartX, stemStartY),
                end = Offset(stemStartX, stemEndY),
                strokeWidth = stemThickness,
                cap = StrokeCap.Square
            )
        } else {
            // Up stem (right side of head)
            val stemStartX = noteX + noteWidth / 2f - (stemThickness / 2f)
            val stemStartY = noteY
            val stemEndY = noteY - stemLength
            drawLine(
                color = noteColor,
                start = Offset(stemStartX, stemStartY),
                end = Offset(stemStartX, stemEndY),
                strokeWidth = stemThickness,
                cap = StrokeCap.Round
            )
        }
        
        // Draw Tilted Note Head (for that classic quarter note oval shape)
        rotate(degrees = -25f, pivot = Offset(noteX, noteY)) {
            drawOval(
                color = noteColor,
                topLeft = Offset(noteX - noteWidth / 2f, noteY - noteHeight / 2f),
                size = Size(noteWidth, noteHeight)
            )
        }
    }
}
