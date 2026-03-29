package com.example.mathpuzzlegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mathpuzzlegame.ui.App
import com.example.mathpuzzlegame.ui.theme.MathPuzzleGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MathPuzzleGameTheme {
                App()
            }
        }
    }
}
