package runner.local

import runner.*
import java.io.File
import java.util.Random

fun main(args: Array<String>) = time {
    runGame("-vis" in args.toSet(), Math.abs(Random().nextLong()), "MSSS")

    println(File(LOG_FILE).readText())
}
