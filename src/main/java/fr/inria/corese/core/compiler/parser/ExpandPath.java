package fr.inria.corese.core.compiler.parser;

import fr.inria.corese.core.compiler.api.QueryVisitor;
import fr.inria.corese.core.kgram.api.core.Regex;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.triple.parser.*;
import fr.inria.corese.core.sparql.triple.update.ASTUpdate;
import fr.inria.corese.core.sparql.triple.update.Composite;
import fr.inria.corese.core.sparql.triple.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrite Property Path into BGP
 * Expand path and loop (+ and *) with a chain of length at most n
 * Implemented as a Visitor that is applied at compilation time, just after parsing.
 * Visitor rewrites the AST, Paths are transformed into BGP
 * <p>
 * Usage:
 * <p>
 * (1) pragma {kg:path kg:expand n}
 * <p>
 * (2) exec.setVisitor(ExpandPath.create(n));
 *
 * @author Olivier Corby, Wimmics, INRIA 2012
 */
public class ExpandPath implements QueryVisitor {

    private static final Logger log = LoggerFactory.getLogger(ExpandPath.class);

    private static final String ROOT = "?_VAR_";
    private static final String NEQ = Term.SNEQ;
    private static final String OR = Term.SEOR;
    private static final String AND = Term.SEAND;

    int max = 5;
    int varCount = 0;
    boolean isDebug = false;
    ASTQuery ast;

    ExpandPath(int n) {
        max = n;
    }

    ExpandPath() {

    }

    public static ExpandPath create() {
        return new ExpandPath();
    }

    public static ExpandPath create(int n) {
        return new ExpandPath(n);
    }

	@Override
    public void visit(ASTQuery ast) {
        rewrite(ast);
    }

	@Override
    public void visit(Query query) {

    }


    /**
     * rewrite path to sparql 1.0 BGP
     */
    public void rewrite(ASTQuery ast) {
        this.ast = ast;
        isDebug = ast.isDebug();
        rewrite(ast.getBody());

        if (ast.isUpdate()) {
            rewrite(ast.getUpdate());
        }
    }


    /**
     * Query is a SPARQL Update
     * rewrite the body of insert delete where
     */
    public void rewrite(ASTUpdate update) {
        for (Update u : update.getUpdates()) {
            if (u.isComposite()) {
                Composite c = u.getComposite();
                if (c.getBody() != null) {
                    rewrite(c.getBody());
                }
            }
        }
    }


    /**
     * Rewrite path as BGP
     * Modify exp (no copy is done)
     */
    public Exp rewrite(Exp exp) {
        if (ast == null) {
            ast = ASTQuery.create();
        }

        if (exp.isTriple()) {
            Triple t = exp.getTriple();
            if (t.isPath()) {
                return rewrite(t, t.getRegex());
            }
        } else if (exp.isQuery()) {
            rewrite(exp.getAST().getBody());
        } else for (int i = 0; i < exp.size(); i++) {
            exp.set(i, rewrite(exp.get(i)));
        }
        return exp;
    }

    private Exp rewrite(Triple path, Expression exp) {
        Exp res = null;

        switch (exp.getretype()) {

            case Regex.UNDEF:
                if (isDebug) log.debug("** Expand: not rewrite UNDEF Path: " + exp);

            case Regex.LABEL:
                res = triple(path, exp);
                break;

            case Regex.STAR:
                if (isDebug) log.debug("** Expand: rewrite exp* " + exp + " as exp+");

            case Regex.PLUS:
                res = loop(path, exp);
                break;

            case Regex.SEQ:
                res = sequence(path, exp);
                break;

            case Regex.ALT:
                res = alt(path, exp);
                break;

            case Regex.NOT:
                res = not(path, exp);
                break;

            case Regex.OPTION:
                res = option(path, exp);
                break;

            case Regex.REVERSE:
                res = reverse(path, exp);
                break;

        }

        res = rewrite(res);
        return res;
    }


    /**
     * Create a fresh new variable
     */
    private Variable variable() {
        return Variable.create(ROOT + varCount++);
    }

    /**
     * exp is a property label, generate a simple triple
     */
    private Exp triple(Triple t, Expression exp) {
        return Triple.create(t.getSubject(), exp.getConstant(), t.getObject());
    }


    /**
     * ?x ^ exp ?y
     * ->
     * ?y exp ?x
     */
    private Exp reverse(Triple t, Expression exp) {
        return ast.createPath(t.getArg(1), exp.getArg(0), t.getArg(0));
    }


    /**
     * e1 | e2
     */
    private Exp alt(Triple t, Expression exp) {
        Exp e1 = ast.createPath(t.getArg(0), exp.getArg(0), t.getArg(1));
        Exp e2 = ast.createPath(t.getArg(0), exp.getArg(1), t.getArg(1));

        BasicGraphPattern bgp1 = BasicGraphPattern.create(e1);
        BasicGraphPattern bgp2 = BasicGraphPattern.create(e2);

        return Union.create(bgp1, bgp2);
    }

