package runner.keyboard

import runner.*
import java.io.File

/**
 * @param args -smart
 */
fun main(args: Array<String>) {
    val builder = ProcessBuilder("java", "-jar", "lib/local-runner.jar",
            "true", "true", "3", LOG_FILE, "${"-smart" in args.toSet()}", "true")
    val process = builder.start()
    runMyStrategy(31001)
    process.waitFor()
    println(File(LOG_FILE).readText())
}
