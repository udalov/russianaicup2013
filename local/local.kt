package runner.local

import Board.Kind.MAP06
import runner.LOG_FILE
import runner.runGame
import runner.time
import java.io.File
import java.util.*

fun main(args: Array<String>) = time {
    val seed = Math.abs(Random().nextLong())
    println(seed)

    runGame("-vis" in args.toSet(), MAP06, seed, "MS", "local")

    println(File(LOG_FILE).readText())
}
