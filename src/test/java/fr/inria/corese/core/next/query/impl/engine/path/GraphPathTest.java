package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class GraphPathTest {
    @Test
    void runsPathEnumerationOnTheWorkerThread() throws InterruptedException {
        PathFinder finder = mock(PathFinder.class);
        Environment environment = mock(Environment.class);
        Node start = mock(Node.class);
        when(finder.getIndex()).thenReturn(1);
        when(finder.get(environment, 1)).thenReturn(start);
        GraphPath worker = new GraphPath(finder, environment);

        worker.start();
        worker.join(2000);

        assertFalse(worker.isAlive());
        verify(finder).process(start, environment);
    }
}
