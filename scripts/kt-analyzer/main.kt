package analyzer

import java.io.*

fun pointsToPlaces(points: List<Int>): List<Int> {
    val sorted = points.withIndices() sortBy { -it.second }
    val result = IntArray(points.size)
    for (i in sorted.indices) {
        result[sorted[i].first] = i
    }
    return result.toList()
}

fun findMy(lineup: String): Int {
    var result: Int? = null
    for ((i, c) in lineup.toCharArray().withIndices()) {
        if (c == 'M') {
            if (result != null) {
                System.err.println("Warning: several positions of MyStrategy")
                break
            }
            result = i
        }
    }
    if (result == null) throw RuntimeException("No position of MyStrategy")
    return result!!
}

fun main(args: Array<String>) {
    val file = File(args[0])
    val lineup = file.getName().substring(0, 4)
    assert(lineup.toUpperCase() == lineup, "Lineup should be uppercased: $lineup")

    val my = findMy(lineup)

    val pl = IntArray(4)

    file.forEachLine {
        val points = it split ' ' map { it.toInt() }
        val places = pointsToPlaces(points)
        pl[places[my]]++
    }

    println(pl makeString " ")
    // val sum = 1.0 * pl.reduce { (a, b) -> a + b }
    // println(pl map { "%.2f".format(it / sum) } makeString " ")
}
