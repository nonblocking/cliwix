package at.nonblocking.cliwix.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class StringBufferConsole implements Console {

    private StringBuffer buffer = new StringBuffer();

    @Override
    public void println(String message) {
        buffer.append(message + "\n");
    }

    @Override
    public void printlnError(String message) {
        buffer.append(message + "\n");
    }

    @Override
    public void printStacktrace(Throwable throwable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        throwable.printStackTrace(ps);
        ps.flush();
        printlnError(baos.toString());
    }

    @Override
    public void exit(int status) {
        if (status != 0) {
            System.err.println(getBufferAsString());
            throw new ExitStatusFailureException();
        }
    }

    public String getBufferAsString() {
        return buffer.toString();
    }
}
