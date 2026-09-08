package fr.inria.corese.core.next.query.impl.engine.model;

import java.util.List;

public interface ExpPattern {

    void getVariables(List<String> list);

    void getVariables(List<String> list, boolean excludeLocal);

}
