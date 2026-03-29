package com.example.mathpuzzlegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

enum class Screen {
    MENU, GAME, ADVANCED
}

enum class CellType {
    FIXED, INPUT, BLOCK
}

data class CellData(
    val row: Int,
    val col: Int,
    val type: CellType,
    val fixedValue: String = "",
    val inputValue: String = ""
)

data class Equation(
    val cells: List<Pair<Int, Int>>
)

private val cellListSaver = listSaver<MutableList<CellData>, String>(
    save = { list ->
        list.map { cell ->
            listOf(
                cell.row.toString(),
                cell.col.toString(),
                cell.type.name,
                cell.fixedValue,
                cell.inputValue
            ).joinToString("||")
        }
    },
    restore = { savedList ->
        savedList.map { item ->
            val parts = item.split("||")
            CellData(
                row = parts[0].toInt(),
                col = parts[1].toInt(),
                type = CellType.valueOf(parts[2]),
                fixedValue = parts[3],
                inputValue = parts[4]
            )
        }.toMutableList()
    }
)

class PuzzleDefinition(
    val rows: Int,
    val cols: Int,
    val cells: MutableList<CellData>,
    val equations: List<Equation>
)

fun createNormalPuzzle(): PuzzleDefinition {
    val rows = 5
    val cols = 5

    val cells = mutableListOf<CellData>()

    fun addCell(row: Int, col: Int, type: CellType, fixed: String = "", input: String = "") {
        cells.add(
            CellData(
                row = row,
                col = col,
                type = type,
                fixedValue = fixed,
                inputValue = input
            )
        )
    }

    // Row 0: 3 × _ = 33
    addCell(0, 0, CellType.FIXED, "3")
    addCell(0, 1, CellType.FIXED, "×")
    addCell(0, 2, CellType.INPUT)
    addCell(0, 3, CellType.FIXED, "=")
    addCell(0, 4, CellType.FIXED, "33")

    // Row 1: black black / black black
    addCell(1, 0, CellType.BLOCK)
    addCell(1, 1, CellType.BLOCK)
    addCell(1, 2, CellType.FIXED, "/")
    addCell(1, 3, CellType.BLOCK)
    addCell(1, 4, CellType.BLOCK)

    // Row 2: 8 - _ = 4
    addCell(2, 0, CellType.FIXED, "8")
    addCell(2, 1, CellType.FIXED, "-")
    addCell(2, 2, CellType.INPUT)
    addCell(2, 3, CellType.FIXED, "=")
    addCell(2, 4, CellType.FIXED, "4")

    // Row 3: black black = black black
    addCell(3, 0, CellType.BLOCK)
    addCell(3, 1, CellType.BLOCK)
    addCell(3, 2, CellType.FIXED, "=")
    addCell(3, 3, CellType.BLOCK)
    addCell(3, 4, CellType.BLOCK)

    // Row 4: 33 / _ = 11
    addCell(4, 0, CellType.FIXED, "33")
    addCell(4, 1, CellType.FIXED, "/")
    addCell(4, 2, CellType.INPUT)
    addCell(4, 3, CellType.FIXED, "=")
    addCell(4, 4, CellType.FIXED, "11")

    val equations = listOf(
        Equation(listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4)), // 3 × _ = 33
        Equation(listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4)), // 8 - _ = 4
        Equation(listOf(0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2)), // 11 / 4 = ? structure
        Equation(listOf(4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4))  // 33 / _ = 11
    )

    return PuzzleDefinition(rows, cols, cells, equations)
}

