package com.dubois.aura

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource

import aura.composeapp.generated.resources.Res
import aura.composeapp.generated.resources.compose_multiplatform
import com.dubois.aura.navigation.Screen
import com.dubois.aura.ui.HomeScreen
import com.dubois.aura.ui.LoginScreen
import com.dubois.aura.ui.ProfileScreen
import com.dubois.aura.ui.RegisterScreen
import com.dubois.aura.viewmodel.UserViewModel

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    val viewModel = remember { UserViewModel() }

    MaterialTheme {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {

            // ── Écran d'accueil original ──────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }

            // ── Connexion ─────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Inscription ───────────────────────────────────────
            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Profil ────────────────────────────────────────────
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
