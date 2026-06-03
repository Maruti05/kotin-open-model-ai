package com.vedica.labs.ind.app.chat.openmodels.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.NeonCyan
import kotlin.math.exp

enum class CurveType { TEMPERATURE, TOP_P, TOP_K, MAX_TOKENS }

@Composable
fun ParameterCurve(
    curveType: CurveType,
    value: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val midY = canvasHeight / 2f

        when (curveType) {
            CurveType.TEMPERATURE -> {
                // Gaussian bell curve - higher temp = wider/broader
                val sigma = 0.3f + value * 0.4f
                val path = Path()
                path.moveTo(0f, canvasHeight)

                for (x in 0 until canvasWidth.toInt()) {
                    val normalizedX = (x / canvasWidth) * 6f - 3f
                    val y = exp(-(normalizedX * normalizedX) / (2 * sigma * sigma))
                    val canvasY = canvasHeight - (y * canvasHeight * 0.9f)
                    if (x == 0) path.moveTo(x.toFloat(), canvasY)
                    else path.lineTo(x.toFloat(), canvasY)
                }

                drawPath(path, NeonCyan, style = Stroke(width = 2.dp.toPx()))
            }
            CurveType.TOP_P -> {
                // Cumulative probability with cutoff line at value position
                val cutoffX = value * canvasWidth
                drawLine(
                    NeonCyan,
                    Offset(cutoffX, 0f),
                    Offset(cutoffX, canvasHeight),
                    strokeWidth = 2.dp.toPx()
                )
                // S-curve
                val path = Path()
                for (x in 0 until canvasWidth.toInt()) {
                    val normX = x / canvasWidth
                    val y = 1f / (1f + exp(-10f * (normX - 0.5f)))
                    val canvasY = canvasHeight - (y * canvasHeight * 0.9f)
                    if (x == 0) path.moveTo(x.toFloat(), canvasY)
                    else path.lineTo(x.toFloat(), canvasY)
                }
                drawPath(path, NeonCyan.copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx()))
            }
            CurveType.TOP_K -> {
                // Histogram bars - selected bar highlighted
                val bars = 10
                val barWidth = canvasWidth / bars
                val selectedBar = ((value / 100f) * bars).toInt().coerceIn(0, bars - 1)

                for (i in 0 until bars) {
                    val barHeight = (0.3f + Math.random().toFloat() * 0.5f) * canvasHeight
                    val x = i * barWidth + 2f
                    val w = barWidth - 4f
                    val color = if (i <= selectedBar) NeonCyan else NeonCyan.copy(alpha = 0.3f)
                    drawRect(
                        color = color,
                        topLeft = Offset(x, canvasHeight - barHeight),
                        size = androidx.compose.ui.geometry.Size(w, barHeight)
                    )
                }
            }
            CurveType.MAX_TOKENS -> {
                // Wave with current position marker
                val markerX = (value / 2048f) * canvasWidth
                val path = Path()
                for (x in 0 until canvasWidth.toInt()) {
                    val freq = 4f * Math.PI.toFloat() * x / canvasWidth
                    val wave = (midY - 16.dp.toPx()) + (16.dp.toPx() * kotlin.math.sin(freq))
                    if (x == 0) path.moveTo(x.toFloat(), wave)
                    else path.lineTo(x.toFloat(), wave)
                }
                drawPath(path, NeonCyan.copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx()))

                // Vertical marker
                drawLine(
                    NeonCyan,
                    Offset(markerX, 0f),
                    Offset(markerX, canvasHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
