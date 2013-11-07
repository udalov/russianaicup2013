package runner

fun time(block: () -> Unit) {
    println("${timer(block)}s")
}

fun timer(block: () -> Unit): Double {
    val begin = System.nanoTime()
    block()
    val end = System.nanoTime()
    return (end - begin) / 1000000000.0
}
