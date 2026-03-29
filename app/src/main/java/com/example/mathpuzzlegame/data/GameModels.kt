package com.example.mathpuzzlegame.data

enum class AppScreen {
    MENU,
    GAME
}

enum class GameMode {
    NORMAL,
    ADVANCED
}

enum class CellType {
    FIXED,
    INPUT,
    BLOCK
}

enum class EquationState {
    INCOMPLETE,
    CORRECT,
    WRONG
}

data class GridPosition(
    val row: Int,
    val col: Int
)

data class CellData(
    val row: Int,
    val col: Int,
    val type: CellType,
    val fixedValue: String = "",
    val inputValue: String = ""
)

data class Equation(
    val cells: List<GridPosition>
)

data class PuzzleDefinition(
    val rows: Int,
    val cols: Int,
    val cells: List<CellData>,
    val equations: List<Equation>
)

data class SessionConfig(
    val mode: GameMode,
    val sessionSeed: Int,
    val requestedEquationCount: Int
)
