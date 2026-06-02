package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.ExprAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function call of a function identified by an IRI
 */
public record FunctionCallAst(TermAst functionName, List<TermAst> arguments) implements ExprAst {
    FunctionCallAst(List<TermAst> args) {
        this(args.getFirst(), args.subList(1, args.size() - 1));
        if (args.isEmpty()) {
            throw new QuerySyntaxException("Unexpected number of arguments for function call constructor");
        }
    }


    @Override
    public String getName() {
        return "Function call " + functionName.getName();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.functionName.accept(visitor);
        this.arguments.forEach(termAst -> {
            termAst.accept(visitor);
        });
    }
}