    /**
     * e1 / e2
     * Special cases:
     * (1) ?x rdf:type/rdfs:subClassOf* ?c
     * ->
     * {?x rdf:type ?c} union { ?x rdf:type/rdfs:subClassOf+ ?c}
     * (2) rdf:rest* / rdf:first -> rdf:first union rdf:rest+ / rdf:first
     */
    private Exp sequence(Triple t, Expression exp) {
        Variable variable = variable();
        Exp e1 = ast.createPath(t.getArg(0), exp.getArg(0), variable);
        Exp e2 = ast.createPath(variable, exp.getArg(1), t.getArg(1));
        Exp res = BasicGraphPattern.create(e1, e2);

        if (exp.getArg(0).isStar() || exp.getArg(0).isOpt()) {
            // use case: rdf:rest*/rdf:first
            // add ?x rdf:first ?y
            Exp e = ast.createPath(t.getArg(0), exp.getArg(1), t.getArg(1));
            BasicGraphPattern b = BasicGraphPattern.create(e);
            res = Union.create(b, res);
        } else if (exp.getArg(1).isStar() || exp.getArg(1).isOpt()) {
            // use case: rdf:type/rdfs:subClassOf*
            // add ?x rdf:type ?c
            Exp e = ast.createPath(t.getArg(0), exp.getArg(0), t.getArg(1));
            BasicGraphPattern b = BasicGraphPattern.create(e);
            res = Union.create(b, res);
        }

        return res;
    }


    /**
     * Expression loop is exp+ or exp*
     * exp* rewritten as exp+ except in the case e1/e2* where it is correctly rewritten
     */
    private Exp loop(Triple t, Expression loop) {
        return loop(t.getSubject(), loop.getArg(0), t.getObject(), new ArrayList<>(), max);
    }


    /**
     * rec create a path bgp of length n between s and o
     * s exp(n) o =
     * {s exp o} union {s exp vi . vi exp(n-1) o}
     * generate filter to prevent loops on intermediate nodes (vi != vj)
     */
    private Exp loop(Atom subject, Expression exp, Atom object, List<Variable> list, int n) {

        Exp res = ast.createPath(subject, exp, object);

        if (n <= 1) {
            return res;
        }

        Variable variable = variable();
        Term diff = filter(variable, list);
        list.add(variable);

        Exp e1 = ast.createPath(subject, exp, variable);
        Exp e2 = loop(variable, exp, object, list, n - 1);

        BasicGraphPattern bgp = BasicGraphPattern.create(e1);

        if (diff != null) {
            bgp.add(diff);
        }

        bgp.add(e2);

        return Union.create(res, bgp);
    }


    /**
     * Generate filter vi != vj
     */
    Term filter(Variable variable, List<Variable> list) {
        Term res = null;

        for (Variable v : list) {
            Term t = Term.create(NEQ, variable, v);
            if (res == null) {
                res = t;
            } else {
                res = Term.create(AND, res, t);
            }
        }

        return res;
    }


    Term filter(List<Variable> list) {
        Term res = null;

        for (int i = 0; i < list.size(); i++) {
            Variable v1 = list.get(i);

            for (int j = i + 1; j < list.size(); j++) {
                Variable v2 = list.get(j);
                Term t = Term.create(NEQ, v1, v2);
                if (res == null) {
                    res = t;
                } else {
                    res = Term.create(AND, res, t);
                }
            }
        }

        return res;
    }


    /**
     * exp? -> exp
     * See also: special treatment in sequence
     */
    private Exp option(Triple t, Expression option) {
        return ast.createPath(t.getSubject(), option.getArg(0), t.getObject());
    }


    /**
     * ! ex:p1
     * ! (ex:p1 | ex:p2)
     * ->
     * ?x ?p ?y . filter(?p != ex:p1)
     */
    private Exp not(Triple t, Expression not) {
        Expression exp = not.getArg(0);
        Variable p = variable();
        Triple tt = Triple.create(t.getSubject(), p, t.getObject());
        BasicGraphPattern bgp = BasicGraphPattern.create(tt);
        Expression f = null;

        if (exp.isConstant()) {
            // ! p
            f = Term.create(NEQ, p, exp);
        } else {
            // ! (p1 | p2)

            for (Expression ee : exp.getArgs()) {
                Term g = Term.create(NEQ, p, ee);
                f = and(f, g);
            }
        }

        bgp.add(f);

        return bgp;
    }


    private Expression and(Expression t1, Expression t2) {
        if (t1 == null) return t2;
        return Term.create(Term.SEAND, t1, t2);
    }


}
