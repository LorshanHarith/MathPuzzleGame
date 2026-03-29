package com.example.mathpuzzlegame.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.mathpuzzlegame.data.AppScreen
import com.example.mathpuzzlegame.data.GameMode
import com.example.mathpuzzlegame.data.SessionConfig
import com.example.mathpuzzlegame.data.sessionConfigSaver
import com.example.mathpuzzlegame.ui.screens.GameScreen
import com.example.mathpuzzlegame.ui.screens.MainMenuScreen
import kotlin.random.Random

@Composable
fun App() {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.MENU.name) }
    var sessionConfig by rememberSaveable(stateSaver = sessionConfigSaver) {
        mutableStateOf(
            SessionConfig(
                mode = GameMode.NORMAL,
                sessionSeed = 1,
                requestedEquationCount = 8
            )
        )
    }

    when (AppScreen.valueOf(currentScreen)) {
        AppScreen.MENU -> MainMenuScreen(
            onNewGame = {
                val randomEquationCount = listOf(4, 8, 12, 16).random()
                sessionConfig = SessionConfig(
                    mode = GameMode.NORMAL,
                    sessionSeed = Random.nextInt(),
                    requestedEquationCount = randomEquationCount
                )
                currentScreen = AppScreen.GAME.name
            },
            onAdvanced = {
                val randomEquationCount = listOf(8, 12, 16).random()
                sessionConfig = SessionConfig(
                    mode = GameMode.ADVANCED,
                    sessionSeed = Random.nextInt(),
                    requestedEquationCount = randomEquationCount
                )
                currentScreen = AppScreen.GAME.name
            }
        )

        AppScreen.GAME -> GameScreen(
            sessionConfig = sessionConfig,
            onBackToMenu = {
                currentScreen = AppScreen.MENU.name
            }
        )
    }
}