fun createAdvancedPuzzle(): PuzzleDefinition {
    val rows = 5
    val cols = 5

    val cells = mutableListOf<CellData>()

    fun addCell(row: Int, col: Int, type: CellType, fixed: String = "", input: String = "") {
        cells.add(
            CellData(
                row = row,
                col = col,
                type = type,
                fixedValue = fixed,
                inputValue = input
            )
        )
    }

    // Harder because more blanks
    // Row 0: _ × _ = 33
    addCell(0, 0, CellType.INPUT)
    addCell(0, 1, CellType.FIXED, "×")
    addCell(0, 2, CellType.INPUT)
    addCell(0, 3, CellType.FIXED, "=")
    addCell(0, 4, CellType.FIXED, "33")

    // Row 1
    addCell(1, 0, CellType.BLOCK)
    addCell(1, 1, CellType.BLOCK)
    addCell(1, 2, CellType.FIXED, "/")
    addCell(1, 3, CellType.BLOCK)
    addCell(1, 4, CellType.BLOCK)

    // Row 2: 8 - _ = 4
    addCell(2, 0, CellType.FIXED, "8")
    addCell(2, 1, CellType.FIXED, "-")
    addCell(2, 2, CellType.INPUT)
    addCell(2, 3, CellType.FIXED, "=")
    addCell(2, 4, CellType.FIXED, "4")

    // Row 3
    addCell(3, 0, CellType.BLOCK)
    addCell(3, 1, CellType.BLOCK)
    addCell(3, 2, CellType.FIXED, "=")
    addCell(3, 3, CellType.BLOCK)
    addCell(3, 4, CellType.BLOCK)

    // Row 4: 33 / _ = _
    addCell(4, 0, CellType.FIXED, "33")
    addCell(4, 1, CellType.FIXED, "/")
    addCell(4, 2, CellType.INPUT)
    addCell(4, 3, CellType.FIXED, "=")
    addCell(4, 4, CellType.INPUT)

    val equations = listOf(
        Equation(listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        Equation(listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4)),
        Equation(listOf(0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2)),
        Equation(listOf(4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4))
    )

    return PuzzleDefinition(rows, cols, cells, equations)
}

@Composable
fun App() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.MENU.name) }
    var gameId by rememberSaveable { mutableStateOf(0) }
    var isAdvanced by rememberSaveable { mutableStateOf(false) }

    when (Screen.valueOf(currentScreen)) {
        Screen.MENU -> MainMenu(
            onNewGame = {
                isAdvanced = false
                gameId++
                currentScreen = Screen.GAME.name
            },
            onAdvanced = {
                isAdvanced = true
                gameId++
                currentScreen = Screen.ADVANCED.name
            }
        )

        Screen.GAME -> GameScreen(
            key = "normal_$gameId",
            puzzleFactory = { createNormalPuzzle() },
            title = "New Game",
            onBackToMenu = { currentScreen = Screen.MENU.name }
        )

        Screen.ADVANCED -> GameScreen(
            key = "advanced_$gameId",
            puzzleFactory = { createAdvancedPuzzle() },
            title = "Advanced Level",
            onBackToMenu = { currentScreen = Screen.MENU.name }
        )
    }
}

@Composable
fun MainMenu(
    onNewGame: () -> Unit,
    onAdvanced: () -> Unit
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cross Math Puzzle Game",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("New Game")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onAdvanced, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Advanced Level")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { showAbout = true }, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("About")
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                Button(onClick = { showAbout = false }) {
                    Text("OK")
                }
            },
            title = { Text("About") },
            text = {
                Text(
                    "Name: Your Name\n" +
                            "Student ID: Your Student ID\n\n" +
                            "I confirm that I understand what plagiarism is and have read and understood " +
                            "the section on Assessment Offences in the Essential Information for Students. " +
                            "The work that I have submitted is entirely my own. Any work from other authors " +
                            "is duly referenced and acknowledged."
                )
            }
        )
    }
}

