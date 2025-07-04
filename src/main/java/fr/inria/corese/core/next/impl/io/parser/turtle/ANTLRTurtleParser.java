package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleParser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ANTLRTurtleParser extends AbstractRDFParser {

    public ANTLRTurtleParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    @Override
    public RdfFormat getRDFFormat() {
        return RdfFormat.TURTLE;
    }

    /**
     * @param config we are not using any config in this parser implementation
     */
    @Override
    public void setConfig(RDFParserOptions config) {
        // nothing to do
    }

    /**
     * @return null, we are not using any config in this parser implementation
     */
    @Override
    public RDFParserOptions getConfig() {
        return null;
    }

    @Override
    public void parse(InputStream in) {
        parse(new InputStreamReader(in), null);
    }

    @Override
    public void parse(InputStream in, String baseURI) {
        parse(new InputStreamReader(in), baseURI);
    }

    @Override
    public void parse(Reader reader) {
        parse(reader, null);
    }

    /**
     * We are using ANTLR4 lexer and parser
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     */
    @Override
    public void parse(Reader reader, String baseURI) {

        try {
            CharStream charStream = CharStreams.fromReader(reader);
            TurtleLexer lexer = new TurtleLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TurtleParser parser = new TurtleParser(tokens);
            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = parser.turtleDoc();
            TurtleListenerImpl listener = new TurtleListenerImpl(getModel(), baseURI, getValueFactory());

            walker.walk((ParseTreeListener) listener, tree);

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Turtle RDF", e);
        }
    }
}