package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EvalJoinTest {
    private static final int LEVEL = 3;
    private final Eval engine = mock(Eval.class);
    private final Memory environment = mock(Memory.class);
    private final Producer producer = mock(Producer.class);
    private final Stack stack = mock(Stack.class);
    private final Node variable = mock(Node.class);
    private final Node value = mock(Node.class);
    private final Mapping leftRow = mock(Mapping.class);
    private final Mapping rightRow = mock(Mapping.class);
    private final EvalJoin join = new EvalJoin(engine);

    EvalJoinTest() {
        when(engine.getMemory()).thenReturn(environment);
        when(environment.push(any(Mapping.class), eq(LEVEL))).thenReturn(true);
        when(leftRow.getNode(variable)).thenReturn(value);
        when(rightRow.getNode(variable)).thenReturn(value);
        when(value.match(value)).thenReturn(true);
    }

    @ParameterizedTest
    @CsvSource({"true, -2", "true, 2", "true, 3", "false, -2", "false, 2", "false, 3"})
    void preservesBacktrackingAndBagMultiplicity(boolean sharedVariable, int continuation) {
        Mappings left = rows(List.of(leftRow, leftRow));
        Mappings right = rows(List.of(rightRow, rightRow, rightRow));
        when(right.find(value, variable)).thenReturn(0);
        when(engine.eval(producer, null, stack, LEVEL + 1)).thenReturn(continuation);

        int actual = sharedVariable
                ? join.joinWithCommonVariable(variable, producer, null, stack, environment, left, right, LEVEL)
                : join.joinWithoutCommonVariable(producer, null, stack, environment, left, right, LEVEL);

        assertEquals(continuation, actual);
        int pairs = continuation < LEVEL ? 1 : 6;
        verify(engine, times(pairs)).eval(producer, null, stack, LEVEL + 1);
        verify(environment, times(pairs)).pop(rightRow);
        verify(environment, times(continuation < LEVEL ? 0 : 2)).pop(leftRow);
    }

    @Test
    void unboundPrefixBackjumpDoesNotContinueIntoBoundCandidates() {
        Mapping unbound = mock(Mapping.class);
        Mappings right = rows(List.of(unbound, rightRow));
        when(engine.eval(producer, null, stack, LEVEL + 1)).thenReturn(LEVEL - 1);

        int actual = join.joinWithCommonVariable(variable, producer, null, stack, environment,
                rows(List.of(leftRow)), right, LEVEL);

        assertEquals(LEVEL - 1, actual);
        verify(environment).pop(unbound);
        verify(environment, never()).push(rightRow, LEVEL);
        verify(right, never()).find(value, variable);
    }

    @Test
    void absentCandidateDoesNotBackjumpBeforeTheNextLeftRow() {
        Mapping otherLeft = mock(Mapping.class);
        when(otherLeft.getNode(variable)).thenReturn(value);
        Mappings right = rows(List.of(rightRow, rightRow));
        when(right.find(value, variable)).thenReturn(-1, 0);
        when(engine.eval(producer, null, stack, LEVEL + 1)).thenReturn(LEVEL);

        int actual = join.joinWithCommonVariable(variable, producer, null, stack, environment,
                rows(List.of(leftRow, otherLeft)), right, LEVEL);

        assertEquals(LEVEL, actual);
        verify(environment).pop(leftRow);
        verify(environment).pop(otherLeft);
        verify(engine, times(2)).eval(producer, null, stack, LEVEL + 1);
    }

    private static Mappings rows(List<Mapping> rows) {
        Mappings mappings = mock(Mappings.class);
        when(mappings.size()).thenReturn(rows.size());
        when(mappings.iterator()).thenAnswer(invocation -> rows.iterator());
        for (int index = 0; index < rows.size(); index++) {
            when(mappings.get(index)).thenReturn(rows.get(index));
        }
        return mappings;
    }
}
