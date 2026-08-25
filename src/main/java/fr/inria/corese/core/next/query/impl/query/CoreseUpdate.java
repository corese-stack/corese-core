package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.data.api.term.IRI;
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
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.common.text.RdfText;

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
        MutationOperations mutations = storage.getMutationOperations();
        CoreseValueFactory factory = new CoreseValueFactory();

        for (UpdateRequestUnitAst operation : request.operations()) {
            switch (operation) {
                case InsertDataRequestAst(QuadsAst data) -> applyQuads(data, mutations, factory, true);
                case DeleteDataRequestAst(QuadsAst data) -> applyQuads(data, mutations, factory, false);
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
                            CoreseValueFactory factory, boolean insert) {
        for (TriplePatternAst triple : quads.defaultTriples()) {
            Statement stmt = toStatement(triple, null, factory);
            if (insert) {
                mutations.insertStatement(stmt);
            } else {
                mutations.deleteStatement(stmt);
            }
        }
        for (NamedGraphQuadsAst block : quads.namedGraphBlocks()) {
            Resource context = (Resource) termToValue(block.graph(), factory);
            for (TriplePatternAst triple : block.triples()) {
                Statement stmt = toStatement(triple, context, factory);
                if (insert) {
                    mutations.insertStatement(stmt);
                } else {
                    mutations.deleteStatement(stmt);
                }
            }
        }
    }

    private Statement toStatement(TriplePatternAst triple, Resource context, CoreseValueFactory factory) {
        Value subject = termToValue(triple.subject(), factory);
        Value object  = termToValue(triple.object(), factory);

        // Resolve predicate — INSERT/DELETE DATA only allows simple predicate IRIs
        if (!(triple.predicate() instanceof PredicatePathAst(TermAst pp))) {
            throw new UnsupportedQueryFeatureException(
                    "Property paths are not allowed in INSERT/DELETE DATA");
        }
        Value predicate = termToValue(pp, factory);

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

    private Value termToValue(TermAst term, CoreseValueFactory factory) {
        return switch (term) {
            case IriAst(String raw) -> factory.createIRI(RdfText.stripAngleBrackets(raw));
            case LiteralAst(String lexical, String datatype, String lang) -> {
                if (lang != null && !lang.isBlank()) {
                    yield factory.createLiteral(lexical, lang);
                }
                if (datatype != null && !datatype.isBlank()) {
                    yield factory.createLiteral(lexical, factory.createIRI(datatype));
                }
                yield factory.createLiteral(lexical);
            }
            default -> throw new UnsupportedQueryFeatureException(
                    "Variables are not allowed in INSERT/DELETE DATA: " + term);
        };
    }
}
