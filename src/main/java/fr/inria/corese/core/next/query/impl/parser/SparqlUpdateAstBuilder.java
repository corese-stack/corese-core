package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
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

    public AddRequestAst addtoAst(SparqlParser.AddContext ctx) {
        return new AddRequestAst(
                graphRefFromGraphOrDefault(ctx.graphOrDefault(0)),
                graphRefFromGraphOrDefault(ctx.graphOrDefault(1)),
                ctx.SILENT() != null);
    }
}
