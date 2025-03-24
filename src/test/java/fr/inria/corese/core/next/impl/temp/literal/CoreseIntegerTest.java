package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseNumber;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.*;

public class CoreseIntegerTest extends AbstractCoreseNumberTest {


    @Override
    protected AbstractCoreseNumber createNumber(String stringValue) {
        return new CoreseInteger(stringValue);
    }

    @Override
    @Test
    public void getCoreseNode() {
        CoreseInteger coreseInteger = new CoreseInteger(1);
        assertNotNull(coreseInteger.getCoreseNode());
        assertTrue(coreseInteger.getCoreseNode() instanceof CoreseNumber);
    }
}