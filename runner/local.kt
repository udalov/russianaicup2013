package runner.local

import runner.*
import runner.WorldMap.*
import java.io.File
import java.util.Random

fun main(args: Array<String>) = time {
    val seed = Math.abs(Random().nextLong())
    println(seed)

    runGame("-vis" in args.toSet(), MAP01, seed, "MSSS", "local")

    println(File(LOG_FILE).readText())
}
