package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.util.StringUtils;

public class PrologueAstListener extends AbstractSparqlAstListener {
    public PrologueAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitBaseDecl(SparqlParser.BaseDeclContext ctx) {
        builder().setBaseUri(StringUtils.trimChevronIRIs(ctx.IRI_REF().getText()));
    }

    @Override
    public void exitPrefixDecl(SparqlParser.PrefixDeclContext ctx) {
        builder().addPrefix(ctx.PNAME_NS().getText(), ctx.IRI_REF().getText());
    }
}
