package fr.inria.corese.core.next.query.impl.engine.planning;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;

import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.filter.FilterOptimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates expression ordering and places filters after their variables become
 * available. Nested EXISTS patterns are planned with the surrounding query scope.
 *
 * @author Olivier Corby, Wimmics Inria I3S, 2014
 *
 */
public final class QueryPlanner implements ExpType {

    private final HeuristicPatternOrdering sort = new HeuristicPatternOrdering();
    private final QueryPlan query;
    private final FilterOptimizer compiler;
    private Producer prod;
    private QueryPlan statisticsQuery;
    private Node statisticsGraph;

    // Assign sorter according to query plan profile
    public QueryPlanner(QueryPlan q) {
        query = q;
        compiler = new FilterOptimizer(q);
        statisticsQuery = q;
    }

    /**
     * Contract: For each BGP: Sort QueryPlan Edges Insert Filters at first place
     * where variables are bound This must be recursively done for exists {}
     * pattern in filter and bind including select, order by, group by and
     * having
     */
    public void compile(Producer prod) {
        this.setProducer(prod);
        VString bound = new VString();
        compile(getQuery(), bound, false);
    }

    /**
     * FilterOptimizer exists {} pattern when filter contains exists {}.
     */
    void compile(Filter f) {
        compile(f, new VString(), false);
    }

    /**
     * FilterOptimizer additional filters that may contain exists {}.
     */
    void modifier(QueryPlan q) {
        compile(q.getSelectFun());
        compile(q.getOrderBy());
        compile(q.getGroupBy());
        if (q.getHaving() != null) {
            compile(q.getHaving().getFilter());
        }
        for (Filter f : q.getFunList()) {
            compile(f);
        }
    }

    /**
     * Recursively sort edges and filters edges are sorted wrt connection:
     * successive edges share variables if possible In each BGP, filters are
     * inserted at the earliest place where their variables are bound lVar is
     * the List of variables already bound
     */
    Exp compile(Exp exp, VString varList, boolean option) {
        QueryPlan previousQuery = statisticsQuery;
        Node previousGraph = statisticsGraph;
        if (exp.isQuery() && exp.getQuery().isDatasetSpecified()) {
            statisticsQuery = exp.getQuery();
        }
        if (exp.isGraph()) {
            statisticsGraph = exp.getGraphName();
        }
        try {
            return compileExpression(exp, varList, option);
        } finally {
            statisticsQuery = previousQuery;
            statisticsGraph = previousGraph;
        }
    }

    private Exp compileExpression(Exp exp, VString varList, boolean option) {
        Type type = exp.type();
        switch (type) {

            case EDGE, XPATH, EVAL, NODE, GRAPHNODE:
                break;

            case FILTER, BIND:
                // compile inner exists {} if any
                compile(exp.getFilter(), varList, option);
                break;

            case QUERY:
                // match query and subquery
                QueryPlan q = exp.getQuery();
                modifier(q);
                if (!varList.isEmpty()) {
                    // lVar = intersection(select variables,lVar)
                    varList = getSelectVariables(q, varList);
                }
                // fall through - continue with subquery body

            default:

                if (type == Type.OPTIONAL || type == Type.UNION || type == Type.MINUS) {
                    option = true;
                }

               // QueryPlan planning
                exp = queryPlan(exp, varList);

                int size = varList.size();

                // bind graph variable *after* exp sorting
                if (exp.isGraph() && exp.getGraphName().isVariable()) {
                    // Bind graph pattern node variable
                    Node gNode = exp.getGraphName();
                    varList.add(gNode.getLabel());
                }

                for (int i = 0; i < exp.size(); i++) {
                    Exp e = compile(exp.get(i), varList, option);
                    exp.set(i, e);
                    if (exp.isBGPAnd()) {
                        // add exp variable to list of bound variable
                        exp.get(i).addBind(varList);
                    }
                }

                varList.clear(size);

        }
        inScopeNodes(exp);

        return exp;
    }

