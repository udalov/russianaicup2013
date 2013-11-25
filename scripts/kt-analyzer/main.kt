package analyzer

import java.io.*

fun main(args: Array<String>) {
    val pl = IntArray(4)

    File(args[0]).forEachLine {
        val place = it.toInt()
        assert(1 <= place && place <= 4, "Bad place: $place")
        pl[place - 1]++
    }

    println(pl makeString " ")
}
