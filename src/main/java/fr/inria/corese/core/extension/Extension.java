package fr.inria.corese.core.extension;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.logic.Distance;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.IDatatypeList;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Access.Level;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.context.ContextLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generic Java Extension function public class
 * prefix fun: &lt;function://fr.inria.corese.core.extension.Extension>
 * fun:test(xt:graph())
 * <p>
 * Provide access to query execution environment
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2020
 */
public class Extension extends Core {
    private static final Logger logger = LoggerFactory.getLogger(Extension.class);

    Binding getBinding() {
        if (getEnvironment() == null) {
            return null;
        }
        return getEnvironment().getBind();
    }

    // inherit access level from Binding
    Context getCreateContext() {
        Binding b = getBinding();
        if (b == null) {
            return new Context(Level.DEFAULT);
        }
        return new Context(b.getAccessLevel());
    }

    ContextLog getLog() {
        return getBinding().getLog();
    }

    ASTQuery parseQuery(String str, Context c) throws EngineException {
        QueryProcess exec = QueryProcess.create();
        Query q = exec.compile(str, c);
        return q.getAST();
    }


    IDatatype list(Edge e) {
        IDatatypeList list = DatatypeMap.newList(
                e.getSubjectValue(), e.getPredicateValue(), e.getObjectValue());
        return DatatypeMap.newList(list);
    }

    // Mappings as list(list(var, val))
    IDatatype list(Mappings map) {
        IDatatypeList list = DatatypeMap.newList();
        for (Mapping m : map) {
            list.addAll(m.getDatatypeList());
        }
        return list;
    }

    // Mapping as list(list(var, val))
    IDatatype list(Mapping m) {
        return m.getDatatypeList();
    }


    public IDatatype distance(IDatatype dt1, IDatatype dt2, IDatatype dt) {
        Graph g = getGraph();
        Node n1 = g.getNode(dt1);
        Node n2 = g.getNode(dt2);
        if (n1 == null || n2 == null) {
            return getValue(Integer.MAX_VALUE);
        }

        Distance distance = g.getClassDistance();
        if (distance == null || (dt != null && !distance.getSubClassOf().equals(dt.getLabel()))) {
            distance = new Distance(g);
            distance.setStep(1);
            if (dt != null) {
                distance.setSubClassOf(dt.getLabel());
            }
            distance.start();
            g.setClassDistance(distance);
        }
        double dd = distance.distance(n1, n2);
        return getValue(dd);
    }


    ProcessVisitor getVisitor() {
        return getEval().getVisitor();
    }

    /**
     * Accessor
     * fun:visitor()
     */
    public IDatatype visitor() {
        return cast(getVisitor());
    }

    Query getQuery() {
        return getEnvironment().getQuery();
    }

    ASTQuery getAST() {
        return getEnvironment().getQuery().getAST();
    }

    IDatatype cast(Object obj) {
        return DatatypeMap.getValue(obj);
    }


    public IDatatype fib(IDatatype n) {
        switch (n.intValue()) {
            case 0:
            case 1:
                return n;
            default:
                return fib(n.minus(DatatypeMap.ONE)).plus(fib(n.minus(DatatypeMap.TWO)));
        }
    }


    int fib(int n) {
        switch (n) {
            case 0:
            case 1:
                return n;
            default:
                return fib(n - 1) + fib(n - 2);
        }
    }


}
