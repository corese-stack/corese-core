package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.NamedGraphQuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;

import java.util.HashSet;
import java.util.Set;

/** Enforces DATA groundness, DELETE blank-node restrictions, and INSERT DATA scope. */
final class UpdateTemplateValidator {
    private final Set<String> dataLabels = new HashSet<>();

    void validate(QuadsAst quads, boolean variables, boolean blanks) {
        Set<String> labels = new HashSet<>();
        for (TriplePatternAst triple : quads.defaultTriples()) {
            triple(triple, variables, blanks, labels);
        }
        for (NamedGraphQuadsAst graph : quads.namedGraphBlocks()) {
            term(graph.graph(), variables, false, labels);
            for (TriplePatternAst triple : graph.triples()) {
                triple(triple, variables, blanks, labels);
            }
        }
        if (!variables && blanks) {
            for (String label : labels) {
                if (!dataLabels.add(label)) {
                    throw new QuerySyntaxException("Blank node label reused across INSERT DATA operations: " + label);
                }
            }
        }
    }

    private void triple(TriplePatternAst triple, boolean variables, boolean blanks, Set<String> labels) {
        term(triple.subject(), variables, blanks, labels);
        term(((PredicatePathAst) triple.predicate()).predicate(), variables, false, labels);
        term(triple.object(), variables, blanks, labels);
    }

    private void term(TermAst term, boolean variables, boolean blanks, Set<String> labels) {
        if (!variables && term instanceof VarAst) {
            throw new QuerySyntaxException("Variables are not allowed in INSERT/DELETE DATA");
        }
        if (term instanceof IriAst(String raw) && raw.startsWith("_:")) {
            if (!blanks) {
                throw new QuerySyntaxException("Blank nodes are not allowed in DELETE templates or graph names");
            }
            labels.add(raw);
        }
    }
}
