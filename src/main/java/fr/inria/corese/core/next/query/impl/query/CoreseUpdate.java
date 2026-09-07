package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.data.spi.io.IOConstants;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.InsertDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.NamedGraphQuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.SparqlAstToExpression;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Prepared SPARQL UPDATE operation.
 *
 * <p>Supports {@code INSERT DATA} and {@code DELETE DATA} through the next storage
 * pipeline. Other update forms (LOAD, CLEAR, DROP, DELETE/INSERT WHERE, etc.) throw
 * {@link UnsupportedQueryFeatureException}.</p>
 */
public final class CoreseUpdate implements Update {

    private final String updateString;
    private final StorageManager storage;
    private final SparqlParser parser;
    private final Runnable executionGuard;

    public CoreseUpdate(
            String updateString,
            StorageManager storage,
            SparqlParser parser,
            Runnable executionGuard) {
        this.updateString = Objects.requireNonNull(updateString, "updateString");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser  = Objects.requireNonNull(parser,  "parser");
        this.executionGuard = Objects.requireNonNull(executionGuard, "executionGuard");
    }

    @Override
    public void execute() throws QueryEvaluationException {
        executionGuard.run();
        UpdateRequestAst request = (UpdateRequestAst) parser.parse(updateString);
        MutationOperations mutations = storage.mutations();
        CoreseValueFactory factory = new CoreseValueFactory();

        for (UpdateRequestUnitAst operation : request.operations()) {
            switch (operation) {
                case InsertDataRequestAst(QuadsAst data) -> applyQuads(data, mutations, factory, request.prologue(), true);
                case DeleteDataRequestAst(QuadsAst data) -> applyQuads(data, mutations, factory, request.prologue(), false);
                default -> throw new UnsupportedQueryFeatureException(
                        "SPARQL UPDATE operation not yet supported: "
                                + operation.getClass().getSimpleName());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Quad mutation helpers
    // -------------------------------------------------------------------------

    private void applyQuads(QuadsAst quads, MutationOperations mutations,
                            CoreseValueFactory factory, QueryPrologueAst prologue, boolean insert) {
        Map<String, BNode> blankNodes = new HashMap<>();
        for (TriplePatternAst triple : quads.defaultTriples()) {
            Statement stmt = toStatement(triple, null, factory, prologue, blankNodes, insert);
            if (insert) {
                mutations.add(stmt);
            } else {
                mutations.remove(stmt);
            }
        }
        for (NamedGraphQuadsAst block : quads.namedGraphBlocks()) {
            Resource context = (Resource) termToValue(block.graph(), factory, prologue, blankNodes, insert);
            for (TriplePatternAst triple : block.triples()) {
                Statement stmt = toStatement(triple, context, factory, prologue, blankNodes, insert);
                if (insert) {
                    mutations.add(stmt);
                } else {
                    mutations.remove(stmt);
                }
            }
        }
    }

    private Statement toStatement(TriplePatternAst triple, Resource context,
                                  CoreseValueFactory factory, QueryPrologueAst prologue,
                                  Map<String, BNode> blankNodes, boolean insert) {
        Value subject = termToValue(triple.subject(), factory, prologue, blankNodes, insert);
        Value object  = termToValue(triple.object(), factory, prologue, blankNodes, insert);

        // Resolve predicate — INSERT/DELETE DATA only allows simple predicate IRIs
        if (!(triple.predicate() instanceof PredicatePathAst(TermAst pp))) {
            throw new UnsupportedQueryFeatureException(
                    "Property paths are not allowed in INSERT/DELETE DATA");
        }
        Value predicate = termToValue(pp, factory, prologue, blankNodes, insert);

        if (!(subject instanceof Resource s)) {
            throw new QueryEvaluationException("UPDATE subject must be a Resource, got: " + subject);
        }
        if (!(predicate instanceof IRI p)) {
            throw new QueryEvaluationException("UPDATE predicate must be an IRI, got: " + predicate);
        }
        if (context != null) {
            return factory.createStatement(s, p, object, context);
        }
        return factory.createStatement(s, p, object);
    }

    private Value termToValue(
            TermAst term,
            CoreseValueFactory factory,
            QueryPrologueAst prologue,
            Map<String, BNode> blankNodes,
            boolean insert) {
        return switch (term) {
            case IriAst(String raw) -> {
                String resolved = SparqlAstToExpression.resolveIri(raw, prologue);
                if (resolved != null && resolved.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
                    if (!insert) {
                        throw new QueryEvaluationException("Blank nodes are not allowed in DELETE DATA");
                    }
                    yield blankNodes.computeIfAbsent(resolved, ignored -> factory.createBNode());
                }
                yield factory.createIRI(resolved);
            }
            case LiteralAst(String lexical, String lang, String datatype) -> {
                String clean = SparqlAstToExpression.unquoteLexical(lexical);
                if (lang != null && !lang.isBlank()) {
                    yield factory.createLiteral(clean, lang);
                }
                if (datatype != null && !datatype.isBlank()) {
                    String resolvedDatatype = SparqlAstToExpression.resolveIri(datatype, prologue);
                    yield factory.createLiteral(clean, factory.createIRI(resolvedDatatype));
                }
                yield factory.createLiteral(clean);
            }
            default -> throw new UnsupportedQueryFeatureException(
                    "Variables are not allowed in INSERT/DELETE DATA: " + term);
        };
    }
}
