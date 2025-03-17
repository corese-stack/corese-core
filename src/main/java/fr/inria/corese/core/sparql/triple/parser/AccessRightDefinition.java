package fr.inria.corese.core.sparql.triple.parser;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
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
    
    public class AccessMap extends HashMap<String, AccessRight.AccessRights> {

        public AccessMap define(String uri, AccessRight.AccessRights b) {
            put(uri, b);
            return this;
        }

        /**
         * return URI access right if any
         * otherwise return namespace(URI) access right if any
         * otherwise return null
         * @return
         */
        AccessRight.AccessRights getAccess(Node node) {
            if (isEmpty()) {
                return null;
            }
            AccessRight.AccessRights b = get(node.getLabel());
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
    
    public void clear() {
        getNodeAccess().clear();
        getPredicateAccess().clear();
        getGraphAccess().clear();
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
    AccessRight.AccessRights getAccess(Edge edge, AccessRight.AccessRights def) {
        AccessRight.AccessRights res = getAccess(edge);
        if (res == null) {
            return def;
        }
        return res;
    }
    
    AccessRight.AccessRights getAccess(Edge edge) {
       return getAccessDirect(edge);
    }
    
    /**
     * URI of default may overload namespace of current (if current has no URI)
     */
    AccessRight.AccessRights getAccessDirect(Edge edge) {
        if (size() == 0) {
            return null;
        }
        return combine(getSubject(edge), combine(getObject(edge), combine(getPredicate(edge), getGraph(edge))));
    }  

    /**
     * Namespace of current overload URI of default
     */
    AccessRight.AccessRights getAccessWithDefault(Edge edge) {
        AccessRight.AccessRights subject = get(getSubject(edge), getSingleton().getSubject(edge));
        AccessRight.AccessRights object = get(getObject(edge), getSingleton().getObject(edge));
        AccessRight.AccessRights pred = get(getPredicate(edge), getSingleton().getPredicate(edge));
        AccessRight.AccessRights graph = get(getGraph(edge), getSingleton().getGraph(edge));
        return combine(subject, combine(object, combine(pred, graph)));
    }

    AccessRight.AccessRights get(AccessRight.AccessRights current, AccessRight.AccessRights defaut) {
        return (current == null) ? defaut : current;
    }

    AccessRight.AccessRights getAccess2(Edge edge, AccessRight.AccessRights def) {
        AccessRight.AccessRights res = getAccessOrDefault(edge);
        if (res == null) {
            return def;
        }
        return res;
    }

    AccessRight.AccessRights getAccessOrDefault(Edge edge) {
        AccessRight.AccessRights res = getAccessBasic(edge);
        if (res == null) {
            return getSingleton().getAccessBasic(edge);
        }
        return res;
    }

    AccessRight.AccessRights getAccessBasic(Edge edge) {
        if (size() > 0) {
            AccessRight.AccessRights node   = combine(getSubject(edge),   getObject(edge));
            AccessRight.AccessRights access = combine(getPredicate(edge), getGraph(edge));
            return combine(node, access);
        }
        return null;
    }
    
    int getMode() {
        return AccessRight.getMode();
    }

    
    AccessRight.AccessRights combine(AccessRight.AccessRights b1, AccessRight.AccessRights b2) {
        if (getMode() == AccessRight.BI_MODE) {
            return combineBinary(b1, b2);
        }
        return moreRestricted(b1, b2);
    }

    AccessRight.AccessRights combineBinary(AccessRight.AccessRights b1, AccessRight.AccessRights b2) {
        if (b1 == null) {
            return b2;
        }
        if (b2 == null) {
            return b1;
        }
        return AccessRight.getLevel((byte) (b1.getByteValue() | b2.getByteValue())) ;
    }


    AccessRight.AccessRights moreRestricted(AccessRight.AccessRights b1, AccessRight.AccessRights b2) {
        if (b1 == null) {
            return b2;
        }
        if (b2 == null) {
            return b1;
        }
        return (b1.getByteValue() > b2.getByteValue()) ? b1 : b2;
    }

    AccessRight.AccessRights lessRestricted(AccessRight.AccessRights b1, AccessRight.AccessRights b2) {
        if (b1 == null) {
            return b2;
        }
        if (b2 == null) {
            return b1;
        }
        return b1.getByteValue() < b2.getByteValue() ? b1 : b2;
    }

  
    // return null when there is no uri access right
    AccessRight.AccessRights getPredicate(Edge edge) {
        return getPredicate().getAccess(edge.getProperty());
    }

    AccessRight.AccessRights getGraph(Edge edge) {
         if (edge.getGraph() == null) {
            return null;
        }
        return getGraph().getAccess(edge.getGraph());
    }

    AccessRight.AccessRights getSubject(Edge edge) {
        return getNode().getAccess(edge.getNode(0));
    }

    AccessRight.AccessRights getObject(Edge edge) {
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
     * @param nodeAccess the nodeAccess to set
     */
    public void setNode(AccessMap nodeAccess) {
        this.setNodeAccess(nodeAccess);
    }

    /**
     * @return the graphAccess
     */
    public AccessMap getGraph() {
        return getGraphAccess();
    }

    /**
     * @param graphAccess the graphAccess to set
     */
    public void setGraph(AccessMap graphAccess) {
        this.setGraphAccess(graphAccess);
    }

    /**
     * @return the predicateAccess
     */
    public AccessMap getPredicate() {
        return getPredicateAccess();
    }

    /**
     * @param predicateAccess the predicateAccess to set
     */
    public void setPredicate(AccessMap predicateAccess) {
        this.setPredicateAccess(predicateAccess);
    }

    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
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
     * @return the inheritDefault
     */
    public boolean isInheritDefault() {
        return inheritDefault;
    }

    /**
     * @param inheritDefault the inheritDefault to set
     */
    public void setInheritDefault(boolean inheritDefault) {
        this.inheritDefault = inheritDefault;
    }
    
    
    
    
    
    
}
