package fr.inria.corese.core.next.api.query;

public interface BooleanQuery extends Query {

    boolean evaluate();

    BooleanQuery setBinding(String name, Object value);
    BooleanQuery clearBindings();
}