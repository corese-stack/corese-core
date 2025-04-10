package fr.inria.corese.core.next.api.model.impl.corese;

import fr.inria.corese.core.next.api.model.ValueFactoryTest;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertNotNull;
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
        Duration duration = Duration.ofHours(23);
        this.valueFactory.createLiteral(duration);

        assertNotNull(this.valueFactory.createLiteral(duration));
    }
}
