package fr.inria.corese.core.approximate.ext;

import fr.inria.corese.core.approximate.algorithm.Parameters;
import fr.inria.corese.core.approximate.strategy.ApproximateStrategy;
import fr.inria.corese.core.approximate.strategy.StrategyType;
import fr.inria.corese.core.compiler.api.QueryVisitor;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.logic.OWL;
import fr.inria.corese.core.logic.RDF;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.corese.core.approximate.strategy.StrategyType.*;

/**
 * AST rewriting: according to different rules, modify the original SPARQL by
 * adding triple patterns/filters, etc
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 5 oct. 2015
 */
public class ASTRewriter implements QueryVisitor {

    public static final String APPROXIMATE = Processor.APPROXIMATE;
    static final int S = 1;
    static final int P = 2;
    static final int O = 3;
    private static final String VAR = "?_var_";
    ApproximateStrategy strategy;
    boolean relaxProperty = false;
    boolean relaxURI = false;
    boolean relaxLiteral = true;
    private int countVar = 0;
    private ASTQuery ast;

    @Override
    public void visit(Query query) {
    }

    @Override
    public void visit(ASTQuery ast) {
        if (!ast.isRelax()) {
            return;
        }
        this.ast = ast;
        init();
        strategy = new ApproximateStrategy();
        this.initOptions(ast);
        visit(ast.getBody());
    }

    // @relax kg:uri_literal_property
    void init() {
        List<String> list = ast.getMetadata().getValues(Metadata.Type.RELAX);
        if (list == null) {
            return;
        }
        if (!list.isEmpty()) {
            // let user decide about literal
            relaxLiteral = false;
        }
        for (String str : list) {
            String name = str.toLowerCase();
            if (name.equals(Metadata.RELAX_URI) || name.contains("*")) {
                relaxURI = true;
            }
            if (name.equals(Metadata.RELAX_PROPERTY) || name.contains("*")) {
                relaxProperty = true;
            }
            if (name.equals(Metadata.RELAX_LITERAL) || name.contains("*")) {
                relaxLiteral = true;
            }
        }
    }

    private void visit(Exp exp) {
        List<Exp> exTemp = new ArrayList<>(exp.getBody());

        for (Exp e : exTemp) {
            if (e.isTriple()) {
                process(exp, e.getTriple());
            } else for (Exp ee : e) {
                visit(ee);
            }
        }
    }

    private void process(Exp exp, Triple t) {

        //1 pre process, choose strategies for each atom
        Map<Integer, TripleWrapper> map = new HashMap<>();

        init(t, t.getSubject(), S, map);
        init(t, t.getPredicate(), P, map);
        init(t, t.getObject(), O, map);

        //2 rewrite triples in AST
        List<Exp> filters = new ArrayList<>();
        List<Optional> options = new ArrayList<>();

        rewrite(map.get(S), filters, options);
        if (relaxProperty) {
            rewrite(map.get(P), filters, options);
        }
        rewrite(map.get(O), filters, options);

        for (Exp filter : filters) {
            exp.add(filter);
        }

        for (Optional option : options) {
            exp.add(option);
        }
    }

    //choose the Strategy for the URI and put them into a list
    private void init(Triple triple, Atom atom, int pos, Map<Integer, TripleWrapper> map) {
        if (atom == null) {
            return;
        }

        List<StrategyType> lst = new ArrayList<>();
        IDatatype dt = atom.getDatatypeValue();

        if (dt.isURI()) {
            if (!relaxURI && pos != P) {
                return;
            }
            //S P O
            add(lst, URI_WN);
            add(lst, URI_LEX);
            add(lst, URI_EQUALITY);

            if (pos == P && !atom.getName().equalsIgnoreCase(RDF.TYPE)) { //property does not have rdfs:label & rdfs:comment
                add(lst, PROPERTY_EQUALITY);
            } else if (pos == O && triple.isType()) {
                add(lst, CLASS_HIERARCHY);
            }
        } else if ((dt.isLiteral() && relaxLiteral) && (dt.getCode() == IDatatype.STRING ||
                dt.getCode() == IDatatype.LITERAL)) {
            add(lst, LITERAL_LEX);
        }

        if (!lst.isEmpty()) {
            map.put(pos, new TripleWrapper(triple, pos, lst));
        }
    }

    //approximate the name of URI
    //ex, kg:john, kg:Johnny
    //applicable to: subject, predicate and object
    private void rewrite(TripleWrapper tw, List<Exp> filters, List<Optional> options) {
        if (tw == null) {
            return;
        }

        Variable variable = new Variable(VAR + countVar++);

        //1. get strategies in group G1 and merge them in one filter
        List<StrategyType> merge = strategy.getMergableStrategies(tw.getStrategies());
        if (!merge.isEmpty()) {
            //2.2 generate filters with functions
            filters.add(createFilter(variable, tw.getAtom(), strategy.getAlgorithmString(merge)));
        }

        //2. iterate other strategies
        for (StrategyType st : tw.getStrategies()) {
            if (merge.contains(st)) {
                continue;
            }

            String label;
            Triple t1;
            Triple t2;
            Optional opt = new Optional();
            switch (st) {
                case PROPERTY_EQUALITY:
                case URI_EQUALITY:
                    label = (st == URI_EQUALITY) ? OWL.SAMEAS : OWL.EQUIVALENTPROPERTY;
                    //create two addional triple pattern {x eq y}
                    t1 = (ast.createTriple(variable, Constant.create(label), tw.getAtom()));
                    t2 = (ast.createTriple(tw.getAtom(), Constant.create(label), variable));

                    //the filter can be omitted, because the similarity (equality =1)
                    //create optional {t1, t2}
                    opt.add(BasicGraphPattern.create(t1, t2));
                    options.add(opt);
                    break;
            }
        }

        //2.3 replace uri with vairable
        tw.setAtom(variable);
    }

    //add a filter with a specific function and parameters
    private Exp createFilter(Variable variable, Atom atom, String algs) {
        Term function = Term.function(APPROXIMATE);
        function.add(variable);
        function.add(atom);
        function.add(Constant.createString(algs));
        function.add(Constant.create(Parameters.THRESHOLD));
        return ASTQuery.createFilter(function);
    }

    private void initOptions(ASTQuery ast) {
        strategy.init(ast);
        Parameters.init(ast);
    }

    private void add(List<StrategyType> list, StrategyType st) {
        if (strategy.check(st)) {
            list.add(st);
        }
    }
}
