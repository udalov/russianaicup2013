package runner.local

import runner.*
import java.io.File
import java.util.Random
import Board.Kind.*

fun main(args: Array<String>) = time {
    val seed = Math.abs(Random().nextLong())
    println(seed)

    runGame("-vis" in args.toSet(), MAP06, seed, "MS", "local")

    println(File(LOG_FILE).readText())
}
