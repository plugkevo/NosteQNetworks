package com.kevann.nosteq

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(context: Context? = null) {
    val navController = rememberNavController()
    val localContext = context ?: LocalContext.current
    val preferencesManager = remember { PreferencesManager(localContext) }
    val username = preferencesManager.getUsername() ?: ""

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isForceUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val remoteVersion = VersionChecker.getRemoteVersion()
                if (remoteVersion != null) {
                    val installedVersion = VersionChecker.getInstalledVersion(localContext)

                    val updateAvailable = VersionChecker.isUpdateNeeded(installedVersion, remoteVersion.currentVersion)
                    val forceUpdateRequired = VersionChecker.isUpdateNeeded(installedVersion, remoteVersion.minRequiredVersion)

                    // Only show dialog if update is actually available
                    if (updateAvailable) {
                        updateInfo = remoteVersion
                        isForceUpdate = forceUpdateRequired
                        showUpdateDialog = true
                    }
                }
            } catch (e: Exception) {
                // silently fail for production
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NosteQ Networks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") { DashboardScreen(navController) }
            composable("packages") { PackagesScreen() }
            composable("account") { AccountScreen() }
            composable("billing") { BillingScreen() }
            composable("support") { SupportScreen() }
            composable("router") { RouterScreen(username = username) }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdatePromptDialog(
            updateInfo = updateInfo!!,
            isForceUpdate = isForceUpdate,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            onUpdateClick = {
                isDownloading = true
                scope.launch {
                    try {
                        ApkDownloader.downloadApk(localContext, updateInfo!!.apkDownloadUrl)
                            .collect { progress ->
                                downloadProgress = progress
                                if (progress.isComplete) {
                                    isDownloading = false
                                }
                            }
                    } catch (e: Exception) {
                        isDownloading = false
                        // silently fail for production
                    }
                }
            },
            onSkipClick = {
                showUpdateDialog = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem("dashboard", Icons.Filled.Home, "Home"),
    BottomNavItem("packages", Icons.Filled.List, "Packages"),
    BottomNavItem("account", Icons.Filled.Person, "Account"),
    BottomNavItem("billing", Icons.Filled.AccountBox, "Billing"),
    BottomNavItem("support", Icons.Filled.Info, "Support")
)
