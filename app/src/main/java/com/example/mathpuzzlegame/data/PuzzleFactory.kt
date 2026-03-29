package com.example.mathpuzzlegame.data

import com.example.mathpuzzlegame.logic.evaluateEquation
import kotlin.math.max
import kotlin.random.Random

object PuzzleFactory {

    private const val MIN_GRID_SIZE = 11
    private const val MAX_GRID_SIZE = 20
    private const val BLOCK_SIZE = 5
    private const val BLOCK_SPACING = 6

    fun createPuzzle(
        mode: GameMode,
        requestedEquationCount: Int,
        seed: Int
    ): PuzzleDefinition {
        val random = Random(seed)
        val blockCount = max(1, requestedEquationCount / 4)
        val rows = chooseDimension(random, blockCount)
        val cols = chooseDimension(random, blockCount)

        val board = MutableList(rows) { row ->
            MutableList(cols) { col ->
                CellData(row = row, col = col, type = CellType.BLOCK)
            }
        }

        val equations = mutableListOf<Equation>()
        val placements = choosePlacements(rows, cols, blockCount, random)

        for (placement in placements) {
            val block = createSolvedBlock(random)
            val inputCells = when (mode) {
                GameMode.NORMAL -> chooseNormalBlankCells(block, random)
                GameMode.ADVANCED -> chooseAdvancedBlankCells(block, random)
            }

            for (row in 0 until BLOCK_SIZE) {
                for (col in 0 until BLOCK_SIZE) {
                    val absoluteRow = placement.row + row
                    val absoluteCol = placement.col + col
                    val solvedValue = block.layout[row][col]
                    board[absoluteRow][absoluteCol] = if (solvedValue == "#") {
                        CellData(
                            row = absoluteRow,
                            col = absoluteCol,
                            type = CellType.BLOCK
                        )
                    } else if (inputCells.contains(row to col)) {
                        CellData(
                            row = absoluteRow,
                            col = absoluteCol,
                            type = CellType.INPUT
                        )
                    } else {
                        CellData(
                            row = absoluteRow,
                            col = absoluteCol,
                            type = CellType.FIXED,
                            fixedValue = solvedValue
                        )
                    }
                }
            }

            equations += block.equations.map { equation ->
                Equation(
                    cells = equation.cells.map { position ->
                        GridPosition(
                            row = placement.row + position.row,
                            col = placement.col + position.col
                        )
                    }
                )
            }
        }

        return PuzzleDefinition(
            rows = rows,
            cols = cols,
            cells = board.flatten(),
            equations = equations
        )
    }

    private fun chooseDimension(random: Random, blockCount: Int): Int {
        val minimumNeeded = max(MIN_GRID_SIZE, blockCount * BLOCK_SPACING - 1)
        val maximumAllowed = max(minimumNeeded, MAX_GRID_SIZE)
        return random.nextInt(minimumNeeded, maximumAllowed + 1)
    }

    private fun choosePlacements(
        rows: Int,
        cols: Int,
        blockCount: Int,
        random: Random
    ): List<GridPosition> {
        val possiblePlacements = mutableListOf<GridPosition>()

        for (row in 0..(rows - BLOCK_SIZE) step BLOCK_SPACING) {
            for (col in 0..(cols - BLOCK_SIZE) step BLOCK_SPACING) {
                possiblePlacements += GridPosition(row, col)
            }
        }

        return possiblePlacements
            .shuffled(random)
            .take(blockCount)
            .sortedWith(compareBy<GridPosition> { it.row }.thenBy { it.col })
    }

    private fun chooseNormalBlankCells(
        block: SolvedBlock,
        random: Random
    ): Set<Pair<Int, Int>> {
        val blanks = mutableSetOf<Pair<Int, Int>>()
        val candidateMap = block.numberPositionsByEquation()

        for (candidateList in candidateMap) {
            blanks += candidateList.random(random)
        }

        val extraCandidates = block.numberPositions.shuffled(random)
        val extraBlankCount = random.nextInt(1, 3)
        for (position in extraCandidates.take(extraBlankCount)) {
            blanks += position
        }

        return blanks
    }

    private fun chooseAdvancedBlankCells(
        block: SolvedBlock,
        random: Random
    ): Set<Pair<Int, Int>> {
        /*
         * Advanced mode uses a greedy "blank as much as possible while keeping one solution"
         * strategy for each 5x5 cross block. The block is small, so we can test whether
         * blanking one more visible number still leaves exactly one valid completion.
         *
         * Why this works well for this coursework:
         * 1. Each block contains only four equations, so generation is quick on a phone.
         * 2. The vertical equation links the three horizontal equations, which makes the
         *    block heavily constrained and suitable for greedy blanking.
         * 3. The result is visibly harder than normal mode because more cells become editable.
         *
         * Limitation:
         * This is a local greedy heuristic rather than a perfect global optimizer. It is a
         * practical choice because the blocks are independent and the heuristic reliably
         * produces dense advanced puzzles without harming performance.
         */
        val visible = block.numberPositions.toMutableSet()

        for (candidate in block.numberPositions.shuffled(random)) {
            val trialVisible = visible.toMutableSet()
            trialVisible.remove(candidate)

            if (countSolutions(block, trialVisible, solutionLimit = 2) == 1) {
                visible.remove(candidate)
            }
        }

        return block.numberPositions.toSet() - visible
    }

