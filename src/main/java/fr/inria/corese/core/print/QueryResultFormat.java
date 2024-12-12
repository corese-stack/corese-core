package fr.inria.corese.core.print;

import fr.inria.corese.core.sparql.triple.parser.Context;

/**
 * Root class for Result Format
 */
public class QueryResultFormat {

    private Context context;
    
    QueryResultFormat() {}
    
    public QueryResultFormat init(Context c) {
        setContext(c);
        return this;
    }
    
    /**
     * in service: param=sv:unselect~var   -- var name without "?"
     * context manage unselect variable list
     * if var is unselect, do not pprint var value in result
     */
    public boolean accept(String variable) {
        if (variable.startsWith("?") || variable.startsWith("$")) {
            variable = variable.substring(1);
        }
        if (getContext() == null) {
            return true;
        }        
        return getContext().acceptVariable(variable);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
    
    
}
