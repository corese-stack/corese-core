package fr.inria.corese.core.next.api.model.impl.corese;

import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.ValueFactoryTest;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.*;

public class CoreseAdaptedValueFactoryTest extends ValueFactoryTest {

    @Before
    @Override
    public void setUp() {
        this.valueFactory = new CoreseAdaptedValueFactory();
    }

    @Test
    @Override
    public void testCreateLiteralTemporalAmount() {
        Duration duration = Duration.ofHours(23);
        this.valueFactory.createLiteral(duration);

        assertNotNull(this.valueFactory.createLiteral(duration));
    }

    @Test
    public void testConstructorWithBoolean() {
        Literal booleanLiteral = this.valueFactory.createLiteral(true);
        assertNotNull(booleanLiteral);
        assertTrue(booleanLiteral.booleanValue());
    }
}
