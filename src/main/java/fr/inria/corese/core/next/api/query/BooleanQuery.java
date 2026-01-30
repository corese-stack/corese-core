package fr.inria.corese.core.next.api.query;

public interface BooleanQuery extends Query<Boolean> {

    Boolean evaluate();

    BooleanQuery setBinding(String name, Object value);
    BooleanQuery clearBindings();
}