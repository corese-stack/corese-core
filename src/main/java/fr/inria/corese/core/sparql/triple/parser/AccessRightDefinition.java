package fr.inria.corese.core.sparql.triple.parser;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.sparql.triple.parser.AccessRight.AccessRights;
import fr.inria.corese.core.sparql.triple.parser.AccessRight.AccessModes;

import java.util.HashMap;

/**
 * Access right granted for specific URI or namespace
 * It **overloads** the default access right given by AccessRight
 * It may increase or reduce the default access right
 * Semantics: access right given to the user for insert and delete
 * 
 * @author Olivier Corby, INRIA 2020
 */
public class AccessRightDefinition {
    
    private static AccessRightDefinition singleton;
    
    private AccessMap nodeAccess;
    private AccessMap graphAccess;
    private AccessMap predicateAccess;
    
    private boolean debug = false;
    private boolean inheritDefault = false;
    
    
    
    static {
        setSingleton(new AccessRightDefinition());
    }
    
    public class AccessMap extends HashMap<String, AccessRights> {


        /**
         * return URI access right if any
         * otherwise return namespace(URI) access right if any
         * otherwise return null
         * @return
         */
        AccessRights getAccess(Node node) {
            if (isEmpty()) {
                return null;
            }
            AccessRights b = get(node.getLabel());
            if (b != null) {
                return b;
            }
            String ns = namespace(node);
            return get(ns);
        }
        
        void inherit(AccessMap map) {
            for (String key : map.keySet()) {
                put(key, map.get(key));
            }
        }
        
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (! getNode().isEmpty()) {
            sb.append("node:");
            sb.append(getNode());
        }
        if (! getPredicate().isEmpty()) {
            sb.append("predicate:");
            sb.append(getPredicate());
        }
        if (! getGraph().isEmpty()) {
            sb.append("graph:");
            sb.append(getGraph());
        }
        return sb.toString();
    }
    
    public AccessRightDefinition() {
        init();
    }
    
    void init() {
        setNodeAccess(new AccessMap());
        setGraphAccess(new AccessMap());
        setPredicateAccess(new AccessMap());
    }
    
    
    public void inheritDefault() {
        inherit(getSingleton());
        setInheritDefault(true);
    }
    
    
    public void inherit(AccessRightDefinition acc) {
        getNodeAccess().inherit(acc.getNodeAccess());
        getPredicateAccess().inherit(acc.getPredicateAccess());
        getGraphAccess().inherit(acc.getGraphAccess());
    }
       
    int size() {
        return getNode().size() + getPredicate().size() + getGraph().size();
    }
    
    
    /**
     * def is the default access right granted
     * res is the URI|namespace access right granted for edge
     * @return
     */
    AccessRights getAccess(Edge edge, AccessRights def) {
        AccessRights res = getAccess(edge);
        if (res == null) {
            return def;
        }
        return res;
    }
    
    AccessRights getAccess(Edge edge) {
       return getAccessDirect(edge);
    }
    
    /**
     * URI of default may overload namespace of current (if current has no URI)
     */
    AccessRights getAccessDirect(Edge edge) {
        if (size() == 0) {
            return null;
        }
        return combine(getSubject(edge), combine(getObject(edge), combine(getPredicate(edge), getGraph(edge))));
    }  


    AccessRights get(AccessRights current, AccessRights defaut) {
        return (current == null) ? defaut : current;
    }


    AccessRights getAccessOrDefault(Edge edge) {
        AccessRights res = getAccessBasic(edge);
        if (res == null) {
            return getSingleton().getAccessBasic(edge);
        }
        return res;
    }

    AccessRights getAccessBasic(Edge edge) {
        if (size() > 0) {
            AccessRights node   = combine(getSubject(edge),   getObject(edge));
            AccessRights access = combine(getPredicate(edge), getGraph(edge));
            return combine(node, access);
        }
        return null;
    }
    
    AccessModes getMode() {
        return AccessRight.getMode();
    }

    
    AccessRights combine(AccessRights b1, AccessRights b2) {
        if (getMode() == AccessModes.BI_MODE) {
            return combineBinary(b1, b2);
        }
        return moreRestricted(b1, b2);
    }

    AccessRights combineBinary(AccessRights b1, AccessRights b2) {
        if (b1 == null) {
            return b2;
        }
        if (b2 == null) {
            return b1;
        }
        return AccessRight.getLevel((byte) (b1.getByteValue() | b2.getByteValue())) ;
    }


    AccessRights moreRestricted(AccessRights b1, AccessRights b2) {
        if (b1 == null) {
            return b2;
        }
        if (b2 == null) {
            return b1;
        }
        return (b1.getByteValue() > b2.getByteValue()) ? b1 : b2;
    }


  
    // return null when there is no uri access right
    AccessRights getPredicate(Edge edge) {
        return getPredicate().getAccess(edge.getProperty());
    }

    AccessRights getGraph(Edge edge) {
         if (edge.getGraph() == null) {
            return null;
        }
        return getGraph().getAccess(edge.getGraph());
    }

    AccessRights getSubject(Edge edge) {
        return getNode().getAccess(edge.getNode(0));
    }

    AccessRights getObject(Edge edge) {
        return getNode().getAccess(edge.getNode(1));
    }
    
   
    
    String namespace(Node node) {
        return NSManager.namespace(node.getLabel());
    }
    
     /**
     * @return the nodeAccess
     */
    public AccessMap getNode() {
        return getNodeAccess();
    }


    /**
     * @return the graphAccess
     */
    public AccessMap getGraph() {
        return getGraphAccess();
    }


    /**
     * @return the predicateAccess
     */
    public AccessMap getPredicate() {
        return getPredicateAccess();
    }


    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return the singleton
     */
    public static AccessRightDefinition getSingleton() {
        return singleton;
    }

    /**
     * @param aSingleton the singleton to set
     */
    public static void setSingleton(AccessRightDefinition aSingleton) {
        singleton = aSingleton;
    }

    /**
     * @return the nodeAccess
     */
    public AccessMap getNodeAccess() {
        return nodeAccess;
    }

    /**
     * @param nodeAccess the nodeAccess to set
     */
    public void setNodeAccess(AccessMap nodeAccess) {
        this.nodeAccess = nodeAccess;
    }

    /**
     * @return the graphAccess
     */
    public AccessMap getGraphAccess() {
        return graphAccess;
    }

    /**
     * @param graphAccess the graphAccess to set
     */
    public void setGraphAccess(AccessMap graphAccess) {
        this.graphAccess = graphAccess;
    }

    /**
     * @return the predicateAccess
     */
    public AccessMap getPredicateAccess() {
        return predicateAccess;
    }

    /**
     * @param predicateAccess the predicateAccess to set
     */
    public void setPredicateAccess(AccessMap predicateAccess) {
        this.predicateAccess = predicateAccess;
    }


    /**
     * @param inheritDefault the inheritDefault to set
     */
    public void setInheritDefault(boolean inheritDefault) {
        this.inheritDefault = inheritDefault;
    }
    
    
    
    
    
    
}
