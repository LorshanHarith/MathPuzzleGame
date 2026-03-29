package com.example.mathpuzzlegame.data

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
            val inputCells = chooseBlankCells(block, mode)

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

    private fun chooseBlankCells(
        block: SolvedBlock,
        mode: GameMode
    ): Set<Pair<Int, Int>> {
        /*
         * We only blank result positions so users always input answers, never the question terms.
         * This also keeps generation fast and deterministic because no backtracking solver is needed.
         */
        return when (mode) {
            GameMode.NORMAL -> block.answerOnlyBlankPositions
            GameMode.ADVANCED -> block.answerOnlyBlankPositions
        }
    }

    private fun createSolvedBlock(random: Random): SolvedBlock {
        /*
         * Vertical equation is placed on column 4 so intersections are result cells from
         * horizontal equations. That guarantees editable cells remain answer positions.
         */
        val top = generateEquationForResult(random)
        val middle = generateEquationForResult(random)
        val verticalResult = top.result + middle.result
        val bottom = generateEquationForTargetResult(random, verticalResult)

        val vertical = ArithmeticEquation(
            firstOperand = top.result,
            operator = "+",
            secondOperand = middle.result,
            result = bottom.result
        )

        val layout = listOf(
            listOf(top.firstOperand.toString(), top.operator, top.secondOperand.toString(), "=", top.result.toString()),
            listOf("#", "#", "#", "#", vertical.operator),
            listOf(middle.firstOperand.toString(), middle.operator, middle.secondOperand.toString(), "=", middle.result.toString()),
            listOf("#", "#", "#", "#", "="),
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
                    GridPosition(0, 4),
                    GridPosition(1, 4),
                    GridPosition(2, 4),
                    GridPosition(3, 4),
                    GridPosition(4, 4)
                )
            )
        )

        return SolvedBlock(
            layout = layout,
            equations = equations,
            answerOnlyBlankPositions = setOf(
                0 to 4,
                2 to 4,
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
                val result = random.nextInt(2, 15)
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

    private fun generateEquationForTargetResult(
        random: Random,
        result: Int
    ): ArithmeticEquation {
        return when (random.nextInt(4)) {
            0 -> {
                val a = random.nextInt(1, result.coerceAtLeast(2))
                val b = result - a
                ArithmeticEquation(a, "+", b, result)
            }

            1 -> {
                val b = random.nextInt(1, 12)
                ArithmeticEquation(result + b, "-", b, result)
            }

            2 -> {
                val divisors = (1..result).filter { divisor -> result % divisor == 0 }
                val a = divisors.random(random)
                val b = result / a
                ArithmeticEquation(a, "x", b, result)
            }

            else -> {
                val b = random.nextInt(1, 12)
                ArithmeticEquation(result * b, "/", b, result)
            }
        }
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
    val answerOnlyBlankPositions: Set<Pair<Int, Int>>
)
