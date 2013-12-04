package runner.auto

import runner.*
import java.io.File

fun findMyPlace(index: Int, players: Int): Int {
    val lines = File(LOG_FILE).readLines()
    assert(lines.size() == players + 2, "Log file should contain exactly ${players + 2} lines: $lines")

    val line = lines[index + 2] split ' '
    if (line[2] != "OK") return -1
    return line[0].toInt()
}

// args = [seed, map, lineup]
fun main(args: Array<String>) {
    val seed = args[0].toLong()
    val map = Board.Kind.valueOf(args[1])
    val lineup = args[2]

    runGame(false, map, seed, lineup, "auto")

    val index = lineup.indexOf('M')
    assert(index >= 0, "M not found in $lineup")
    println(findMyPlace(index, lineup.length))
}
