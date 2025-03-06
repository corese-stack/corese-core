package fr.inria.corese.core.next.api.model.impl.corese;

import fr.inria.corese.core.next.api.model.ValueFactoryTest;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertThrows;

public class CoreseAdaptedValueFactoryTest extends ValueFactoryTest {

    @Before
    @Override
    public void setUp() {
        this.valueFactory = new CoreseAdaptedValueFactory();
    }

    @Test
    @Override
    public void testCreateLiteralTemporalAmount() {
        assertThrows(UnsupportedOperationException.class, () -> this.valueFactory.createLiteral(Duration.ofDays(1)));
    }
}
