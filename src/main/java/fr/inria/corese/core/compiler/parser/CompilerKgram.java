package fr.inria.corese.core.compiler.parser;

import fr.inria.corese.core.kgram.api.core.Filter;
import fr.inria.corese.core.kgram.api.core.*;
import fr.inria.corese.core.kgram.tool.Message;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Proxy between KGRAM compiler and corese.core.compiler.
 * Use KGRAM graph and filter
 *
 * @author corby
 */
public class CompilerKgram implements ExpType, Compiler {
    static final String EQUAL = "=";
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CompilerKgram.class);
    ASTQuery ast;
    EdgeImpl edge;

    HashMap<String, Node> varTable;
    HashMap<String, Node> resTable;
    HashMap<String, Node> bnodeTable;

    public CompilerKgram() {
        varTable = new HashMap<>();
        resTable = new HashMap<>();
        bnodeTable = new HashMap<>();
    }

    public static CompilerKgram create() {
        return new CompilerKgram();
    }

    @Override
    public HashMap<String, Node> getVarTable() {
        return varTable;
    }

    @Override
    public void share(Compiler cp) {
        varTable = cp.getVarTable();
    }


    @Override
    public void setAST(ASTQuery ast) {
        this.ast = ast;
    }


    @Override
    public List<Filter> compileFilter(Expression exp) throws EngineException {
        ArrayList<Filter> list = new ArrayList<>();
        compile(exp, list);
        return list;
    }

    void compile(Expression exp, List<Filter> list) throws EngineException {
        if (exp.isAnd()) {
            for (Expression e : exp.getArgs()) {
                compile(e, list);
            }
        } else {
            Filter f = compile(exp);
            if (f != null) list.add(f);
        }
    }


    /**
     * Generate one filter
     */
    @Override
    public Filter compile(Expression exp) throws EngineException {
        Expression ee = process(exp);
        Expression cpl = ee.process(ast);
        if (cpl == null) {
            logger.warn(Message.Prefix.REWRITE.getString(), exp, ast);
            cpl = exp;
        }
        cpl.compile(ast);
        return cpl;
    }

    /**
     * xpath() = exp
     * ->
     * exp in list(xpath())
     */
    Expression process(Expression exp) {
        if (exp.isTerm() &&
                exp.getName().equals(EQUAL) &&
                exp.getArg(0).isFunction() &&
                exp.getArg(0).getName().equals(Processor.XPATH)) {
            Term list = Term.list();
            list.add(exp.getArg(0));
            return Term.create(Processor.IN, exp.getArg(1), list);
        }
        return exp;
    }

    Node getNode(Atom at) {
        return getNode(at, false);
    }

    NodeImpl getNodeImpl(Atom at) {
        return (NodeImpl) getNode(at, false);
    }

    @Override
    public Node createNode(Atom at, boolean isReuse) {
        return getNode(at, isReuse);
    }


    /**
     * isReuse = true means reuse existing Node if any
     * Generate a new Node for resources and for literals
     * For resources it is to enable to approximate match a query Class with
     * different target classes
     */
    Node getNode(Atom at, boolean isReuse) {

        if (at.isVariable()) {
            Node node = varTable.get(at.getName());
            if (node == null) {
                node = new NodeImpl(at);
                varTable.put(at.getName(), node);
            }
            return node;
        } else if (at.isResource() && isReuse) {
            Node node = resTable.get(at.getName());
            if (node == null) {
                node = new NodeImpl(at);
                resTable.put(at.getName(), node);
            }
            return node;
        } else if (at.isBlank()) {
            Node node = bnodeTable.get(at.getName());
            if (node == null) {
                node = new NodeImpl(at);
                bnodeTable.put(at.getName(), node);
            }
            return node;
        }
        return new NodeImpl(at);
    }

    @Override
    public Collection<Node> getVariables() {
        return varTable.values();
    }

    @Override
    public Edge compile(Triple tt, boolean reuse) {
        return compile(tt, reuse, false);
    }

    /**
     * when rec = true:
     * when triple(t p o) where t is triple reference of triple(a q b t) ->
     * recursively compile triple(a q b t)
     * use case: values ?t {<<<<a q b>> p o>>}
     */

    Node getNodeRec(Atom at, boolean reuse, boolean rec) {
        Node node = getNode(at, reuse);
        if (rec && at.isTriple() && at.getTriple() != null &&
                node.getEdge() == null) {
            Edge complileddge = compile(at.getTriple(), reuse, rec);
            complileddge.setCreated(true);
            node.setEdge(complileddge);
        }
        return node;
    }

    @Override
    public Edge compile(Triple triple, boolean reuse, boolean rec) {
        EdgeImpl compiledEdge = new EdgeImpl(triple);
        compiledEdge.setCreated(rec);
        Node subject = getNodeRec(triple.getSubject(), reuse, rec);
        if (triple.getVariable() != null) {
            Node variable = getNode(triple.getVariable());
            compiledEdge.setEdgeVariable(variable);
        }
        Node predicate = getNode(triple.getProperty(), reuse);
        // PRAGMA:
        // ?x rdf:type c:Image
        // in this case we want each triple rdf:type c:Image to have its own c:Image Node
        // to accept type subsumption
        // if it would be same Node, it would need to be bound to same value
        // TODO: fix it for relax
        Node object = getNodeRec(triple.getObject(), reuse, rec);
        compiledEdge.add(subject);
        compiledEdge.add(object);
        compiledEdge.setEdgeNode(predicate);

        if (triple.getArgs() != null) {
            // tuple(s p o arg1 .. argn)
            for (Atom arg : triple.getArgs()) {
                NodeImpl sup = getNodeImpl(arg);

                if (arg.isVariable()) {
                    sup.setMatchNodeList(arg.getVariable().isMatchNodeList());
                    sup.setMatchCardinality(arg.getVariable().isMatchCardinality());
                }
                if (sup.isTriple()) {
                    // triple(s p o t) where t is triple reference
                    // t points to target edge
                    sup.setEdge(compiledEdge);
                }
                compiledEdge.add(sup);
            }
        }

        return compiledEdge;

    }


    @Override
    public Node createNode(String name) {
        return getNode(new Variable(name));
    }

    @Override
    public Node createNode(Atom at) {
        return getNode(at);
    }

    @Override
    public List<Filter> getFilters() {
        return new ArrayList<>();
    }

    @Override
    public Edge getEdge() {
        return edge;
    }

    @Override
    public Node getNode() {
        return null;
    }

    @Override
    public boolean isFail() {
        return false;
    }


    /*****************************
     *
     * PATH
     *
     * **/

    @Override
    public Regex getRegex(Filter f) throws EngineException {
        Expression exp = (Expression) f;
        if (exp.isFunction(Processor.MATCH)) {
            Expression regex = exp.getArg(1);
            regex.compile(ast);
            return regex;
        }
        return null;
    }

    @Override
    public String getMode(Filter f) {
        Expression exp = (Expression) f;
        if (exp.isFunction(Processor.MATCH) && exp.getArity() == 3) {
            return exp.getArg(2).getLabel();
        }
        return null;
    }

    @Override
    public int getMin(Filter f) {
        Expression exp = (Expression) f;

        if (exp.getArity() == 2 && exp.getArg(1).isConstant()) {
            Constant cst = (Constant) exp.getArg(1);
            if (exp.getName().equals(">=") || exp.getName().equals("="))
                return cst.getDatatypeValue().intValue();
            else if (exp.getName().equals(">"))
                return cst.getDatatypeValue().intValue() + 1;
        }
        return -1;
    }

    @Override
    public int getMax(Filter f) {

        Expression exp = (Expression) f;

        if (exp.getArity() == 2 && exp.getArg(1).isConstant()) {
            Constant cst = (Constant) exp.getArg(1);
            if (exp.getName().equals("<=") || exp.getName().equals("="))
                return cst.getDatatypeValue().intValue();
            else if (exp.getName().equals("<"))
                return cst.getDatatypeValue().intValue() - 1;
        }
        return -1;
    }


}
