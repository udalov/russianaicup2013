package runner

import org.apache.log4j.Logger
import java.net.ConnectException
import java.io.File

open class Player(val name: String, val classFile: String)
object MyStrategy : Player("MyStrategy", "#LocalTestPlayer")
object EmptyPlayer : Player("EmptyPlayer", javaClass<com.a.b.a.a.e.b>().getSimpleName() + ".class")
object QuickStartGuy : Player("QuickStartGuy", javaClass<com.a.b.a.a.e.c>().getSimpleName() + ".class")
object SmartGuy : Player("SmartGuy", javaClass<com.a.b.a.a.e.a>().getSimpleName() + ".class")

val LOG_FILE = "out/log.txt"

fun localRunner(vis: Boolean, teamSize: Int, seed: Long, p1: Player, p2: Player, p3: Player, p4: Player): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    return com.a.b.c(array(
            "-move-count=50",
            "-render-to-screen=$vis",
            "-render-to-screen-scale=1.0",
            "-render-to-screen-sync=true",
            "-results-file=$LOG_FILE",
            "-debug=true",
            "-base-adapter-port=31001",
            "-seed=$seed",
            "-p1-name=${p1.name}",
            "-p2-name=${p2.name}",
            "-p3-name=${p3.name}",
            "-p4-name=${p4.name}",
            "-p1-team-size=$teamSize",
            "-p2-team-size=$teamSize",
            "-p3-team-size=$teamSize",
            "-p4-team-size=$teamSize",
            p1.classFile,
            p2.classFile,
            p3.classFile,
            p4.classFile
    ))
}

fun runMyStrategy() {
    while (true) {
        try {
            Runner.main(array<String>())
        } catch (e: ConnectException) {
            if (e.getMessage() == "Connection refused") {
                Thread.sleep(40)
                continue
            } else throw e
        }
        break
    }
}

fun runGame(vis: Boolean, seed: Long, lineup: String) {
    fun parse(c: Char) = when (c) {
        'M' -> MyStrategy
        'E' -> EmptyPlayer
        'Q' -> QuickStartGuy
        'S' -> SmartGuy
        else -> throw IllegalStateException("Unknown player: $c")
    }

    Thread(localRunner(vis, 3, seed, parse(lineup[0]), parse(lineup[1]), parse(lineup[2]), parse(lineup[3]))).start()

    runMyStrategy()
}

fun time(block: () -> Unit) {
    println("${timer(block)}s")
}

fun timer(block: () -> Unit): Double {
    val begin = System.nanoTime()
    block()
    val end = System.nanoTime()
    return (end - begin) / 1e9
}
