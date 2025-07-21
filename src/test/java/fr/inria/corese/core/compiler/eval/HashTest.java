package fr.inria.corese.core.compiler.eval;

import static org.junit.Assert.*;

public class HashTest {
    @org.junit.Test
    public void test1()
    {
        Hash hash = new Hash( "abc" );
        assertNotNull( hash );
    }
}
