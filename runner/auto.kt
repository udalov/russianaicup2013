package runner.auto

import runner.*
import java.io.File

fun findMyPlace(index: Int): Int {
    val lines = File(LOG_FILE).readLines()
    assert(lines.size() == 6, "Log file should contain exactly 6 lines: $lines")

    val line = lines[index + 2] split ' '
    if (line[2] != "OK") return -1
    return line[0].toInt()
}

// args = [seed, lineup]
fun main(args: Array<String>) {
    val seed = args[0].toLong()
    val lineup = args[1]
    runGame(false, seed, lineup)

    val index = lineup.indexOf('M')
    assert(index >= 0, "M not found in $lineup")
    println(findMyPlace(index))
}
