package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DayStudyMetric
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale
import kotlin.math.max

@Composable
fun StudyHoursBarChart(
    metrics: List<DayStudyMetric>,
    barColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    targetHours: Float? = null,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty() || metrics.all { it.studyHours <= 0f }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Complete a study session to plot your study hours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxMetricHours = metrics.maxOfOrNull { it.studyHours } ?: 0f
    val maxScale = max(max(maxMetricHours, targetHours ?: 0f), 1f) * 1.15f
    val gridColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val chartHeight = size.height - 10.dp.toPx()
            val barCount = metrics.size
            val slotWidth = size.width / barCount.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.52f).coerceIn(10.dp.toPx(), 34.dp.toPx())

            // Horizontal reference lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = chartHeight * (1f - fraction)
                drawLine(
                    color = gridColor.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Optional daily target line
            if (targetHours != null && targetHours > 0f && targetHours <= maxScale) {
                val targetY = chartHeight * (1f - (targetHours / maxScale))
                drawLine(
                    color = EmeraldSuccess.copy(alpha = 0.7f),
                    start = Offset(0f, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            metrics.forEachIndexed { index, item ->
                val ratio = (item.studyHours / maxScale).coerceIn(0f, 1f)
                val barHeight = (chartHeight * ratio).coerceAtLeast(if (item.studyHours > 0f) 4.dp.toPx() else 0f)
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = chartHeight - barHeight

                if (barHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, secondaryColor)
                        ),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            metrics.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (item.studyHours > 0f) {
                            String.format(Locale.US, "%.1fh", item.studyHours)
                        } else "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.studyHours > 0f) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
fun TopicStatusDonutChart(
    completed: Int,
    inProgress: Int,
    notStarted: Int,
    modifier: Modifier = Modifier
) {
    val total = completed + inProgress + notStarted
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val notStartedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier.size(116.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(116.dp)) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                if (total <= 0) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                } else {
                    val completedSweep = (completed.toFloat() / total) * 360f
                    val inProgressSweep = (inProgress.toFloat() / total) * 360f
                    val notStartedSweep = (notStarted.toFloat() / total) * 360f

                    var startAngle = -90f
                    if (completedSweep > 0f) {
                        drawArc(
                            color = EmeraldSuccess,
                            startAngle = startAngle,
                            sweepAngle = completedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += completedSweep
                    }
                    if (inProgressSweep > 0f) {
                        drawArc(
                            color = AmberWarning,
                            startAngle = startAngle,
                            sweepAngle = inProgressSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += inProgressSweep
                    }
                    if (notStartedSweep > 0f) {
                        drawArc(
                            color = notStartedColor,
                            startAngle = startAngle,
                            sweepAngle = notStartedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pct = if (total > 0) (completed * 100f) / total else 0f
                Text(
                    text = String.format(Locale.US, "%.0f%%", pct),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$completed/$total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DonutLegendRow("Completed", completed, EmeraldSuccess)
            DonutLegendRow("In Progress", inProgress, AmberWarning)
            DonutLegendRow("Not Started", notStarted, notStartedColor)
        }
    }
}

@Composable
private fun DonutLegendRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TrendLineChart(
    points: List<Pair<String, Float>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    emptyMessage: String,
    valueSuffix: String = "%",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val gridColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val topPad = 12.dp.toPx()
            val bottomPad = 12.dp.toPx()
            val sidePad = 14.dp.toPx()
            val usableH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val usableW = (size.width - sidePad * 2).coerceAtLeast(1f)

            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = topPad + usableH * (1f - frac)
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val coords = points.mapIndexed { idx, (_, value) ->
                val x = if (points.size == 1) {
                    size.width / 2f
                } else {
                    sidePad + (idx.toFloat() / (points.size - 1)) * usableW
                }
                val normalized = (value.coerceIn(0f, 100f) / 100f)
                val y = topPad + usableH * (1f - normalized)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(coords.first().x, topPad + usableH)
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, topPad + usableH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))
                    )
                )

                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 2.3.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val first = points.first()
            val last = points.last()
            Text(
                text = "${first.first} (${String.format(Locale.US, "%.0f", first.second)}$valueSuffix)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (points.size > 1) {
                Text(
                    text = "Latest: ${last.first} (${String.format(Locale.US, "%.1f", last.second)}$valueSuffix)",
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ConsistencyHeatmapGrid(
    days: List<DayStudyMetric>,
    dailyTargetMinutes: Int,
    modifier: Modifier = Modifier
) {
    // 12 columns (weeks) x 7 rows (days)
    val weeks = days.chunked(7)
    val baseSurface = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val primary = MaterialTheme.colorScheme.primary
    val target = dailyTargetMinutes.coerceAtLeast(30)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weeks.forEach { weekDays ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekDays.forEach { metric ->
                        val intensityColor = when {
                            metric.studyMinutes == 0 && metric.tasksCompleted == 0 -> baseSurface
                            metric.studyMinutes >= target -> EmeraldSuccess
                            metric.studyMinutes >= target / 2 -> primary
                            else -> primary.copy(alpha = 0.45f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(intensityColor)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last 12 Weeks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    baseSurface,
                    primary.copy(alpha = 0.45f),
                    primary,
                    EmeraldSuccess
                ).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(swatch)
                    )
                }
                Text(
                    text = "Goal+",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
