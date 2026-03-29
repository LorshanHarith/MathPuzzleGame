package com.example.mathpuzzlegame.logic

import com.example.mathpuzzlegame.data.CellData
import com.example.mathpuzzlegame.data.CellType
import com.example.mathpuzzlegame.data.Equation
import com.example.mathpuzzlegame.data.EquationState

fun getCellText(cell: CellData): String {
    return when (cell.type) {
        CellType.FIXED -> cell.fixedValue
        CellType.INPUT -> cell.inputValue
        CellType.BLOCK -> ""
    }
}

fun evaluateEquation(
    equation: Equation,
    cells: List<CellData>
): EquationState {
    val values = equation.cells.map { position ->
        cells.first { it.row == position.row && it.col == position.col }
    }

    if (values.size != 5) {
        return EquationState.WRONG
    }

    val first = getCellText(values[0])
    val operator = getCellText(values[1])
    val second = getCellText(values[2])
    val equalsSign = getCellText(values[3])
    val result = getCellText(values[4])

    if (equalsSign != "=") {
        return EquationState.WRONG
    }

    if (first.isBlank() || second.isBlank() || result.isBlank()) {
        return EquationState.INCOMPLETE
    }

    val a = first.toIntOrNull() ?: return EquationState.WRONG
    val b = second.toIntOrNull() ?: return EquationState.WRONG
    val c = result.toIntOrNull() ?: return EquationState.WRONG

    val calculated = when (operator) {
        "+" -> a + b
        "-" -> a - b
        "x", "*" -> a * b
        "/" -> {
            if (b == 0 || a % b != 0) {
                return EquationState.WRONG
            }
            a / b
        }

        else -> return EquationState.WRONG
    }

    return if (calculated == c) EquationState.CORRECT else EquationState.WRONG
}
