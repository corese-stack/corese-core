package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.query.api.result.Binding;

public class CoreseBinding implements Binding {
    private final String varName;
    private final Value bindingValue;

    public CoreseBinding(String variable, Value value) {
        this.varName = variable;
        this.bindingValue = value;
    }

    @Override
    public String name() {
        return this.varName;
    }

    @Override
    public Value value() {
        return this.bindingValue;
    }

}
