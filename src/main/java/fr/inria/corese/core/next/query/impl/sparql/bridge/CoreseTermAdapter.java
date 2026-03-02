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

}
