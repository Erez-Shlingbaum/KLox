import lox.LoxRuntimeError
import org.junit.Test
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertEquals


class ScriptsTest: LoxTest() {
    @Test
    fun testFibonacci() {
        val script = File("$resourceDir/fibonacci.lox")
        lox.execute(script.readText())

        val scanner = Scanner(outContent.toString())

        val expected: MutableList<Int> = ArrayList()
        while (scanner.hasNextInt())
            expected += scanner.nextInt()

        assertEquals(expected,
            listOf(
                0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144,
                233, 377, 610, 987, 1597, 2584, 4181,
            ))
    }

    @Test
    fun testCounter() {
        val script = File("$resourceDir/counter.lox")
        lox.execute(script.readText())

        val scanner = Scanner(outContent.toString())

        val expected: MutableList<Int> = ArrayList()
        while (scanner.hasNextInt())
            expected += scanner.nextInt()

        assertEquals(expected,
            listOf(
                1, 2, 1, 2
            ))
    }
}