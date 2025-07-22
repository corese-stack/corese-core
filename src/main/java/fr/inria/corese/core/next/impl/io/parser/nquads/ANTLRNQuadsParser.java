package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsLexer;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsParser;
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

/**
 * An ANTLR4-based parser for N-Quads format.
 * This parser uses an ANTLR grammar to tokenize and parse N-Quads documents,
 * then a listener to build the RDF model.
 */
public class ANTLRNQuadsParser extends AbstractRDFParser {

    /**
     * Constructor for the ANTLRNQuadsParser.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     */
    public ANTLRNQuadsParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    /**
     * Constructor for the ANTLRNQuadsParser with configuration options.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param config  The configuration options for parsing.
     */
    public ANTLRNQuadsParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.NQUADS;
    }


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

    /**
     * Parses N-Quads data from a Reader using ANTLR4.
     *
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI (ignored for N-Quads as all URIs are absolute).
     * @throws ParsingErrorException if a parsing or I/O error occurs.
     */
    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            NQuadsLexer lexer = new NQuadsLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            NQuadsParser antlrParser = new NQuadsParser(tokens);
            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = antlrParser.nquadsDoc();

            NQuadsListener listener = new NQuadsListener(getModel(), getValueFactory(), getConfig());

            walker.walk((ParseTreeListener) listener, tree);

        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse N-Quads: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during N-Quads parsing: " + e.getMessage(), e);
        }
    }
}
