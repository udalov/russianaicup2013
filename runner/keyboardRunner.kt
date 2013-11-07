package keyboardRunner

import localRunner.*

/**
 * @param args -smart
 */
public fun main(args: Array<String>): Unit {
    val process = ProcessBuilder("java", "-jar", "lib/local-runner.jar",
            "true", "true", "3", logFile, "${"-smart" in args.toSet()}", "true")
    process.start()
    runMyStrategy()
    printLogFile()
}
