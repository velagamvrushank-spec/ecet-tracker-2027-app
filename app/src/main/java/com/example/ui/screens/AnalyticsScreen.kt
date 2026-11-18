package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.ConsistencyHeatmapGrid
import com.example.ui.components.EcetCard
import com.example.ui.components.StudyHoursBarChart
import com.example.ui.components.TopicStatusDonutChart
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.EcetUiState
import java.util.Locale

@Composable
fun AnalyticsScreen(
    uiState: EcetUiState,
    modifier: Modifier = Modifier
) {
    val analytics = uiState.analytics
    val overview = uiState.overview
    val notStartedCount = overview.subjectSummaries.sumOf { it.notStartedTopics }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Analytics & Reports",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Calculated strictly from your saved study sessions, topics, MCQs, and mocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Study Streak Counter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = AmberWarning.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Current streak",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Current Streak", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${analytics.currentStreakDays} days",
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Best Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${analytics.bestStreakDays} days",
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${analytics.activeDaysLast30}/30 active days",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Daily Consistency Tracker
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Daily Consistency Tracker", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Days with logged study sessions or completed tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ConsistencyHeatmapGrid(
                        days = analytics.heatmapDays,
                        dailyTargetMinutes = uiState.preferences.dailyStudyTargetMinutes
                    )
                }
            }

            // Weekly Study Hours Chart
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Weekly Study Hours", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Last 7 days · Dashed line shows daily goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StudyHoursBarChart(
                        metrics = analytics.weeklyStudyDays,
                        barColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        targetHours = uiState.preferences.dailyStudyTargetMinutes / 60f
                    )
                }
            }

            // Monthly Study Hours Chart
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Monthly Study Hours", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "4-week rolling breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StudyHoursBarChart(
                        metrics = analytics.monthlyStudyBuckets,
                        barColor = MaterialTheme.colorScheme.secondary,
                        secondaryColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Topic Completion Graph (Donut)
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Topic Completion Breakdown", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    TopicStatusDonutChart(
                        completed = overview.completedTopics,
                        inProgress = overview.inProgressTopics,
                        notStarted = notStartedCount
                    )
                }
            }

            // Subject-wise Completion Chart
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Subject-Wise Completion", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        analytics.subjectCompletions.forEach { summary ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = summary.subject.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${summary.completedTopics}/${summary.totalTopics} (${String.format(Locale.US, "%.0f%%", summary.completionPercent)})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = summary.subject.accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                LinearProgressIndicator(
                                    progress = { (summary.completionPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = summary.subject.accentColor,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // MCQ Accuracy Graph
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("MCQ Accuracy Trend", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Accuracy across your recent practice sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TrendLineChart(
                        points = analytics.mcqAccuracySeries,
                        lineColor = EmeraldSuccess,
                        emptyMessage = "Log MCQ practice sessions to plot your accuracy graph."
                    )
                }
            }

            // Mock Test Performance Graph
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Mock Test Performance Graph", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Percentage score across recorded mock exams",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TrendLineChart(
                        points = analytics.mockScoreSeries,
                        lineColor = MaterialTheme.colorScheme.primary,
                        emptyMessage = "Record mock tests to visualize your score progression."
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
