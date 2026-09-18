package fr.inria.corese.core.next.query.impl.sparql.parser;


import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.common.text.RdfText;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.AddRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ClearRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.CopyRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.CreateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DatasetClauseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteWhereRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DropRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GraphRefAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.InsertDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ModifyRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MoveRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Builder for Update operations.
 */
public class SparqlUpdateAstBuilder extends SparqlQueryAstBuilder{

    /**
     * Update Query queue
     */
    private final List<UpdateRequestUnitAst> updateRequestAst = new ArrayList<>();
    private final List<QueryPrologueAst> prologues = new ArrayList<>();
    private final UpdateTemplateValidator templates = new UpdateTemplateValidator();


    SparqlUpdateAstBuilder(SparqlParserOptions options) {
        super(options);
    }

    @Override
    public void addPrefix(String prefix, String uri) {
        String name = RdfText.stripTrailingColon(prefix);
        prefixDeclarations.removeIf(declaration -> declaration.prefix().equals(name));
        super.addPrefix(prefix, uri);
    }

    @Override
    public void setBaseUri(String uri) {
        String iri = RdfText.stripAngleBrackets(uri);
        baseUri = IRIUtils.isAbsoluteIRI(iri) ? iri : IRIUtils.resolveIRIAgainstBase(baseUri, iri);
    }

    @Override
    public QueryAst getResult() {
        QueryPrologueAst prologueAst = new QueryPrologueAst(List.copyOf(getPrefixDeclaration()), new IriAst(getBaseUri()));

        return new UpdateRequestAst(prologueAst, this.updateRequestAst, prologues);
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
        switch (ast) {
            case InsertDataRequestAst(QuadsAst data) -> templates.validate(data, false, true);
            case DeleteDataRequestAst(QuadsAst data) -> templates.validate(data, false, false);
            case DeleteWhereRequestAst(QuadsAst data) -> templates.validate(data, true, false);
            case ModifyRequestAst modify -> templates.validate(modify.deleteTemplate(), true, false);
            default -> { /* Graph management has no quad templates. */ }
        }
        this.updateRequestAst.add(ast);
        prologues.add(new QueryPrologueAst(getPrefixDeclaration(), new IriAst(getBaseUri())));
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

    public ModifyRequestAst modifyToAst(SparqlParser.ModifyContext context) {
        QuadsAst delete = context.deleteClause() == null ? new QuadsAst(null, null)
                : quadsFromQuads(context.deleteClause().quadPattern().quads());
        QuadsAst insert = context.insertClause() == null ? new QuadsAst(null, null)
                : quadsFromQuads(context.insertClause().quadPattern().quads());
        Set<IriAst> defaults = new LinkedHashSet<>();
        Set<IriAst> named = new LinkedHashSet<>();
        for (SparqlParser.UsingClauseContext using : context.usingClause()) {
            IriAst iri = (IriAst) termFromIriRef(using.iriRef());
            if (using.NAMED() == null) {
                defaults.add(iri);
            } else {
                named.add(iri);
            }
        }
        IriAst with = context.iriRef() == null ? null : (IriAst) termFromIriRef(context.iriRef());
        return new ModifyRequestAst(with, delete, insert, new DatasetClauseAst(defaults, named),
                (SelectQueryAst) super.getResult());
    }

    private QuadsAst quadsFromQuads(SparqlParser.QuadsContext context) {
        return new SparqlQuadTemplateBuilder(this).quads(context);
    }
}
