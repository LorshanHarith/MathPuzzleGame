package com.example.mathpuzzlegame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mathpuzzlegame.data.CellType
import com.example.mathpuzzlegame.data.EquationState
import com.example.mathpuzzlegame.data.GameMode
import com.example.mathpuzzlegame.data.PuzzleFactory
import com.example.mathpuzzlegame.data.SessionConfig
import com.example.mathpuzzlegame.data.cellListSaver
import com.example.mathpuzzlegame.logic.evaluateEquation
import com.example.mathpuzzlegame.logic.getCellText
import com.example.mathpuzzlegame.ui.components.NumberInputDialog
import com.example.mathpuzzlegame.ui.components.PuzzleCell
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GameScreen(
    sessionConfig: SessionConfig,
    onBackToMenu: () -> Unit
) {
    val sessionKey = remember(sessionConfig) {
        "${sessionConfig.mode.name}_${sessionConfig.sessionSeed}_${sessionConfig.requestedEquationCount}"
    }
    val puzzle by produceState<com.example.mathpuzzlegame.data.PuzzleDefinition?>(
        initialValue = null,
        key1 = sessionKey
    ) {
        /*
         * Puzzle generation can be noticeably heavier in advanced mode because the generator
         * checks whether additional blank cells still leave a unique solution. Running that
         * work on a background dispatcher prevents the Android splash screen from appearing to
         * freeze while Compose waits for the first frame.
         */
        value = withContext(Dispatchers.Default) {
            PuzzleFactory.createPuzzle(
                mode = sessionConfig.mode,
                requestedEquationCount = sessionConfig.requestedEquationCount,
                seed = sessionConfig.sessionSeed
            )
        }
    }

    if (puzzle == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (sessionConfig.mode == GameMode.NORMAL) {
                    "Preparing new game..."
                } else {
                    "Preparing advanced puzzle..."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Please wait a moment.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val activePuzzle = puzzle!!

    var cells by rememberSaveable(sessionKey, stateSaver = cellListSaver) {
        mutableStateOf(activePuzzle.cells)
    }
    var selectedCellIndex by rememberSaveable(sessionKey) { mutableIntStateOf(-1) }
    var showInputDialog by rememberSaveable(sessionKey) { mutableStateOf(false) }
    var timerEnabled by rememberSaveable(sessionKey) { mutableStateOf(false) }
    var timeLeft by rememberSaveable(sessionKey) { mutableIntStateOf(60) }
    var gameOver by rememberSaveable(sessionKey) { mutableStateOf(false) }

    val equationStates = remember(cells, activePuzzle.equations) {
        activePuzzle.equations.map { equation -> evaluateEquation(equation, cells) }
    }
    val score = equationStates.count { it == EquationState.CORRECT }
    val allInputCellsFilled = cells
        .filter { it.type == CellType.INPUT }
        .all { it.inputValue.isNotBlank() }
    val puzzleSolved = allInputCellsFilled && equationStates.all { it == EquationState.CORRECT }
    val inputDialogInitialValue = if (selectedCellIndex in cells.indices) {
        cells[selectedCellIndex].inputValue
    } else {
        ""
    }

    BackHandler(onBack = onBackToMenu)

    LaunchedEffect(timerEnabled, timeLeft, gameOver, puzzleSolved) {
        if (timerEnabled && timeLeft > 0 && !gameOver && !puzzleSolved) {
            delay(1_000)
            timeLeft -= 1
            if (timeLeft == 0) {
                gameOver = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = if (sessionConfig.mode == GameMode.NORMAL) {
                        "New Game"
                    } else {
                        "Advanced Level"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = timerEnabled,
                        onCheckedChange = { checked ->
                            if (!gameOver && !puzzleSolved) {
                                timerEnabled = checked
                                if (!checked) {
                                    timeLeft = 60
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Time: $timeLeft",
                        color = if (timerEnabled) Color(0xFF8A3D00) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

            Text(
            text = "Grid: ${activePuzzle.rows} x ${activePuzzle.cols}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                for (row in 0 until activePuzzle.rows) {
                    Row {
                        for (col in 0 until activePuzzle.cols) {
                            val cell = cells.first { it.row == row && it.col == col }
                            val relatedStates = activePuzzle.equations
                                .filter { equation ->
                                    equation.cells.any { position ->
                                        position.row == row && position.col == col
                                    }
                                }
                                .map { equation -> evaluateEquation(equation, cells) }

                            val backgroundColor = when {
                                cell.type == CellType.BLOCK -> Color(0xFF1F2933)
                                relatedStates.contains(EquationState.WRONG) -> Color(0xFFF8B4B4)
                                relatedStates.contains(EquationState.CORRECT) -> Color(0xFFB8F5C8)
                                cell.type == CellType.FIXED -> Color(0xFFE9EEF2)
                                else -> Color.White
                            }

                            val textColor = if (cell.type == CellType.BLOCK) {
                                Color(0xFF1F2933)
                            } else {
                                Color(0xFF1A1A1A)
                            }

                            val canClick = cell.type == CellType.INPUT && !gameOver && !puzzleSolved

                            PuzzleCell(
                                value = getCellText(cell),
                                backgroundColor = backgroundColor,
                                textColor = textColor,
                                clickable = canClick,
                                onClick = {
                                    selectedCellIndex = cells.indexOf(cell)
                                    showInputDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (puzzleSolved) {
            Text(
                text = "Puzzle completed. Press the Android Back button to return to the menu and start a new game.",
                color = Color(0xFF116329),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (gameOver) {
            Text(
                text = "GAME OVER!",
                color = Color(0xFFB00020),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showInputDialog && selectedCellIndex in cells.indices) {
        NumberInputDialog(
            initialValue = inputDialogInitialValue,
            onDismiss = { showInputDialog = false },
            onConfirm = { enteredValue ->
                val updated = cells.toMutableList()
                val oldCell = updated[selectedCellIndex]
                updated[selectedCellIndex] = oldCell.copy(inputValue = enteredValue)
                cells = updated
                showInputDialog = false
            }
        )
    }
}
