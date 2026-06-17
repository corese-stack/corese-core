package fr.inria.corese.core.next.query.impl.parser.semantic.support;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;

/**
 * Visitor interface for the AST hierarchy
 */
public interface AstVisitor {
    void visit(PatternAst ast);
    void visit(QueryAst ast);
    void visit(UpdateRequestUnitAst ast);
    void visit(TermAst ast);
    void visit(ProjectionAst ast);
    void visit(QueryPrologueAst ast);
    void visit(ValuesAst ast);
    void visit(ValueMappingAst ast);
    void visit(SolutionModifierAst ast);
    void visit(GroupGraphPatternAst ast);
    void visit(TriplePatternAst ast);
    void visit(ConstructTemplateAst ast);
    void visit(DatasetClauseAst ast);
    void visit(GroupByAst ast);
    void visit(HavingAst ast);
    void visit(OrderConditionAst ast);
    void visit(PrefixDeclarationAst ast);
    void visit(ServiceAst ast);
    void visit(GraphRefAst ast);
}
