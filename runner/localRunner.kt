package runner.local

import org.apache.log4j.Logger
import java.net.ConnectException
import java.io.File
import runner.time

open class Player(val name: String, val classFile: String)
object MyStrategy : Player("MyStrategy", "#LocalTestPlayer")
object EmptyPlayer : Player("EmptyPlayer", javaClass<com.a.b.a.a.e.b>().getSimpleName() + ".class")
object QuickStartGuy : Player("QuickStartGuy", javaClass<com.a.b.a.a.e.c>().getSimpleName() + ".class")
object SmartGuy : Player("SmartGuy", javaClass<com.a.b.a.a.e.a>().getSimpleName() + ".class")

val LOG_FILE = "out/log.txt"

fun localRunner(vis: Boolean, teamSize: Int, p1: Player, p2: Player, p3: Player, p4: Player): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    return com.a.b.c(array(
            "-move-count=50",
            "-render-to-screen=$vis",
            "-render-to-screen-scale=1.0",
            "-render-to-screen-sync=true",
            "-results-file=$LOG_FILE",
            "-debug=true",
            "-base-adapter-port=31000",
            "-seed=",
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
                Thread.sleep(200)
                continue
            } else throw e
        }
        break
    }

    println(File(LOG_FILE).readText())
}

fun main(args: Array<String>) = time {
    val p1 = QuickStartGuy
    val p2 = MyStrategy
    val p3 = QuickStartGuy
    val p4 = QuickStartGuy

    Thread(localRunner("-vis" in args.toSet(), 3, p1, p2, p3, p4)).start()

    runMyStrategy()
}
