package fr.inria.corese.core.next.impl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class CoreConfigTest {

    @Test
    public void instantiationTest(){
        assertEquals("INFO", CoreConfig.getInstance().getLogLevel());
    }
}
