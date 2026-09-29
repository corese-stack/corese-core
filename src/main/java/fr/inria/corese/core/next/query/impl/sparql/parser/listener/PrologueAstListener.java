package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.common.text.RdfText;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlIriEscapes;

public class PrologueAstListener extends AbstractSparqlAstListener {
    public PrologueAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitBaseDecl(SparqlParser.BaseDeclContext ctx) {
        builder().setBaseUri(RdfText.stripAngleBrackets(SparqlIriEscapes.decode(ctx.IRI_REF().getText())));
    }

    @Override
    public void exitPrefixDecl(SparqlParser.PrefixDeclContext ctx) {
        builder().addPrefix(ctx.PNAME_NS().getText(), SparqlIriEscapes.decode(ctx.IRI_REF().getText()));
    }
}
