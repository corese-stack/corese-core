package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackTest {
    @Test
    void preservesListEqualityAndHashLookup() {
        Stack stack = Stack.create(Exp.create(Exp.Type.AND));
        List<Exp> list = new ArrayList<>(stack);

        assertEquals(list, stack);
        assertEquals(stack, list);
        assertEquals(list.hashCode(), stack.hashCode());
        assertTrue(new HashSet<>(List.of(stack)).contains(list));
        assertTrue(new HashSet<>(List.of(list)).contains(stack));
    }
}