@Composable
fun GameScreen(
    key: String,
    puzzleFactory: () -> PuzzleDefinition,
    title: String,
    onBackToMenu: () -> Unit
) {
    val puzzle = remember(key) { puzzleFactory() }

    var cells by rememberSaveable(key, stateSaver = cellListSaver) {
        mutableStateOf(puzzle.cells)
    }

    var selectedIndex by rememberSaveable(key) { mutableStateOf(-1) }
    var showInputDialog by rememberSaveable(key) { mutableStateOf(false) }
    var currentInput by rememberSaveable(key) { mutableStateOf("") }

    var timerEnabled by rememberSaveable(key) { mutableStateOf(false) }
    var timeLeft by rememberSaveable(key) { mutableStateOf(60) }
    var gameOver by rememberSaveable(key) { mutableStateOf(false) }

    val equationStates = remember(cells) {
        puzzle.equations.map { equation ->
            evaluateEquation(equation, cells)
        }
    }

    val score = equationStates.count { it == EquationState.CORRECT }

    val allInputsFilled = cells
        .filter { it.type == CellType.INPUT }
        .all { it.inputValue.isNotBlank() }

    val puzzleCompleted = allInputsFilled && equationStates.all { it == EquationState.CORRECT }

    BackHandler {
        onBackToMenu()
    }

    LaunchedEffect(timerEnabled, gameOver, puzzleCompleted) {
        if (timerEnabled && !gameOver && !puzzleCompleted) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
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
        Text(text = title, style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Timer")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = timerEnabled,
                        onCheckedChange = {
                            if (!gameOver && !puzzleCompleted) {
                                timerEnabled = it
                                if (!it) {
                                    timeLeft = 60
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Time: $timeLeft")
                }
            }

            Text("Score: $score")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until puzzle.rows) {
                Row {
                    for (col in 0 until puzzle.cols) {
                        val cell = cells.first { it.row == row && it.col == col }
                        val relatedStates = puzzle.equations
                            .filter { eq -> eq.cells.contains(row to col) }
                            .map { eq -> evaluateEquation(eq, cells) }

                        val cellColor = when {
                            cell.type == CellType.BLOCK -> Color.Black
                            relatedStates.contains(EquationState.WRONG) -> Color.Red
                            relatedStates.contains(EquationState.CORRECT) -> Color.Green
                            else -> Color.White
                        }

                        val displayValue = when (cell.type) {
                            CellType.FIXED -> cell.fixedValue
                            CellType.INPUT -> if (cell.inputValue.isBlank()) "" else cell.inputValue
                            CellType.BLOCK -> ""
                        }

                        val canClick = cell.type == CellType.INPUT && !gameOver && !puzzleCompleted

                        BoxCell(
                            value = displayValue,
                            color = cellColor,
                            clickable = canClick,
                            onClick = {
                                selectedIndex = cells.indexOf(cell)
                                currentInput = cell.inputValue
                                showInputDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (puzzleCompleted) {
            Text(
                "PUZZLE COMPLETED! Press Back to return to the menu.",
                color = Color(0xFF0A7D20)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (gameOver) {
            Text(
                "GAME OVER!",
                color = Color.Red,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(onClick = onBackToMenu) {
            Text("Back")
        }
    }

    if (showInputDialog && selectedIndex != -1) {
        AlertDialog(
            onDismissRequest = {
                showInputDialog = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentInput.isBlank() || currentInput.toIntOrNull() != null) {
                            val updated = cells.toMutableList()
                            val oldCell = updated[selectedIndex]
                            updated[selectedIndex] = oldCell.copy(inputValue = currentInput)
                            cells = updated
                            showInputDialog = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Enter Number") },
            text = {
                TextField(
                    value = currentInput,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            currentInput = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        )
    }
}

enum class EquationState {
    INCOMPLETE, CORRECT, WRONG
}

fun evaluateEquation(
    equation: Equation,
    cells: List<CellData>
): EquationState {
    val values = equation.cells.map { (row, col) ->
        cells.first { it.row == row && it.col == col }
    }

    if (values.size != 5) return EquationState.WRONG

    val first = getCellText(values[0])
    val op = getCellText(values[1])
    val second = getCellText(values[2])
    val equalsSign = getCellText(values[3])
    val result = getCellText(values[4])

    if (equalsSign != "=") return EquationState.WRONG
    if (first.isBlank() || second.isBlank() || result.isBlank()) return EquationState.INCOMPLETE

    val a = first.toIntOrNull() ?: return EquationState.WRONG
    val b = second.toIntOrNull() ?: return EquationState.WRONG
    val c = result.toIntOrNull() ?: return EquationState.WRONG

    val answer = when (op) {
        "+" -> a + b
        "-" -> a - b
        "×", "x", "*" -> a * b
        "/" -> {
            if (b == 0) return EquationState.WRONG
            if (a % b != 0) return EquationState.WRONG
            a / b
        }
        else -> return EquationState.WRONG
    }

    return if (answer == c) EquationState.CORRECT else EquationState.WRONG
}

fun getCellText(cell: CellData): String {
    return when (cell.type) {
        CellType.FIXED -> cell.fixedValue
        CellType.INPUT -> cell.inputValue
        CellType.BLOCK -> ""
    }
}

@Composable
fun BoxCell(
    value: String,
    color: Color,
    clickable: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .border(1.dp, Color.Black)
            .background(color)
            .then(
                if (clickable) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = if (color == Color.Black) Color.White else Color.Black
        )
    }
}