package com.kevannTechnologies.nosteqCustomers

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(context: Context? = null) {
    val navController = rememberNavController()
    val localContext = context ?: LocalContext.current
    val preferencesManager = remember { PreferencesManager(localContext) }
    val username = preferencesManager.getUsername() ?: ""

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
