package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.NamedGraphQuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.SparqlTermResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Instantiates one solution, sharing fresh blank nodes across its quad templates. */
final class UpdateTemplate {
    private final SparqlTermResolver resolver;
    private final BindingSet bindings;
    private final Map<String, BNode> blankNodes = new HashMap<>();

    UpdateTemplate(SparqlTermResolver resolver, BindingSet bindings) {
        this.resolver = resolver;
        this.bindings = bindings;
    }

    List<Statement> instantiate(QuadsAst template, Resource defaultGraph) {
        List<Statement> statements = new ArrayList<>();
        append(template.defaultTriples(), defaultGraph, statements);
        for (NamedGraphQuadsAst block : template.namedGraphBlocks()) {
            if (value(block.graph()) instanceof IRI graph) {
                append(block.triples(), graph, statements);
            }
        }
        return statements;
    }

    private void append(List<TriplePatternAst> triples, Resource graph, List<Statement> statements) {
        for (TriplePatternAst triple : triples) {
            Value subject = value(triple.subject());
            Value object = value(triple.object());
            Value predicate = value(((PredicatePathAst) triple.predicate()).predicate());
            if (subject instanceof Resource resource && predicate instanceof IRI iri && object != null) {
                statements.add(Values.factory().createStatement(resource, iri, object, graph));
            }
        }
    }

    private Value value(TermAst term) {
        if (term instanceof VarAst(String name)) {
            return bindings == null ? null : bindings.getValue(name);
        }
        if (term instanceof IriAst(String raw) && raw.startsWith("_:")) {
            return blankNodes.computeIfAbsent(raw, ignored -> Values.factory().createBNode());
        }
        return (Value) resolver.toNode(term).getDatatypeValue();
    }
}
