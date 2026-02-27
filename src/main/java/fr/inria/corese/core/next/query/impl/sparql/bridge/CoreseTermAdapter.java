package fr.inria.corese.core.next.query.impl.sparql.bridge;


import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.cst.RDFS;
import fr.inria.corese.core.sparql.triple.parser.Constant;

/**
 * Converts AST terms ({@link TermAst}) into KGRAM {@link Node} instances.
 */
public final class CoreseTermAdapter {

    private CoreseTermAdapter() {
    }

    public static Node toNode(TermAst term) {
        if (term == null) {
            throw new IllegalArgumentException("term is null");
        }
        return switch (term) {
            case VarAst varAst -> NodeImpl.createVariable(varAst.name());
            case IriAst iriAst -> NodeImpl.createResource(iriAst.raw());
            case LiteralAst literalAst -> toLiteralNode(literalAst);
            default -> throw new IllegalArgumentException("Unknown term type: " + term.getClass().getName());
        };
    }

    private static Node toLiteralNode(LiteralAst literalAst) {
        String lexical = literalAst.lexical();
        String datatype = literalAst.datatype();
        String lang = literalAst.lang();
        Constant constant;
        if (lang != null && !lang.isBlank()) {
            constant = Constant.create(lexical, RDFS.qxsdString, lang);
        } else if (datatype != null && !datatype.isBlank()) {
            constant = Constant.create(lexical, datatype);
        } else {
            constant = Constant.createString(lexical);
        }
        return NodeImpl.createNode(constant);
    }
}
