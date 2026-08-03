package fr.inria.corese.core.next.query.kgram.core;

import fr.inria.corese.core.next.query.kgram.api.core.Node;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ExpNodeCollector {

    private List<Node> nodeList;
    private final List<Node> selectNodeList;
    private final List<Node> existNodeList;

    private boolean inSubScope = false;
    private boolean bind = false;
    private boolean blank = false;
    private boolean optional = false;
    private boolean exist = false;
    // when false, return nodes from first exp
    private boolean all = true;

    public ExpNodeCollector() {
        nodeList = new ArrayList<>();
        selectNodeList = new ArrayList<>();
        existNodeList = new ArrayList<>();
    }

    public ExpNodeCollector(boolean inSubScope, boolean bind) {
        this();
        setInSubScope(inSubScope).setBind(bind);
    }

    public ExpNodeCollector copy() {
        ExpNodeCollector h = new ExpNodeCollector();
        h.setAll(isAll()).setBind(isBind())
                .setBlank(isBlank()).setExist(isExist())
                .setInSubScope(isInSubScope());
        return h;
    }

    void add(Node node) {
        if (node != null && (isBlank() || (node.isVariable() && !node.isBlank()))) {
            addDistinct(node);
        }
    }

    void addDistinct(Node node) {
        if (!getNodeList().contains(node)) {
            getNodeList().add(node);
        }
    }

    // add select nodes that are not already in node list
    public List<Node> getNodes() {
        for (Node selectNode : getSelectNodeList()) {
            if (!getNodeList().contains(selectNode)) {
                getNodeList().add(overloadSelectNodeByExistNode(selectNode));
            }
        }

        if (isExist()) {
            // collect exists { } nodes
            for (Node existNode : getExistNodeList()) {
                addDistinct(existNode);
            }
        }

        return getNodeList();
    }

    /**
     * use case:
     * select * where {
     * {select * where {?x foaf:knows ?y}}
     * filter exists {?x foaf:knows ?y} }
     * <p>
     * lNode = {} lSelNode = {?x, ?y} lExistNode = {?x, ?y}
     * overload select nodes of subquery by exists nodes
     * this code would be useful if nodes ?y and ?y were different
     * currently they are the same, hence it is useless
     */
    Node overloadSelectNodeByExistNode(Node node) {
        if (getExistNodeList().contains(node)) {
            return get(getExistNodeList(), node);
        } else {
            return node;
        }
    }

    Node get(List<Node> lNode, Node node) {
        for (Node qNode : lNode) {
            if (qNode.equals(node)) {
                return qNode;
            }
        }
        return null;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public List<Node> getSelectNodeList() {
        return selectNodeList;
    }


    public List<Node> getExistNodeList() {
        return existNodeList;
    }

    public boolean isInSubScope() {
        return inSubScope;
    }

    public ExpNodeCollector setInSubScope(boolean inSubScope) {
        this.inSubScope = inSubScope;
        return this;
    }

    boolean isInSubScopeSample() {
        return isInSubScope() && !isAll();
    }

    public boolean isBind() {
        return bind;
    }

    public ExpNodeCollector setBind(boolean bind) {
        this.bind = bind;
        return this;
    }

    public boolean isBlank() {
        return blank;
    }

    public ExpNodeCollector setBlank(boolean blank) {
        this.blank = blank;
        return this;
    }

    public boolean isOptional() {
        return optional;
    }

    public ExpNodeCollector setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public boolean isExist() {
        return exist;
    }

    public ExpNodeCollector setExist(boolean exist) {
        this.exist = exist;
        return this;
    }

    public boolean isAll() {
        return all;
    }

    public ExpNodeCollector setAll(boolean all) {
        this.all = all;
        return this;
    }

    // return nodes from first exp of BGP
    // use case: compute relevant variable bindings
    public ExpNodeCollector sample() {
        setAll(false);
        return this;
    }

    // return all nodes
    public ExpNodeCollector all() {
        setAll(true);
        return this;
    }

}
