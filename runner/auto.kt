package runner.auto

import runner.*
import java.io.File
import java.util.ArrayList

fun printShortLog() {
    val lines = File(LOG_FILE).readLines()
    assert(lines.size() == 6, "Log file should contain exactly 6 lines: $lines")

    val points = ArrayList<Int>(4)
    for (i in 2..5) {
        val line = lines[i] split ' '
        points.add(if (line[2] == "OK") line[1].toInt() else -1000000000)
    }

    println(points makeString " ")
}

// args = [seed, lineup]
fun main(args: Array<String>) {
    runGame(false, args[0].toLong(), args[1])
    printShortLog()
}
