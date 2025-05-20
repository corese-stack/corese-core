package fr.inria.corese.core.compiler.eval;

import static org.junit.jupiter.api.Assertions.*;

public class HashTest {
    @org.junit.jupiter.api.Test
    public void test1()
    {
        Hash hash = new Hash( "abc" );
        assertNotNull( hash );
    }
}
