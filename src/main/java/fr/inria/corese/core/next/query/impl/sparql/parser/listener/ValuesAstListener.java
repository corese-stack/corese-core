package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.*;

/**
 * Capture parsing of VALUES. VALUES can be declared both in the WHERE clause (through {@code inlineData}) and outside the query (through {@code valuesClause}).
 */
public class ValuesAstListener extends AbstractSparqlQueryAstListener {

    public ValuesAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitValuesClause(SparqlParser.ValuesClauseContext ctx) {
        if(ctx.dataBlock() != null) {
            queryBuilder().addValues(processDataBlock(ctx.dataBlock()));
        }
    }

    @Override
    public void exitInlineData(SparqlParser.InlineDataContext ctx) {
        if(ctx.dataBlock() != null) {
            builder().addInlineValues(processDataBlock(ctx.dataBlock()));
        }
    }

    private ValuesAst processDataBlock(SparqlParser.DataBlockContext ctx) {
        if(ctx.inlineDataOneVar() != null) {
            return processInlineDataOneVar(ctx.inlineDataOneVar());
        }
        if (ctx.inlineDataFull() != null) {
            return processInlineDataFull(ctx.inlineDataFull());
        }
        throw new QuerySyntaxException("Missing data block in VALUES clause");
    }

    private ValuesAst processInlineDataOneVar(SparqlParser.InlineDataOneVarContext ctx) {
        if (ctx.var_() == null) {
            throw new QuerySyntaxException("Missing variable for solution mapping in VALUES clause");
        }
        VarAst variable = (VarAst) builder().termFromVar(ctx.var_());
        List<VarAst> header = List.of(variable);
        List<ValueMappingAst> rows = new ArrayList<>();
        for (var value : ctx.dataBlockValue()) {
            rows.add(new ValueMappingAst(termAstFromDataBlockValues(header, List.of(value))));
        }
        return new ValuesAst(header, rows);
    }

    /**
     *
     * @return A list of terms or null for UNDEF values
     */
    private Map<VarAst, TermAst> termAstFromDataBlockValues(List<VarAst> variables, List<SparqlParser.DataBlockValueContext> dataBlockValueList) {
        if(variables.size() != dataBlockValueList.size()) {
            throw new QuerySyntaxException("VALUE solutions should have a value for every variable and at least a variable for a solution.");
        }
        Map<VarAst, TermAst> valuesList = new LinkedHashMap<>();
        for(int varNum = 0; varNum < variables.size(); varNum++) {
            VarAst variable = variables.get(varNum);
            SparqlParser.DataBlockValueContext dataBlockValueContext = dataBlockValueList.get(varNum);
            if(dataBlockValueContext.iriRef() != null) {
                valuesList.put(variable, this.builder().termFromIriRef(dataBlockValueContext.iriRef()));
            } else if(dataBlockValueContext.rdfLiteral() != null) {
                valuesList.put(variable, this.builder().termFromRdfLiteral(dataBlockValueContext.rdfLiteral()));
            } else if(dataBlockValueContext.numericLiteral() != null) {
                valuesList.put(variable, this.builder().termFromNumericLiteral(dataBlockValueContext.numericLiteral()));
            } else if(dataBlockValueContext.booleanLiteral() != null) {
                valuesList.put(variable, this.builder().termFromBooleanLiteral(dataBlockValueContext.booleanLiteral()));
            } else if(dataBlockValueContext.UNDEF() != null) {
                valuesList.put(variable, null);
            }
        }
        return valuesList;
    }

    private ValuesAst processInlineDataFull(SparqlParser.InlineDataFullContext ctx) {
        List<VarAst> varList = new ArrayList<>();
        for (var variable : ctx.var_()) {
            varList.add((VarAst) builder().termFromVar(variable));
        }
        List<ValueMappingAst> valuesList = new ArrayList<>();
        for (var row : ctx.dataBlockValues()) {
            valuesList.add(new ValueMappingAst(termAstFromDataBlockValues(varList, row.dataBlockValue())));
        }
        return new ValuesAst(varList, valuesList);
    }
}
