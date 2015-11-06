package runner

import Board
import org.apache.log4j.Logger
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.ConnectException
import java.util.*

open class Player(val name: String, val classFile: String)
object MyStrategy : Player("MyStrategy", "#LocalTestPlayer")
object EmptyPlayer : Player("EmptyPlayer", com.a.b.a.a.e.b::class.java.simpleName + ".class")
object QuickStartGuy : Player("QuickStartGuy", com.a.b.a.a.e.c::class.java.simpleName + ".class")
object SmartGuy : Player("SmartGuy", com.a.b.a.a.e.a::class.java.simpleName + ".class")

val LOG_FILE = "out/log.txt"

fun localRunner(vis: Boolean, map: Board.Kind, teamSize: Int, seed: Long, players: List<Player>): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    val args = arrayListOf(
            "-move-count=50",
            "-render-to-screen=$vis",
            "-render-to-screen-scale=1.0",
            "-render-to-screen-sync=true",
            "-results-file=$LOG_FILE",
            "-debug=true",
            "-map=${map.toString().toLowerCase()}",
            "-base-adapter-port=31001",
            "-seed=$seed"
    )

    for ((index, player) in players.withIndex()) {
        val i = index + 1
        args.add("-p$i-name=${player.name}")
        args.add("-p$i-team-size=$teamSize")
        args.add(player.classFile)
    }

    return com.a.b.c(args.toTypedArray())
}

fun runMyStrategy(port: Long) {
    while (true) {
        try {
            val runnerClass = Class.forName("Runner")
            val main = runnerClass.getDeclaredMethod("main", Array<String>::class.java)
            try {
                main(null, arrayOf("127.0.0.1", "$port", "0000000000000000"))
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        } catch (e: ConnectException) {
            if (e.message?.startsWith("Connection refused") ?: false) {
                Thread.sleep(40)
                continue
            } else throw e
        }
        break
    }
}

fun parsePlayer(c: Char) = when (c) {
    'M' -> MyStrategy
    'E' -> EmptyPlayer
    'Q' -> QuickStartGuy
    'S' -> SmartGuy
    else -> throw IllegalStateException("Unknown player: $c")
}

fun runGame(vis: Boolean, map: Board.Kind, seed: Long, lineup: String, threadName: String) {
    val teamSize = if (lineup.length == 2) 5 else 4

    val threads = ArrayList<Thread>(5)
    threads.add(Thread(localRunner(vis, map, teamSize, seed, lineup.map(::parsePlayer))))

    val initialPort: Long = 31001
    var myStrategies = 0

    for (team in lineup) {
        if (team != 'M') continue

        val port = initialPort + (myStrategies++)

        if (myStrategies == 1) {
            threads.add(Thread {
                Thread.currentThread().name = threadName
                runMyStrategy(port)
            })
            continue
        }

        val file = File("dist/m${myStrategies}.jar")
        assert(file.exists()) { "Compile a strategy to test against to: $file" }
        threads.add(Thread {
            Thread.currentThread().name = threadName
            while (true) {
                val process = Runtime.getRuntime().exec("java -cp $file Runner 127.0.0.1 $port 0000000000000000")
                val err = process.errorStream?.reader()
                process.waitFor()
                if (err?.readText()?.contains("Connection refused") ?: false) {
                    Thread.sleep(40)
                    continue
                }
                break
            }
        })
    }

    for (thread in threads) {
        thread.start()
    }

    for (thread in threads) {
        thread.join()
    }
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
