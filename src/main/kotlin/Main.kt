import lox.Lox
import java.io.File
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    val lox = Lox()
    when {
        args.isEmpty() -> lox.repl()
        args.size == 1 -> {
            val script = File(args[0])
            if (!script.isFile)
                println("Not a valid file: ${args[0]}")
            else {
                lox.execute(script.readText())
                if (lox.hadError)
                    exitProcess(65)
                if (lox.hadRuntimeError)
                    exitProcess(70)
            }
        }
        else -> System.err.println("Too many arguments")
    }
}