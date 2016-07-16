package at.nonblocking.cliwix.model;


import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordTest {

    @Test
    public void generateSHA1HashTest() throws Exception {
        assertEquals("qUqP5cyxm6YcTAhz05Hph5gvu9M=", Password.generateSHA1Hash("test"));
    }

}