    private fun countSolutions(
        block: SolvedBlock,
        visibleNumbers: Set<Pair<Int, Int>>,
        solutionLimit: Int
    ): Int {
        val fixedNumbers = visibleNumbers.associateWith { position ->
            block.layout[position.first][position.second].toInt()
        }
        val blankNumbers = block.numberPositions.filter { it !in visibleNumbers }
        var solutions = 0

        fun search(assignments: MutableMap<Pair<Int, Int>, Int>) {
            if (solutions >= solutionLimit) {
                return
            }

            val next = blankNumbers.firstOrNull { it !in assignments } ?: run {
                if (allEquationsValid(block, fixedNumbers, assignments)) {
                    solutions++
                }
                return
            }

            val candidates = deriveCandidates(block, next, fixedNumbers, assignments)
            for (candidate in candidates) {
                assignments[next] = candidate
                if (equationsRemainPossible(block, fixedNumbers, assignments)) {
                    search(assignments)
                }
                assignments.remove(next)
            }
        }

        search(mutableMapOf())
        return solutions
    }

    private fun allEquationsValid(
        block: SolvedBlock,
        fixedNumbers: Map<Pair<Int, Int>, Int>,
        assignments: Map<Pair<Int, Int>, Int>
    ): Boolean {
        return block.equations.all { equation ->
            val asCells = equation.cells.map { position ->
                val coordinate = position.row to position.col
                val text = when {
                    fixedNumbers.containsKey(coordinate) -> fixedNumbers.getValue(coordinate).toString()
                    assignments.containsKey(coordinate) -> assignments.getValue(coordinate).toString()
                    else -> block.layout[position.row][position.col]
                }

                CellData(
                    row = position.row,
                    col = position.col,
                    type = CellType.FIXED,
                    fixedValue = text
                )
            }

            evaluateEquation(Equation(equation.cells), asCells) == EquationState.CORRECT
        }
    }

    private fun equationsRemainPossible(
        block: SolvedBlock,
        fixedNumbers: Map<Pair<Int, Int>, Int>,
        assignments: Map<Pair<Int, Int>, Int>
    ): Boolean {
        return block.equations.all { equation ->
            val values = listOf(0, 2, 4).map { index ->
                val position = equation.cells[index].row to equation.cells[index].col
                fixedNumbers[position] ?: assignments[position]
            }

            val operator = block.layout[equation.cells[1].row][equation.cells[1].col]
            when {
                values[0] != null && values[1] != null && values[2] != null ->
                    evaluate(operator, values[0]!!, values[1]!!) == values[2]

                values[0] != null && values[1] != null ->
                    evaluate(operator, values[0]!!, values[1]!!) != null

                values[0] != null && values[2] != null ->
                    invertSecond(operator, values[0]!!, values[2]!!) != null

                values[1] != null && values[2] != null ->
                    invertFirst(operator, values[1]!!, values[2]!!) != null

                else -> true
            }
        }
    }

    private fun deriveCandidates(
        block: SolvedBlock,
        target: Pair<Int, Int>,
        fixedNumbers: Map<Pair<Int, Int>, Int>,
        assignments: Map<Pair<Int, Int>, Int>
    ): List<Int> {
        val relatedEquations = block.equations.filter { equation ->
            equation.cells.any { it.row == target.first && it.col == target.second }
        }

        var candidateSet: Set<Int>? = null

        for (equation in relatedEquations) {
            val aPosition = equation.cells[0].row to equation.cells[0].col
            val bPosition = equation.cells[2].row to equation.cells[2].col
            val cPosition = equation.cells[4].row to equation.cells[4].col

            val a = fixedNumbers[aPosition] ?: assignments[aPosition]
            val b = fixedNumbers[bPosition] ?: assignments[bPosition]
            val c = fixedNumbers[cPosition] ?: assignments[cPosition]
            val operator = block.layout[equation.cells[1].row][equation.cells[1].col]

            val localCandidates = when (target) {
                aPosition -> if (b != null && c != null) listOfNotNull(invertFirst(operator, b, c)) else null
                bPosition -> if (a != null && c != null) listOfNotNull(invertSecond(operator, a, c)) else null
                cPosition -> if (a != null && b != null) listOfNotNull(evaluate(operator, a, b)) else null
                else -> null
            }

            if (localCandidates != null) {
                candidateSet = if (candidateSet == null) {
                    localCandidates.toSet()
                } else {
                    candidateSet!!.intersect(localCandidates.toSet())
                }
            }
        }

        return (candidateSet ?: (1..81).toSet())
            .filter { it > 0 }
            .sorted()
    }

