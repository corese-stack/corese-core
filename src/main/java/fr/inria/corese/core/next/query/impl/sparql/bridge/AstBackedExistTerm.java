package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.kgram.api.core.ExprType;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.sparql.triple.parser.ASTBuffer;
import fr.inria.corese.core.sparql.triple.parser.Term;

import java.util.Objects;

final class AstBackedExistTerm extends Term {

    private final GroupGraphPatternAst patternAst;
    private Exp compiledPattern;

    AstBackedExistTerm(GroupGraphPatternAst patternAst) {
        super("exists");
        setOper(ExprType.EXIST);
        this.patternAst = Objects.requireNonNull(patternAst, "patternAst");
    }

    GroupGraphPatternAst patternAst() {
        return patternAst;
    }

    void setCompiledPattern(Exp compiledPattern) {
        this.compiledPattern = compiledPattern;
    }

    Exp compiledPattern() {
        return compiledPattern;
    }

    /**
     * The established {@code Term} contract reports an existence test through
     * {@code getExist() != null}, but this bridge carries the pattern as a next-KGRAM
     * {@link Exp} instead. Reporting it here keeps
     * {@code isRecExist()} true, which the engine relies on to place FILTER EXISTS correctly
     * (QuerySorter, in-scope filters of OPTIONAL/MINUS).
     */
    @Override
    public boolean isTermExist() {
        return true;
    }

    @Override
    public boolean isExist() {
        return true;
    }

    @Override
    public ASTBuffer toString(ASTBuffer sb) {
        return sb.append("exists {...}");
    }
}
