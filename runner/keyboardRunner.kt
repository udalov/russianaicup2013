package runner.keyboard

import runner.local.*

/**
 * @param args -smart
 */
public fun main(args: Array<String>) {
    val process = ProcessBuilder("java", "-jar", "lib/local-runner.jar",
            "true", "true", "3", LOG_FILE, "${"-smart" in args.toSet()}", "true")
    process.start()
    runMyStrategy()
}
