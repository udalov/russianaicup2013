package localRunner

import org.apache.log4j.Logger
import java.net.ConnectException
import java.io.File

fun localRunner(vis: Boolean, teamSize: Int, logFile: String, smartGuy: Boolean): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    val strategyClass = if (smartGuy) javaClass<com.a.b.a.a.e.a>() else javaClass<com.a.b.a.a.e.c>()

    return com.a.b.c(array(
            "-move-count=50",
            "-render-to-screen=$vis",
            "-render-to-screen-scale=1.0",
            "-render-to-screen-sync=true",
            "-results-file=" + logFile,
            "-debug=true",
            "-base-adapter-port=31000",
            "-seed=",
            "-p1-name=EmptyPlayer",
            "-p2-name=MyStrategy",
            "-p3-name=" + (if (smartGuy) "SmartGuy 1" else "QuickStartGuy 1"),
            "-p4-name=" + (if (smartGuy) "SmartGuy 2" else "QuickStartGuy 2"),
            "-p1-team-size=" + teamSize,
            "-p2-team-size=" + teamSize,
            "-p3-team-size=" + teamSize,
            "-p4-team-size=" + teamSize,
            javaClass<com.a.b.a.a.e.b>().getSimpleName() + ".class",
            "#LocalTestPlayer",
            strategyClass.getSimpleName() + ".class",
            strategyClass.getSimpleName() + ".class")
    )
}

val logFile = "out/log.txt"

fun printLogFile() {
    println(File(logFile).readText())
}

fun runMyStrategy() {
    while (true) {
        try {
            Runner.main(array<String>())
        } catch (e: ConnectException) {
            if ("Connection refused" == e.getMessage()) {
                Thread.sleep(500)
                continue
            }
        }
        break
    }
}

/**
 * @param args -vis, -smart
 */
fun main(args: Array<String>): Unit {
    val set = args.toSet()
    Thread(localRunner("-vis" in set, 3, logFile, "-smart" in set)).start()
    runMyStrategy()
    printLogFile()
}
