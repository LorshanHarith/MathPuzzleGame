package com.example.mathpuzzlegame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onNewGame: () -> Unit,
    onAdvanced: () -> Unit
) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cross Math Puzzle",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New Game")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAdvanced,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Advanced Level")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("About")
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            },
            title = {
                Text("About")
            },
            text = {
                Text(
                    "Name: YOUR NAME HERE\n" +
                        "Student ID: YOUR STUDENT ID HERE\n\n" +
                        "I confirm that I understand what plagiarism is and have read and " +
                        "understood the section on Assessment Offences in the Essential " +
                        "Information for Students. The work that I have submitted is entirely " +
                        "my own. Any work from other authors is duly referenced and acknowledged."
                )
            }
        )
    }
}
