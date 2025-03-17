package fr.inria.corese.core.api;

import java.util.List;


public interface Log {

    void reset();

    void log(Log.Operation type, Object obj);

    void log(Log.Operation type, Object obj1, Object obj2);

    List<Object> get(Log.Operation type);

    enum Operation {
        LOAD, QUERY, UPDATE
    }


}
