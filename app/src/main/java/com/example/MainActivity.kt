package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.AppDatabase
import com.example.data.HealthRepository
import com.example.ui.HealthViewModel
import com.example.ui.HealthViewModelFactory
import com.example.ui.NavRoute
import com.example.ui.components.BiometricLockScreen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getDatabase(this)
        val repository = HealthRepository(database.healthDao())
        val viewModelFactory = HealthViewModelFactory(repository)

        setContent {
            HealthTimelineTheme {
                val viewModel: HealthViewModel = viewModel(factory = viewModelFactory)
                val patient by viewModel.patient.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val language = patient?.language ?: "en"
                val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

                val biometricAuthManager = remember { BiometricAuthManager(this) }
                var isLocked by remember {
                    mutableStateOf(biometricAuthManager.isBiometricLockEnabled && !BiometricAuthManager.isUnlockedInSession)
                }

                if (isLocked) {
                    BiometricLockScreen(
                        biometricAuthManager = biometricAuthManager,
                        onUnlocked = { isLocked = false },
                        t = t
                    )
                } else {
                    val showBottomBar = patient != null && currentDestination?.let { dest ->
                        listOf(NavRoute.Home, NavRoute.Timeline, NavRoute.Medications, NavRoute.Summary).any { route ->
                            dest.hierarchy.any { it.hasRoute(route::class) }
                        }
                    } ?: false

                    Scaffold(
                        containerColor = Canvas,
                        bottomBar = {
                            if (showBottomBar) {
                                FloatingTabNavigation(
                                    currentDestination = currentDestination,
                                    navController = navController,
                                    t = t
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (patient == null) NavRoute.Onboarding else NavRoute.Home,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable<NavRoute.Onboarding> {
                                OnboardingScreen(
                                    onFinish = {
                                        navController.navigate(NavRoute.Home) {
                                            popUpTo(NavRoute.Onboarding) { inclusive = true }
                                        }
                                    },
                                    viewModel = viewModel
                                )
                            }
                            composable<NavRoute.Home> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToTimeline = {
                                        navController.navigate(NavRoute.Timeline)
                                    },
                                    onNavigateToProfile = {
                                        navController.navigate(NavRoute.Profile)
                                    },
                                    onNavigateToCondition = { conditionId ->
                                        navController.navigate(NavRoute.ConditionDetail(conditionId))
                                    }
                                )
                            }
                            composable<NavRoute.Capture> {
                                CaptureScreen(
                                    onDismiss = { navController.popBackStack() },
                                    onNavigateToExtract = {
                                        navController.navigate(NavRoute.Extraction("doc-new"))
                                    },
                                    onNavigateToManual = {
                                        navController.navigate(NavRoute.Timeline)
                                    },
                                    viewModel = viewModel
                                )
                            }
                            composable<NavRoute.Extraction> {
                                ExtractionScreen(
                                    onBack = { navController.popBackStack() },
                                    onConfirm = {
                                        navController.navigate(NavRoute.Summary)
                                    },
                                    viewModel = viewModel
                                )
                            }
                            composable<NavRoute.ConditionDetail> { backStackEntry ->
                                val route: NavRoute.ConditionDetail = backStackEntry.toRoute()
                                ConditionDetailScreen(
                                    conditionId = route.conditionId,
                                    onBack = { navController.popBackStack() },
                                    onViewReport = { docId ->
                                        navController.navigate(NavRoute.ReportViewer(docId))
                                    },
                                    viewModel = viewModel
                                )
                            }
                            composable<NavRoute.ReportViewer> { backStackEntry ->
                                val route: NavRoute.ReportViewer = backStackEntry.toRoute()
                                ReportViewerScreen(
                                    documentId = route.documentId,
                                    onBack = { navController.popBackStack() },
                                    viewModel = viewModel
                                )
                            }
                            composable<NavRoute.Timeline> {
                                TimelineScreen(
                                    viewModel = viewModel,
                                    onNavigateToCapture = {
                                        navController.navigate(NavRoute.Capture)
                                    }
                                )
                            }
                            composable<NavRoute.Medications> {
                                MedicationsScreen(viewModel = viewModel)
                            }
                            composable<NavRoute.Summary> {
                                SummaryScreen(
                                    onBack = {
                                        navController.navigate(NavRoute.Home) {
                                            popUpTo(NavRoute.Home) { inclusive = true }
                                        }
                                    },
                                    viewModel = viewModel
                                )
                            }
                            composable<NavRoute.Profile> {
                                ProfileScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToOnboarding = {
                                        navController.navigate(NavRoute.Onboarding)
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingTabNavigation(
    currentDestination: androidx.navigation.NavDestination?,
    navController: androidx.navigation.NavController,
    t: (String, String) -> String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(12.dp, CircleShape, spotColor = Ink.copy(alpha = 0.12f)),
            shape = CircleShape,
            color = Surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home (⌂)
                NavIconItem(
                    iconText = "⌂",
                    label = t("Home", "হোম"),
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(NavRoute.Home::class) } == true,
                    onClick = {
                        navController.navigate(NavRoute.Home) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 2. Timeline (◷)
                NavIconItem(
                    iconText = "◷",
                    label = t("Timeline", "টাইমলাইন"),
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(NavRoute.Timeline::class) } == true,
                    onClick = {
                        navController.navigate(NavRoute.Timeline) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 3. Scan Button (elevated circle button: 46x46dp, Primary, white camera icon)
                Box(
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Primary)
                        .clickable { navController.navigate(NavRoute.Capture) }
                        .shadow(8.dp, CircleShape, spotColor = Primary.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                // 4. Meds (💊)
                NavIconItem(
                    iconText = "💊",
                    label = t("Meds", "ওষুধ"),
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(NavRoute.Medications::class) } == true,
                    onClick = {
                        navController.navigate(NavRoute.Medications) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 5. Summary (📋)
                NavIconItem(
                    iconText = "📋",
                    label = t("Summary", "সারসংক্ষেপ"),
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(NavRoute.Summary::class) } == true,
                    onClick = {
                        navController.navigate(NavRoute.Summary) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NavIconItem(
    iconText: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = iconText,
            fontSize = 18.sp,
            color = if (selected) Primary else InkSoft,
            lineHeight = 18.sp
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Primary else InkSoft,
            fontFamily = BodyFontFamily,
            maxLines = 1
        )
    }
}
