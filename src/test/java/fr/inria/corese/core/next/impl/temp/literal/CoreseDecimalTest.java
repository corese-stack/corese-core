package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.sparql.datatype.CoreseNumber;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CoreseDecimalTest  extends AbstractCoreseNumberTest {
    @Override
    protected AbstractCoreseNumber createNumber(String stringValue) {
        return new CoreseDecimal(stringValue);
    }

    @Override
    public void getCoreseNode() {
        CoreseDecimal coreseDecimal = new CoreseDecimal(1.0);
        assertNotNull(coreseDecimal.getCoreseNode());
        assertTrue(coreseDecimal.getCoreseNode() instanceof CoreseNumber);
    }
}
