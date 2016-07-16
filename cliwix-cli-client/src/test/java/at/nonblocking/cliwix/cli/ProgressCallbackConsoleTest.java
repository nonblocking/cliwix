package at.nonblocking.cliwix.cli;


import org.junit.Test;
import static org.junit.Assert.*;

public class ProgressCallbackConsoleTest {

    @Test
    public void test1() {
        StringBufferConsole console = new StringBufferConsole();

        ProgressCallback progressCallback = new ProgressCallbackConsoleImpl(console);

        progressCallback.bytesTransferred(222512, 2345678);

        assertEquals("Transferred: 217kB/2MB (9%)\n", console.getBufferAsString());
    }

}
