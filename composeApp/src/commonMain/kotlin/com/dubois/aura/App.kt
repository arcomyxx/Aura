package com.dubois.aura

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.dubois.aura.navigation.Screen
import com.dubois.aura.ui.HomeScreen
import com.dubois.aura.ui.LoginScreen
import com.dubois.aura.ui.RegisterScreen
import com.dubois.aura.ui.main.MainScreen
import com.dubois.aura.ui.theme.AuraTheme
import com.dubois.aura.viewmodel.MainViewModel
import com.dubois.aura.viewmodel.UserViewModel

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    val userViewModel = remember { UserViewModel() }
    val mainViewModel = remember { MainViewModel() }

    // Si une session Firebase est déjà ouverte, on va directement à l'écran
    // principal (Firebase persiste la connexion sur l'appareil).
    val startDestination = remember {
        if (userViewModel.isLoggedIn()) Screen.Main.route else Screen.Home.route
    }

    AuraTheme {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            // ── Écran d'accueil ───────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }

            // ── Connexion ─────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = userViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Inscription ───────────────────────────────────────
            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = userViewModel,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Expérience principale (Profil / Amis / Classement) ─
            composable(Screen.Main.route) {
                MainScreen(
                    viewModel = mainViewModel,
                    onLogout = {
                        userViewModel.logout()
                        navController.navigate(Screen.Home.route) {
                            // Vide toute la pile pour ne pas pouvoir revenir sur Main.
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
