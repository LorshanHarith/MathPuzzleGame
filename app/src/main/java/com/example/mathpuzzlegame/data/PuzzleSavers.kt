package com.example.mathpuzzlegame.data

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

val cellListSaver = listSaver<List<CellData>, String>(
    save = { cells ->
        cells.map { cell ->
            listOf(
                cell.row.toString(),
                cell.col.toString(),
                cell.type.name,
                cell.fixedValue,
                cell.inputValue
            ).joinToString("||")
        }
    },
    restore = { saved ->
        saved.map { encoded ->
            val parts = encoded.split("||")
            CellData(
                row = parts[0].toInt(),
                col = parts[1].toInt(),
                type = CellType.valueOf(parts[2]),
                fixedValue = parts[3],
                inputValue = parts[4]
            )
        }
    }
)

val sessionConfigSaver: Saver<SessionConfig, Any> = listSaver<SessionConfig, String>(
    save = { config ->
        listOf(
            config.mode.name,
            config.sessionSeed.toString(),
            config.requestedEquationCount.toString()
        )
    },
    restore = { saved ->
        if (saved.size < 3) {
            null
        } else {
            SessionConfig(
                mode = GameMode.valueOf(saved[0]),
                sessionSeed = saved[1].toInt(),
                requestedEquationCount = saved[2].toInt()
            )
        }
    }
)
