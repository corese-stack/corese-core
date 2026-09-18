package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructTemplateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.Objects;

/**
 * Compiles a {@link ConstructQueryAst} template and modifiers into a runtime {@link Query}.
 */
final class ConstructQueryCompiler {

    private ConstructQueryCompiler() {
    }

    /**
     * Compiles and attaches the construct template to the runtime query shell.
     *
     * @param query query shell with compiled WHERE body and dataset
     * @param constructQueryAst AST of the CONSTRUCT query
     * @param compiler where compiler carrying the query prologue
     */
    static void compile(Query query, ConstructQueryAst constructQueryAst, WhereCompiler compiler) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(constructQueryAst, "constructQueryAst");
        Objects.requireNonNull(compiler, "compiler");

        rejectUnsupportedConstructClauses(constructQueryAst);
        Exp template = compileConstructTemplate(query, constructQueryAst.constructTemplate(), compiler);
        query.setConstruct(true);
        query.setConstruct(template);
        query.setConstructNodes(template.getNodes());
    }

    /**
     * Defensively rejects grouping and duplicate modifiers unsupported for {@code CONSTRUCT}.
     *
     * @param constructQueryAst query whose modifiers are checked
     * @throws UnsupportedQueryFeatureException if an unsupported modifier is present
     */
    private static void rejectUnsupportedConstructClauses(ConstructQueryAst constructQueryAst) {
        SolutionModifierAst mod = constructQueryAst.solutionModifier();
        if (mod.hasGroupBy() || mod.hasHaving() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedQueryFeatureException(
                    "GROUP BY, HAVING, DISTINCT and REDUCED are not supported yet by the next pipeline for CONSTRUCT");
        }
    }

    /**
     * Compiles a {@code CONSTRUCT} template into a KGRAM {@link Exp} (a BGP of edges), kept separate
     * from the {@code WHERE} body and carried by {@link Query#setConstruct(Exp)}.
     */
    private static Exp compileConstructTemplate(
            Query query, ConstructTemplateAst template, WhereCompiler compiler) {
        Exp bgp = Exp.create(Type.BGP);
        for (TriplePatternAst triple : template.triplePatternAsts()) {
            Node subject = constructNode(query, triple.subject(), compiler);
            Node predicate = constructNode(
                    query, WhereCompiler.simplePredicate(triple.predicate()), compiler);
            Node object = constructNode(query, triple.object(), compiler);
            bgp.add(new AstBackedEdge(subject, predicate, object));
        }
        return bgp;
    }

    /**
     * Resolves a template term to a runtime {@link Node}. A variable reuses the body node when it is
     * bound by the {@code WHERE}; otherwise it stays a fresh node (an unbound template variable is
     * valid SPARQL and simply skips its triple at instantiation, so this does not throw). IRIs, blank
     * nodes and literals become fresh constant nodes.
     */
    private static Node constructNode(Query query, TermAst term, WhereCompiler compiler) {
        if (term instanceof VarAst(String name)) {
            Node bound = visibleBodyNode(query, name);
            return bound != null ? bound : NodeImpl.forVariable(name);
        }
        return compiler.termResolver().toNode(term);
    }

    private static Node visibleBodyNode(Query query, String name) {
        for (Node node : query.selectNodesFromPattern()) {
            if (node.getLabel().equals(name)) {
                return node;
            }
        }
        return null;
    }
}
