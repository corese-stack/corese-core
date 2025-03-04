package fr.inria.corese.core.next.api.model.impl.corese;

import fr.inria.corese.core.next.api.model.ValueFactoryTest;
import org.junit.Before;

public class CoreseAdaptedValueFactoryTest extends ValueFactoryTest {

    @Before
    @Override
    public void setUp() {
        this.valueFactory = new CoreseAdaptedValueFactory();
    }
}
