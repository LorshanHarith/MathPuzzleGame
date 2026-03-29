package com.example.mathpuzzlegame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mathpuzzlegame.data.CellData
import com.example.mathpuzzlegame.data.CellType
import com.example.mathpuzzlegame.data.EquationState
import com.example.mathpuzzlegame.data.GameMode
import com.example.mathpuzzlegame.data.GridPosition
import com.example.mathpuzzlegame.data.PuzzleDefinition
import com.example.mathpuzzlegame.data.PuzzleFactory
import com.example.mathpuzzlegame.data.SessionConfig
import com.example.mathpuzzlegame.data.cellListSaver
import com.example.mathpuzzlegame.logic.evaluateEquation
import com.example.mathpuzzlegame.logic.getCellText
import com.example.mathpuzzlegame.ui.components.NumberInputDialog
import com.example.mathpuzzlegame.ui.components.PuzzleCell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun GameScreen(
    sessionConfig: SessionConfig,
    onBackToMenu: () -> Unit
) {
    val sessionKey = remember(sessionConfig) {
        "${sessionConfig.mode.name}_${sessionConfig.sessionSeed}_${sessionConfig.requestedEquationCount}"
    }

    val puzzle by produceState<PuzzleDefinition?>(initialValue = null, key1 = sessionKey) {
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
    var showQuitDialog by rememberSaveable(sessionKey) { mutableStateOf(false) }

    var timerEnabled by rememberSaveable(sessionKey) { mutableStateOf(false) }
    var timerEndEpochMillis by rememberSaveable(sessionKey) { mutableLongStateOf(0L) }
    var timeLeft by rememberSaveable(sessionKey) { mutableIntStateOf(60) }
    var gameOver by rememberSaveable(sessionKey) { mutableStateOf(false) }

    val cellsByPosition = remember(cells) {
        cells.associateBy { it.row to it.col }
    }

    val equationStates = remember(cells, activePuzzle.equations) {
        activePuzzle.equations.map { equation -> evaluateEquation(equation, cells) }
    }
    val score = equationStates.count { it == EquationState.CORRECT }

    val equationIndicesByPosition = remember(activePuzzle.equations) {
        val map = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
        activePuzzle.equations.forEachIndexed { index, equation ->
            equation.cells.forEach { position ->
                val key = position.row to position.col
                map.getOrPut(key) { mutableListOf() }.add(index)
            }
        }
        map
    }

    val allInputCellsFilled = cells
        .filter { it.type == CellType.INPUT }
        .all { it.inputValue.isNotBlank() }
    val puzzleSolved = allInputCellsFilled && equationStates.all { it == EquationState.CORRECT }
    val gameLocked = gameOver || puzzleSolved

    val cellSize = remember(activePuzzle.rows, activePuzzle.cols) {
        val densityAnchor = maxOf(activePuzzle.rows, activePuzzle.cols)
        when {
            densityAnchor >= 20 -> 22.dp
            densityAnchor >= 18 -> 24.dp
            densityAnchor >= 16 -> 26.dp
            densityAnchor >= 14 -> 30.dp
            densityAnchor >= 12 -> 34.dp
            else -> 40.dp
        }
    }

    val inputDialogInitialValue = if (selectedCellIndex in cells.indices) {
        cells[selectedCellIndex].inputValue
    } else {
        ""
    }

    val requestExit = remember(gameLocked) {
        {
            if (gameLocked) {
                onBackToMenu()
            } else {
                showQuitDialog = true
            }
        }
    }

    BackHandler(onBack = { requestExit() })

    LaunchedEffect(timerEnabled, timerEndEpochMillis, puzzleSolved, gameOver) {
        if (!timerEnabled || puzzleSolved || gameOver) {
            return@LaunchedEffect
        }

        while (timerEnabled && !puzzleSolved && !gameOver) {
            val remainingMillis = timerEndEpochMillis - System.currentTimeMillis()
            val remainingSeconds = ((remainingMillis + 999L) / 1000L).toInt()

            if (remainingSeconds <= 0) {
                timeLeft = 0
                timerEnabled = false
                gameOver = true
                break
            }

            timeLeft = remainingSeconds
            delay(200L)
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
                            if (gameLocked) {
                                return@Switch
                            }
                            if (checked) {
                                timerEnabled = true
                                gameOver = false
                                timeLeft = 60
                                timerEndEpochMillis = System.currentTimeMillis() + 60_000L
                            } else {
                                timerEnabled = false
                                timeLeft = 60
                                timerEndEpochMillis = 0L
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { requestExit() }) {
                    Text(if (gameLocked) "Home" else "Quit")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Grid: ${activePuzzle.rows} x ${activePuzzle.cols}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 380.dp)
                .border(1.dp, Color(0xFF3C4247))
                .padding(4.dp)
        ) {
            val verticalGridScroll = rememberScrollState()
            val horizontalGridScroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .verticalScroll(verticalGridScroll)
                    .horizontalScroll(horizontalGridScroll)
            ) {
                for (row in 0 until activePuzzle.rows) {
                    Row {
                        for (col in 0 until activePuzzle.cols) {
                            val cell = cellsByPosition[row to col] ?: CellData(
                                row = row,
                                col = col,
                                type = CellType.BLOCK
                            )
                            val relatedIndices = equationIndicesByPosition[row to col].orEmpty()
                            val relatedStates = relatedIndices.map { index -> equationStates[index] }

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

                            val canClick = cell.type == CellType.INPUT && !gameLocked

                            PuzzleCell(
                                value = getCellText(cell),
                                backgroundColor = backgroundColor,
                                textColor = textColor,
                                clickable = canClick,
                                cellSize = cellSize,
                                onClick = {
                                    selectedCellIndex = cells.indexOfFirst {
                                        it.row == cell.row && it.col == cell.col
                                    }
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
                text = "Puzzle completed. You can go Home now.",
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

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Quit Game?") },
            text = {
                Text("Your current puzzle progress will be lost. Do you want to quit?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onBackToMenu()
                    }
                ) {
                    Text("Quit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
