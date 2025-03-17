package fr.inria.corese.core.sparql.triple.update;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.parser.TopExp;

import java.util.EnumMap;

/**
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 */
public class Update extends TopExp {

    public enum Keyword {
        LOAD, CLEAR, DROP, CREATE, ADD, MOVE, COPY, PROLOG, INSERT, DELETE, COMPOSITE
    }

    static final EnumMap<Keyword, String> NAME = new EnumMap<>(Keyword.class);

    static {
        NAME.put(Keyword.LOAD, "load");
        NAME.put(Keyword.CLEAR, "clear");
        NAME.put(Keyword.DROP, "drop");
        NAME.put(Keyword.CREATE, "create");
        NAME.put(Keyword.ADD, "add");
        NAME.put(Keyword.MOVE, "move");
        NAME.put(Keyword.COPY, "copy");
        NAME.put(Keyword.PROLOG, "prolog");
        NAME.put(Keyword.INSERT, "insert");
        NAME.put(Keyword.DELETE, "delete");
        NAME.put(Keyword.COMPOSITE, "composite");
    }

    Update.Keyword type;
    ASTUpdate astu;
    // Update operation may have a local prolog
    // otherwise use the global one
    private NSManager nsm;

    public boolean isInsert() {
        return false;
    }

    public boolean isDelete() {
        return false;
    }

    public boolean isInsertData() {
        return false;
    }

    public boolean isDeleteData() {
        return false;
    }
    
    public boolean isLoad() {
        return false;
    }

    String title() {
        return NAME.get(type);
    }

    public Update.Keyword type() {
        return type;
    }

    void set(ASTUpdate a) {
        astu = a;
    }

    public ASTUpdate getASTUpdate() {
        return astu;
    }

    public ASTQuery getASTQuery() {
        return astu.getASTQuery();
    }

    public String expand(String name) {
        if (name == null) {
            return null;
        }
        String res = getNSM().toNamespaceB(name);
        return res;
    }

    public Constant getGraphName() {
        return null;
    }
    
    public Constant getGraphNameDeleteInsert() {
        return null;
    }

    public boolean isComposite() {
        return false;
    }

    public boolean isBasic() {
        return false;
    }

    public Composite getComposite() {
        return null;
    }

    public Basic getBasic() {
        return null;
    }

    public void setLocalNSM(NSManager nsm) {
        this.nsm = nsm;
    }

    public NSManager getLocalNSM() {
        return nsm;
    }

    /**
     * Local or global NSM
     *
     */
    public NSManager getNSM() {
        if (getLocalNSM() != null && getLocalNSM().isUserDefine()) {
            return getLocalNSM();
        } else {
            return getGlobalNSM();
        }
    }

    public NSManager getGlobalNSM() {
        return astu.getNSM();
    }
    
}
