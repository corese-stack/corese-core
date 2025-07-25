package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;

import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.TriGLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
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
import java.nio.charset.StandardCharsets;

public class ANTLRTrigParser extends AbstractRDFParser {

    public ANTLRTrigParser(Model model, ValueFactory factory) { super(model, factory); }

    public ANTLRTrigParser(Model model, ValueFactory factory, IOOptions config) {super(model, factory, config);}

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.TRIG;
    }

    @Override
    public void setConfig(IOOptions config) {}

    @Override
    public void parse(InputStream in) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), null);
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    @Override
    public void parse(Reader reader) throws ParsingErrorException {
        parse(reader, null);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            TriGLexer triGLexer = new TriGLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(triGLexer);
            TriGParser triGParser = new TriGParser(tokens);
            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = triGParser.trigDoc();
            TriGListernerImpl listerner = new TriGListernerImpl(getModel(), getValueFactory(), this.getConfig());
            walker.walk((ParseTreeListener) listerner, tree);
        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse TriG RDF: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during TriG parsing: " + e.getMessage(), e);
        }
    }
}