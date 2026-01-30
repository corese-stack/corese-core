package fr.inria.corese.core.next.api.query;

public interface BooleanQuery extends Query<Boolean> {

    @Override
    Boolean evaluate();
}