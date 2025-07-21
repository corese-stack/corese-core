package fr.inria.corese.core.query;

import fr.inria.corese.core.api.Log;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Log activity (load, query, etc.)
 *
 * @author Olivier Corby, Edelweiss INRIA 2011
 */
public class LogImpl implements Log {

    static final String DEFAULT_FILE = "/tmp/kgram_";

    ArrayList<Object> load, query, update;

    String file;

    boolean isLoad = true,
            isQuery = true,
            isUpdate = true;

    LogImpl() {
        load = new ArrayList<Object>();
        query = new ArrayList<Object>();
        update = new ArrayList<Object>();
        file = DEFAULT_FILE + new Date();
    }

    public static LogImpl create() {
        return new LogImpl();
    }

    public void reset() {
        load.clear();
        query.clear();
        update.clear();
    }

    public void setQuery(boolean b) {
        isQuery = b;
    }

    public void setUpdate(boolean b) {
        isUpdate = b;
    }

    public void setLoad(boolean b) {
        isLoad = b;
    }

    public void setActive(boolean b) {
        setLoad(b);
        setQuery(b);
        setUpdate(b);
    }

    public void log(Log.Operation type, Object obj) {
        switch (type) {
            case LOAD:
                load(obj);
                break;
            case QUERY:
                query((Query) obj);
                break;
            case UPDATE:
                update((Query) obj);
                break;
        }
    }

    public void log(Log.Operation type, Object obj1, Object obj2) {
        switch (type) {
            case QUERY:
                query((Query) obj1);
                break;
        }
    }

    public List<Object> get(Log.Operation type) {
        switch (type) {
            case LOAD:
                return load;
            case QUERY:
                return query;
            case UPDATE:
                return update;
        }
        return new ArrayList<Object>();
    }

    void load(Object name) {
        if (isLoad) {
            load.add(name);
        }
    }

    void query(Query q) {
        ASTQuery ast = q.getAST();
        if (isQuery) {
            query.add(ast.getText());
        }
    }

    void update(Query q) {
        ASTQuery ast = q.getAST();
        if (isUpdate) {
            update.add(ast.getText());
        }
    }

}