    VString getSelectVariables(QueryPlan q, VString lVar) {
        VString list = new VString();
        for (Exp ee : q.getSelectFun()) {
            Node node = ee.getNode();
            if (lVar.contains(node.getLabel())) {
                list.add(node.getLabel());
            }
        }
        return list;
    }

    /**
     * Sort exp statements
     */
    Exp queryPlan(Exp exp, VString lVar) {
        // identify remarkable filters such as ?x = <uri>
        // create OPT_BIND(?x = <uri>) store it in FILTER
        List<Exp> lBind = findBindings(exp);
        if (exp.isBGPAnd()) {
            // sort edges wrt connection
            switch (getQuery().getPlanProfile()) {

                case QueryPlan.QP_T0:
                    sortFilter(exp, lVar);
                    break;

                case QueryPlan.QP_BGP, QueryPlan.QP_DEFAULT, QueryPlan.QP_HEURISTICS_BASED:
                    orderPatterns(exp, lVar, lBind);
                    // move filters
                    sortFilter(exp, lVar);
                    setBind(getQuery(), exp);

                    break;

                default:
                    break;
            }
            service(exp);
        }
        return exp;
    }

    private void orderPatterns(Exp exp, List<String> variables, List<Exp> bindings) {
        if (getProducer() instanceof QueryStatistics statistics) {
            sort.sort(statisticsQuery, statisticsGraph, exp, variables, bindings, statistics);
        } else if (getQuery().getPlanProfile() == QueryPlan.QP_HEURISTICS_BASED) {
            sort.sort(exp, bindings, getProducer());
        } else {
            sort.sort(getQuery(), exp, variables, bindings);
        }
    }
    /**
     * Compute and record list of inscope variables
     * that may be bound to evaluate exp in an optimized way
     */
    void inScopeNodes(Exp exp) {
         if (exp.isOptional()) {
            // A optional B
            // variables bound by A
            optional(exp);
        }
        else if (exp.isMinus()) {
            minus(exp);
         }
        else if (exp.isUnion()) {
            union(exp);
        } else if (exp.isGraph()) {
            graph(exp);
        }
        else if (exp.isJoin()) {
            exp.bindNodes();
        }
    }

    void optional(Exp exp){
        exp.first().setNodeList(exp.getInScopeNodes());
        exp.optional();
    }

    // used by Eval subEval() bind()
    void union(Exp exp){
        exp.first().setNodeList(exp.first().getAllInScopeNodes());
        exp.rest().setNodeList(exp.rest().getAllInScopeNodes());
    }

    void minus(Exp exp){
        exp.first().setNodeList(exp.first().getInScopeNodes());
    }

    void graph(Exp exp) {
       exp.setNodeList(exp.getInScopeNodes());
    }

    void setBind(QueryPlan q, Exp exp){
        if (q.isUseBind()){
            exp.setBind();
        }
    }



    void compile(Filter f, VString lVar, boolean opt) {
        compile(f.getExp(), lVar, opt);
    }

    /**
     * FilterOptimizer pattern of exists {} if any
     */
    void compile(Expr exp, VString lVar, boolean opt) {
        if (exp.oper() == ExprType.EXIST) {
            compile(getQuery().getPattern(exp), lVar, opt);
        } else {
            for (Expr ee : exp.getExpList()) {
                compile(ee, lVar, opt);
            }
        }
    }

    void compile(List<Exp> list) {
        for (Exp ee : list) {
            // Case: group by or order by expression containing exists pattern
            if (ee.getFilter() != null) {
                compile(ee.getFilter());
            }
        }
    }

