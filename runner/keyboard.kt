package runner.keyboard

import runner.*
import java.io.File

/**
 * @param args -smart
 */
fun main(args: Array<String>) {
    val process = ProcessBuilder("java", "-jar", "lib/local-runner.jar",
            "true", "true", "3", LOG_FILE, "${"-smart" in args.toSet()}", "true")
    process.start()
    runMyStrategy()

    println(File(LOG_FILE).readText())
}
