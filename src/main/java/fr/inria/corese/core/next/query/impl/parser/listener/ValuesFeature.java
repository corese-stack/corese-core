package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.*;

/**
 * Capture parsing of VALUES. VALUES can be declared both in the WHERE clause (through {@code inlineData}) and outside the query (through {@code valuesClause}).
 */
public class ValuesFeature extends AbstractSparqlQueryFeature {

    public ValuesFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitValuesClause(SparqlParser.ValuesClauseContext ctx) {
        if(ctx.dataBlock() != null) {
            processDataBlock(ctx.dataBlock());
        }
    }

    @Override
    public void exitInlineData(SparqlParser.InlineDataContext ctx) {
        if(ctx.dataBlock() != null) {
            processDataBlock(ctx.dataBlock());
        }
    }

    private void processDataBlock(SparqlParser.DataBlockContext ctx) {
        if(ctx.inlineDataOneVar() != null) {
            processInlineDataOneVar(ctx.inlineDataOneVar());
        } else if (ctx.inlineDataFull() != null) {
            processInlineDataFull(ctx.inlineDataFull());
        }
    }

    private void processInlineDataOneVar(SparqlParser.InlineDataOneVarContext ctx) {
        if(ctx.var_() != null) {
            VarAst varAst = (VarAst) this.builder().termFromVar(ctx.var_());
            if(!Objects.equals(varAst.name(), "()")) {
                List<ValueMappingAst> mappingAstList = new ArrayList<>();
                if(ctx.dataBlockValue() != null) {
                    ctx.dataBlockValue().forEach(dataBlockValueContext -> {
                        Map<VarAst, TermAst> valueMap = termAstFromDataBlockValues(List.of(varAst), List.of(dataBlockValueContext));
                        // Each dataBlockValue is a solution
                        mappingAstList.add(new ValueMappingAst(valueMap));
                    });
                    this.queryBuilder().addValues(mappingAstList);
                }
            }
        } else if(ctx.var_() == null && ctx.dataBlockValue() != null) {
            throw new QuerySyntaxException("Missing variable for solution mapping in VALUES clause");
        }
    }

    /**
     *
     * @param dataBlockValueList
     * @return A list of terms or null for UNDEF values
     */
    private Map<VarAst, TermAst> termAstFromDataBlockValues(List<VarAst> variables, List<SparqlParser.DataBlockValueContext> dataBlockValueList) {
        if(variables.size() != dataBlockValueList.size()) {
            throw new QuerySyntaxException("VALUE solutions should have a value for every variable and at least a variable for a solution.");
        }
        Map<VarAst, TermAst> valuesList = new HashMap<>();
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
        dataBlockValueList.forEach(dataBlockValueContext -> {
        });
        return valuesList;
    }

    private void processInlineDataFull(SparqlParser.InlineDataFullContext ctx) {
        List<VarAst> varList = new ArrayList<>();
        if(ctx.var_() != null) {
            ctx.var_().forEach(varContext -> {
                VarAst varAst = (VarAst) this.builder().termFromVar(varContext);
                if(!Objects.equals(varAst.name(), "()")) {
                    varList.add(varAst);
                }
            });
        }
        if(!varList.isEmpty()) {
            List<ValueMappingAst> valuesList = new ArrayList<>();
            if(ctx.dataBlockValues() != null) { // Each dataBlockValues is a solution
                ctx.dataBlockValues().forEach(dataBlockValuesContext -> { // Each dataBlockValue is a value for a variable in a solution
                    if(dataBlockValuesContext.dataBlockValue() != null) {
                        Map<VarAst, TermAst> valueList = termAstFromDataBlockValues(varList, dataBlockValuesContext.dataBlockValue());
                        valuesList.add(new ValueMappingAst(valueList));
                    }
                });
            }
            this.queryBuilder().addValues(valuesList);
        }
    }
}
