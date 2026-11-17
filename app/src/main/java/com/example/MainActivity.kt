package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.StudySubTab
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.theme.EcetTrackerTheme
import com.example.ui.viewmodel.EcetViewModel

enum class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    SYLLABUS("syllabus", "Syllabus", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    STUDY("study", "Study", Icons.Filled.Timer, Icons.Outlined.Timer),
    ANALYTICS("analytics", "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EcetViewModel = viewModel()
            EcetTrackerApp(viewModel = viewModel)
        }
    }
}

@Composable
fun EcetTrackerApp(viewModel: EcetViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timerState by viewModel.timerUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    EcetTrackerTheme(themeMode = uiState.preferences.themeMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavDestination.HOME.route

        var studySubTab by rememberSaveable { mutableStateOf(StudySubTab.TIMER) }
        var pendingAddTopic by rememberSaveable { mutableStateOf(false) }
        var pendingAddTask by rememberSaveable { mutableStateOf(false) }
        var pendingAddMcq by rememberSaveable { mutableStateOf(false) }
        var pendingAddMock by rememberSaveable { mutableStateOf(false) }

        fun navigateToTab(destination: BottomNavDestination) {
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            BottomNavDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            BottomNavDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = BottomNavDestination.HOME.route,
                        enterTransition = { fadeIn(animationSpec = tween(180)) },
                        exitTransition = { fadeOut(animationSpec = tween(150)) },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(BottomNavDestination.HOME.route) {
                            HomeScreen(
                                uiState = uiState,
                                timerState = timerState,
                                onSetExamDate = { viewModel.updateExamDate(it) },
                                onQuickStartTimer = {
                                    studySubTab = StudySubTab.TIMER
                                    navigateToTab(BottomNavDestination.STUDY)
                                },
                                onQuickAddTopic = {
                                    pendingAddTopic = true
                                    navigateToTab(BottomNavDestination.SYLLABUS)
                                },
                                onQuickAddTask = {
                                    studySubTab = StudySubTab.TASKS
                                    pendingAddTask = true
                                    navigateToTab(BottomNavDestination.STUDY)
                                },
                                onQuickRecordMcq = {
                                    studySubTab = StudySubTab.MCQ
                                    pendingAddMcq = true
                                    navigateToTab(BottomNavDestination.STUDY)
                                },
                                onQuickRecordMock = {
                                    studySubTab = StudySubTab.MOCKS
                                    pendingAddMock = true
                                    navigateToTab(BottomNavDestination.STUDY)
                                },
                                onToggleTask = { viewModel.toggleDailyTask(it) },
                                onOpenSyllabus = { navigateToTab(BottomNavDestination.SYLLABUS) },
                                onOpenStudyTab = { tab ->
                                    studySubTab = tab
                                    navigateToTab(BottomNavDestination.STUDY)
                                }
                            )
                        }

                        composable(BottomNavDestination.SYLLABUS.route) {
                            SyllabusScreen(
                                uiState = uiState,
                                shouldOpenAddTopic = pendingAddTopic,
                                onConsumeOpenAddTopic = { pendingAddTopic = false },
                                onSaveTopic = { id, subj, title, status, priority, notes, targetDay ->
                                    viewModel.saveTopic(id, subj, title, status, priority, notes, targetDay)
                                },
                                onUpdateStatus = { topic, status ->
                                    viewModel.updateTopicStatus(topic, status)
                                },
                                onDeleteTopic = { viewModel.deleteTopic(it) },
                                onImportSyllabus = { raw, subject ->
                                    viewModel.importSyllabus(raw, subject)
                                }
                            )
                        }

                        composable(BottomNavDestination.STUDY.route) {
                            StudyHubScreen(
                                selectedSubTab = studySubTab,
                                onSelectSubTab = { studySubTab = it },
                                uiState = uiState,
                                timerState = timerState,
                                shouldOpenAddTask = pendingAddTask,
                                onConsumeOpenAddTask = { pendingAddTask = false },
                                shouldOpenAddMcq = pendingAddMcq,
                                onConsumeOpenAddMcq = { pendingAddMcq = false },
                                shouldOpenAddMock = pendingAddMock,
                                onConsumeOpenAddMock = { pendingAddMock = false },
                                onStartOrResumeTimer = { viewModel.startOrResumeTimer() },
                                onPauseTimer = { viewModel.pauseTimer() },
                                onResetTimer = { viewModel.resetTimer() },
                                onFinishAndSaveSession = { viewModel.finishAndSaveSession() },
                                onSetTimerMode = { viewModel.setTimerMode(it) },
                                onSetPomodoroPhase = { viewModel.setPomodoroPhase(it) },
                                onSelectTimerSubject = { viewModel.selectTimerSubject(it) },
                                onSelectTimerTopic = { viewModel.selectTimerTopic(it) },
                                onUpdatePomodoroDurations = { f, s, l ->
                                    viewModel.updatePomodoroDurations(f, s, l)
                                },
                                onDeleteStudySession = { viewModel.deleteStudySession(it) },
                                onSaveDailyTask = { id, title, subj, prio, dateDay, done ->
                                    viewModel.saveDailyTask(id, title, subj, prio, dateDay, done)
                                },
                                onToggleDailyTask = { viewModel.toggleDailyTask(it) },
                                onDeleteDailyTask = { viewModel.deleteDailyTask(it) },
                                onAddMcqSession = { subj, topicName, att, cor, inc, unatt, mins, dateDay ->
                                    viewModel.addMcqSession(subj, topicName, att, cor, inc, unatt, mins, dateDay)
                                },
                                onDeleteMcqSession = { viewModel.deleteMcqSession(it) },
                                onSaveMockTest = { id, title, dateDay, m, mMax, p, pMax, c, cMax, e, eMax, att, cor, inc, notes ->
                                    viewModel.saveMockTest(id, title, dateDay, m, mMax, p, pMax, c, cMax, e, eMax, att, cor, inc, notes)
                                },
                                onDeleteMockTest = { viewModel.deleteMockTest(it) }
                            )
                        }

                        composable(BottomNavDestination.ANALYTICS.route) {
                            AnalyticsScreen(uiState = uiState)
                        }

                        composable(BottomNavDestination.PROFILE.route) {
                            ProfileScreen(
                                uiState = uiState,
                                onUpdateProfile = { name, branch, exam, targetMins ->
                                    viewModel.updateProfile(name, branch, exam, targetMins)
                                },
                                onUpdateExamDate = { viewModel.updateExamDate(it) },
                                onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                                onUpdateNotifications = { rem, pomo, hr, min ->
                                    viewModel.updateNotificationPreferences(rem, pomo, hr, min)
                                },
                                onExportBackupJson = { viewModel.exportBackupJson() },
                                onRestoreBackupJson = { viewModel.restoreBackupJson(it) },
                                onResetAllData = { viewModel.resetAllData() },
                                onEmitMessage = { viewModel.emitMessage(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