    /**
     * Move filter at place where variables are bound in exp
     */
    @SuppressWarnings({"java:S3776", "java:S127"})
    void sortFilter(Exp exp, VString varList) {
        int size = varList.size();
        List<String> filterVarList;
        List<Exp> done = new ArrayList<>();

        for (int indexFilter = exp.size() - 1; indexFilter >= 0; indexFilter--) {
            // reverse to get them in same order after placement

            Exp filterExp = exp.get(indexFilter);

            if (filterExp.isFilter() && !done.contains(filterExp)) {

                Filter filter = filterExp.getFilter();

                if (compiler.isLocal(filter)) {
                    // optional {} !bound()
                    // !bound() should not be moved
                    done.add(filterExp);
                    continue;
                }

                varList.clear(size);

                filterVarList = filter.getVariables();
                boolean isExist = filter.getExp().isRecExist();

                for (int indexExp = 0; indexExp < exp.size(); indexExp++) {
                    // search exp e after which filter f is bound
                    Exp e = exp.get(indexExp);

                    e.share(filterVarList, varList);
                    boolean bound = getQuery().bound(filterVarList, varList);

                    if (bound || (isExist && indexExp + 1 == exp.size())) {
                        // insert filter after exp
                        // an exist filter that is not bound is moved at the end because it
                        // may bound its own variables.
                        if (bound && e.isOptional() && e.first().size() > 0) {
                            // add filter in first arg of optional
                            e.first().add(filter);
                        }
                        e.addFilter(filter);
                        done.add(filterExp);
                        if (indexFilter < indexExp) {
                            // filter is before, move it after
                            exp.remove(filterExp);
                            exp.add(indexExp, filterExp);
                        } else if (indexFilter > indexExp + 1) {
                            // put if just behind
                            exp.remove(filterExp);
                            exp.add(indexExp + 1, filterExp);
                            indexFilter++;
                        }

                        break;
                    }
                }
            }
        }
        varList.clear(size);
    }

    /**
     * Identify remarkable filter ?x &lt; ?y ?x = ?y or ?x = cst or !bound() filter
     * is tagged
     */
    public List<Exp> findBindings(Exp exp) {
        for (Exp ee : exp) {
            if (ee.isFilter()) {
                compiler.process(getQuery(), ee);
            }
        }
        return exp.varBind();
    }

    /**
     * JOIN service
     */
    void service(Exp exp) {
        int hasService = 0;
        for (Exp ee : exp) {
            if (isService(ee)) {
                hasService += 1;
            }
        }
        if (hasService > 0) {
            // replace: exp . service by: join(exp, service)
            service(exp, hasService);
        }
    }

    boolean isService(Exp exp) {
        return switch (exp.type()) {
            case SERVICE -> true;
            case UNION -> unionService(exp);
            default -> false;
        };
    }

    boolean unionService(Exp exp) {
        return bgpService(exp.get(0)) || bgpService(exp.get(1));
    }

     boolean bgpService(Exp exp) {
        for (Exp e : exp) {
            if (isService(e)) {
                return true;
            }
        }
        return false;
     }


    /**
     * For each service in exp
     * replace: (ee service) by: join(ee, service)
     */
    void service(Exp exp, int nbService) {

        if (nbService < 1 || (nbService == 1 && isService(exp.get(0)))) {
            // nothing to do
            return;
        }

        int count = 0;
        int i = 0;

        Exp and = Exp.create(Type.AND);

        while (count < nbService) {
            // there are services

            while (!isService(exp.get(i))) {
                // find next service
                and.add(exp.get(i));
                exp.remove(i);
            }

            // exp.get(i) is a service
            count++;

            if (and.size() == 0) {
                and.add(exp.get(i));
            } else {
                Exp join = Exp.create(Type.JOIN, and, exp.get(i));
                and = Exp.create(Type.AND);
                and.add(join);
            }

            exp.remove(i);
        }

        while (exp.size() > 0) {
            // no more service
            and.add(exp.get(0));
            exp.remove(0);
        }

        for (Exp e : and) {
            exp.add(e);
        }
    }

    public QueryPlan getQuery() {
        return query;
    }

    public Producer getProducer() {
        return prod;
    }

    public void setProducer(Producer prod) {
        this.prod = prod;
    }

    static class VString extends ArrayList<String> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;


        void clear(int size) {
            if (size == 0) {
                clear();
            } else {
                while (size() > size) {
                    remove(size() - 1);
                }
            }
        }

        @Override
        public boolean add(String v) {
            if (!contains(v)) {
                super.add(v);
            }
            return true;
        }
    }
}
