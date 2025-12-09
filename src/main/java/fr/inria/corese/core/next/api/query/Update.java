package fr.inria.corese.core.next.api.query;

public interface Update extends Operation {

    void execute();

    Update setBinding(String name, Object value);

}
