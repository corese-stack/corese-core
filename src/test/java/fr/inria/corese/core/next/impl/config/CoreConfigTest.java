package fr.inria.corese.core.next.impl.config;

import org.junit.Assert;
import org.junit.Test;

public class CoreConfigTest {

    @Test
    public void instantiationTest(){
        Assert.assertEquals("INFO", CoreConfig.getInstance().logLevel);
    }
}
