package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Update operations.
 */
public class SparqlUpdateAstBuilder extends SparqlAstBuilder{

    /**
     * Update Query queue
     */
    private final List<UpdateRequestUnitAst> updateRequestAst = new ArrayList<>();


    SparqlUpdateAstBuilder(SparqlParserOptions options) {
        super(options);
    }

    public QueryAst getResult() {
        QueryPrologueAst prologueAst = new QueryPrologueAst(List.copyOf(getPrefixDeclaration()), new IriAst(getBaseUri()));

        return new UpdateRequestAst(prologueAst, this.updateRequestAst);
    }

    public LoadRequestAst loadToAst(SparqlParser.LoadContext ctx) {
        GraphRefAst sourceGraphAst = null;
        GraphRefAst targetGraphAst = null;
        if(ctx.iriRef() != null) {
            sourceGraphAst = new GraphRefAst((IriAst) this.termFromIriRef(ctx.iriRef()));
        }
        if(ctx.graphRef() != null) {
            targetGraphAst = this.graphRefFromGraphRef(ctx.graphRef());
        }
        boolean silentFlag = ctx.SILENT() != null;
        if(sourceGraphAst != null) {
            return new LoadRequestAst(sourceGraphAst, targetGraphAst, silentFlag);
        } else {
            throw new QueryEvaluationException("No source graph found in LOAD query");
        }
    }

    public ClearRequestAst cleartoAst(SparqlParser.ClearContext ctx) {
        GraphRefAst targetGraphRef = null;
        boolean silentFlag = ctx.SILENT() != null;
        if(ctx.graphRefAll() != null) {
            targetGraphRef = this.graphRefFromGraphRefAll(ctx.graphRefAll());
        }
        if(targetGraphRef != null) {
            return new ClearRequestAst(targetGraphRef, silentFlag);
        } else {
            throw new QueryEvaluationException("No target graph reference found in CLEAR query");
        }
    }

    public DropRequestAst dropToAst(SparqlParser.DropContext ctx) {
        GraphRefAst targetGraphRef = null;
        boolean silentFlag = ctx.SILENT() != null;
        if (ctx.graphRefAll() != null) {
            targetGraphRef = this.graphRefFromGraphRefAll(ctx.graphRefAll());
        }
        if (targetGraphRef != null) {
            return new DropRequestAst(targetGraphRef, silentFlag);
        } else {
            throw new QueryEvaluationException("No target graph reference found in DROP query");
        }
    }

    public CreateRequestAst createToAst(SparqlParser.CreateContext ctx) {
        boolean silentFlag = ctx.SILENT() != null;
        if (ctx.graphRef() != null) {
            GraphRefAst targetGraphRef = this.graphRefFromGraphRef(ctx.graphRef());
            return new CreateRequestAst(targetGraphRef, silentFlag);
        }
        throw new QueryEvaluationException("No target graph reference found in CREATE query");
    }

    public void addRequest(UpdateRequestUnitAst ast) {
        this.updateRequestAst.add(ast);
    }

    public AddRequestAst addToAst(SparqlParser.AddContext ctx) {
        return new AddRequestAst(
                graphRefFromGraphOrDefault(ctx.graphOrDefault(0)),
                graphRefFromGraphOrDefault(ctx.graphOrDefault(1)),
                ctx.SILENT() != null);
    }

    public CopyRequestAst copyToAst(SparqlParser.CopyContext ctx) {
        return new CopyRequestAst(
                graphRefFromGraphOrDefault(ctx.graphOrDefault(0)),
                graphRefFromGraphOrDefault(ctx.graphOrDefault(1)),
                ctx.SILENT() != null);
    }

    public MoveRequestAst moveToAst(SparqlParser.MoveContext ctx) {
        return new MoveRequestAst(
                graphRefFromGraphOrDefault(ctx.graphOrDefault(0)),
                graphRefFromGraphOrDefault(ctx.graphOrDefault(1)),
                ctx.SILENT() != null);
    }

    public InsertDataRequestAst insertDataToAst(SparqlParser.InsertDataContext ctx) {
        return new InsertDataRequestAst(quadsFromQuads(ctx.quadData().quads()));
    }

    public DeleteDataRequestAst deleteDataToAst(SparqlParser.DeleteDataContext ctx) {
        return new DeleteDataRequestAst(quadsFromQuads(ctx.quadData().quads()));
    }

    public DeleteWhereRequestAst deleteWhereToAst(SparqlParser.DeleteWhereContext ctx) {
        return new DeleteWhereRequestAst(quadsFromQuads(ctx.quadPattern().quads()));
    }

    private QuadsAst quadsFromQuads(SparqlParser.QuadsContext ctx) {
        List<TriplePatternAst> defaultTriples = new ArrayList<>();
        List<NamedGraphQuadsAst> namedGraphBlocks = new ArrayList<>();

        for (SparqlParser.TriplesTemplateContext tt : ctx.triplesTemplate()) {
            defaultTriples.addAll(triplesFromTriplesTemplate(tt));
        }

        for (SparqlParser.QuadsNotTriplesContext qnt : ctx.quadsNotTriples()) {
            TermAst graph = termFromVarOrIriRef(qnt.varOrIri());
            List<TriplePatternAst> graphTriples = new ArrayList<>();
            if (qnt.triplesTemplate() != null) {
                graphTriples.addAll(triplesFromTriplesTemplate(qnt.triplesTemplate()));
            }
            namedGraphBlocks.add(new NamedGraphQuadsAst(graph, graphTriples));
        }

        return new QuadsAst(defaultTriples, namedGraphBlocks);
    }

    private List<TriplePatternAst> triplesFromTriplesTemplate(SparqlParser.TriplesTemplateContext ctx) {
        List<TriplePatternAst> triples = new ArrayList<>();
        SparqlParser.TriplesTemplateContext current = ctx;
        while (current != null) {
            SparqlParser.TriplesSameSubjectContext tss = current.triplesSameSubject();
            if (tss != null && tss.varOrTerm() != null && tss.propertyListNotEmpty() != null) {
                TermAst subject = termFromVarOrTerm(tss.varOrTerm());
                var propertyList = tss.propertyListNotEmpty();
                for (int verbIndex = 0; verbIndex < propertyList.verb().size(); verbIndex++) {
                    TermAst predicate = termFromVerb(propertyList.verb(verbIndex));
                    List<TermAst> objects = termListFromObjectList(propertyList.objectList(verbIndex));
                    for (TermAst object : objects) {
                        triples.add(new TriplePatternAst(subject, predicate, object));
                    }
                }
            }
            current = current.triplesTemplate();
        }
        return triples;
    }
}
