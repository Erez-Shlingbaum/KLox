import org.junit.Test
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertEquals


class ScriptsTest : LoxTest() {
    @Test
    fun testFibonacci() {
        val script = File("$resourceDir/fibonacci.lox")
        lox.execute(script.readText())

        val scanner = Scanner(outContent.toString())

        val actual: MutableList<Int> = ArrayList()
        while (scanner.hasNextInt())
            actual += scanner.nextInt()

        assertEquals(
            listOf(
                0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144,
                233, 377, 610, 987, 1597, 2584, 4181,
            ),
            actual
        )
    }

    @Test
    fun testCounter() {
        val script = File("$resourceDir/counter.lox")
        lox.execute(script.readText())

        val scanner = Scanner(outContent.toString())

        val actual: MutableList<Int> = ArrayList()
        while (scanner.hasNextInt())
            actual += scanner.nextInt()

        assertEquals(
            listOf(
                1, 2, 1, 2
            ),
            actual
        )
    }

    @Test
    fun testOperators() {
        val script = File("$resourceDir/operators.lox")
        lox.execute(script.readText())

        val source = outContent.toString()
        val scanner = Scanner(source)

        val actual: MutableList<Int> = ArrayList()
        while (scanner.hasNextInt())
            actual += scanner.nextInt()

        assertEquals(
            listOf(
                1, 13, 12, -6, 18, 4,
                511,
                // 1
                14, 56, 7, 263, 123123123,
                1023, 991, 927,
                927, 927, 927,
                136, 243, 434,
                8912896, 3888, 868,
                8912896, 0, 434,
                4, 3, 2,
                -12, 16, -13,
                17472, -1456, -91,
                364, 48, -30,
                28, -42, -90,
                65536, 16, 4,
                // 2
                14, 56, 7, 263, 123123123,
                1023, 991, 927,
                927, 927, 927,
                136, 243, 434,
                8912896, 3888, 868,
                8912896, 0, 434,
                4, 3, 2,
                -12, 16, -13,
                17472, -1456, -91,
                364, 48, -30,
                28, -42, -90,
                65536, 16, 4,
            ),
            actual
        )
    }
}