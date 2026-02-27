package fr.inria.corese.core.next.query.impl.sparql.options;

import java.util.List;

public interface ErrorHandlingOptions {

    /**
     * if true stop the parsing and throw an exception
     * @return
     */
    boolean isFailFast();

    /**
     * if true collect the error in a List
     * @return
     */
    boolean isCollectErrors();
    List<String> getErrors();
}