    private fun createSolvedBlock(random: Random): SolvedBlock {
        val vertical = generateEquationForResult(random)
        val top = generateEquationWithSecondOperand(random, vertical.firstOperand)
        val middle = generateEquationWithSecondOperand(random, vertical.secondOperand)
        val bottom = generateEquationWithSecondOperand(random, vertical.result)

        val layout = listOf(
            listOf(top.firstOperand.toString(), top.operator, top.secondOperand.toString(), "=", top.result.toString()),
            listOf("#", "#", vertical.operator, "#", "#"),
            listOf(middle.firstOperand.toString(), middle.operator, middle.secondOperand.toString(), "=", middle.result.toString()),
            listOf("#", "#", "=", "#", "#"),
            listOf(bottom.firstOperand.toString(), bottom.operator, bottom.secondOperand.toString(), "=", bottom.result.toString())
        )

        val equations = listOf(
            Equation(
                listOf(
                    GridPosition(0, 0),
                    GridPosition(0, 1),
                    GridPosition(0, 2),
                    GridPosition(0, 3),
                    GridPosition(0, 4)
                )
            ),
            Equation(
                listOf(
                    GridPosition(2, 0),
                    GridPosition(2, 1),
                    GridPosition(2, 2),
                    GridPosition(2, 3),
                    GridPosition(2, 4)
                )
            ),
            Equation(
                listOf(
                    GridPosition(4, 0),
                    GridPosition(4, 1),
                    GridPosition(4, 2),
                    GridPosition(4, 3),
                    GridPosition(4, 4)
                )
            ),
            Equation(
                listOf(
                    GridPosition(0, 2),
                    GridPosition(1, 2),
                    GridPosition(2, 2),
                    GridPosition(3, 2),
                    GridPosition(4, 2)
                )
            )
        )

        return SolvedBlock(
            layout = layout,
            equations = equations,
            numberPositions = listOf(
                0 to 0,
                0 to 2,
                0 to 4,
                2 to 0,
                2 to 2,
                2 to 4,
                4 to 0,
                4 to 2,
                4 to 4
            )
        )
    }

    private fun generateEquationForResult(random: Random): ArithmeticEquation {
        return when (random.nextInt(4)) {
            0 -> {
                val a = random.nextInt(2, 15)
                val b = random.nextInt(2, 15)
                ArithmeticEquation(a, "+", b, a + b)
            }

            1 -> {
                val b = random.nextInt(2, 15)
                val result = random.nextInt(1, 15)
                ArithmeticEquation(result + b, "-", b, result)
            }

            2 -> {
                val a = random.nextInt(2, 10)
                val b = random.nextInt(2, 10)
                ArithmeticEquation(a, "x", b, a * b)
            }

            else -> {
                val b = random.nextInt(2, 10)
                val result = random.nextInt(2, 10)
                ArithmeticEquation(b * result, "/", b, result)
            }
        }
    }

    private fun generateEquationWithSecondOperand(
        random: Random,
        secondOperand: Int
    ): ArithmeticEquation {
        return when (random.nextInt(4)) {
            0 -> {
                val a = random.nextInt(2, 25)
                ArithmeticEquation(a, "+", secondOperand, a + secondOperand)
            }

            1 -> {
                val result = random.nextInt(1, 25)
                ArithmeticEquation(result + secondOperand, "-", secondOperand, result)
            }

            2 -> {
                val a = random.nextInt(2, 12)
                ArithmeticEquation(a, "x", secondOperand, a * secondOperand)
            }

            else -> {
                val result = random.nextInt(2, 12)
                ArithmeticEquation(secondOperand * result, "/", secondOperand, result)
            }
        }
    }

    private fun evaluate(
        operator: String,
        first: Int,
        second: Int
    ): Int? {
        return when (operator) {
            "+" -> first + second
            "-" -> first - second
            "x" -> first * second
            "/" -> if (second != 0 && first % second == 0) first / second else null
            else -> null
        }
    }

    private fun invertFirst(
        operator: String,
        second: Int,
        result: Int
    ): Int? {
        return when (operator) {
            "+" -> result - second
            "-" -> result + second
            "x" -> if (second != 0 && result % second == 0) result / second else null
            "/" -> result * second
            else -> null
        }?.takeIf { it > 0 }
    }

    private fun invertSecond(
        operator: String,
        first: Int,
        result: Int
    ): Int? {
        return when (operator) {
            "+" -> result - first
            "-" -> first - result
            "x" -> if (first != 0 && result % first == 0) result / first else null
            "/" -> if (result != 0 && first % result == 0) first / result else null
            else -> null
        }?.takeIf { it > 0 }
    }
}

private data class ArithmeticEquation(
    val firstOperand: Int,
    val operator: String,
    val secondOperand: Int,
    val result: Int
)

private data class SolvedBlock(
    val layout: List<List<String>>,
    val equations: List<Equation>,
    val numberPositions: List<Pair<Int, Int>>
) {
    fun numberPositionsByEquation(): List<List<Pair<Int, Int>>> {
        return equations.map { equation ->
            listOf(
                equation.cells[0].row to equation.cells[0].col,
                equation.cells[2].row to equation.cells[2].col,
                equation.cells[4].row to equation.cells[4].col
            )
        }
    }
}
