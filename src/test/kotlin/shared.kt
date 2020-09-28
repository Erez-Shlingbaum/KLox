import lox.Lox
import org.junit.After
import org.junit.Before
import java.io.ByteArrayOutputStream
import java.io.PrintStream

const val resourceDir = "src/test/resources"

open class LoxTest {
    protected val lox = Lox()

    protected val outContent = ByteArrayOutputStream()
    protected val errContent = ByteArrayOutputStream()
    protected val originalOut: PrintStream = System.out
    protected val originalErr: PrintStream = System.err

    @Before
    fun setUpStreams() {
        System.setOut(PrintStream(outContent))
        System.setErr(PrintStream(errContent))
    }

    @After
    fun restoreStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }


}