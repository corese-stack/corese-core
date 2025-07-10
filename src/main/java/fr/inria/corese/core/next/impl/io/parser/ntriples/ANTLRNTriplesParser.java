package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.NTriplesLexer;
import fr.inria.corese.core.next.impl.parser.antlr.NTriplesParser;
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
 * An ANTLR4-based parser for N-Triples format.
 * This parser uses an ANTLR grammar to tokenize and parse N-Triples documents,
 * then a listener to build the RDF model.
 */
public class ANTLRNTriplesParser extends AbstractRDFParser {

    public ANTLRNTriplesParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    public ANTLRNTriplesParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RdfFormat getRDFFormat() {
        return RdfFormat.NTRIPLES;
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
     * Parses N-Triples data from a Reader using ANTLR4.
     *
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI (ignored for N-Triples as all URIs are absolute).
     * @throws ParsingErrorException if a parsing or I/O error occurs.
     */
    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            NTriplesLexer lexer = new NTriplesLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            NTriplesParser antlrParser = new NTriplesParser(tokens);
            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = antlrParser.ntriplesDoc();


            NTriplesListener listener = new NTriplesListener(getModel(), getValueFactory(), getConfig());

            walker.walk((ParseTreeListener) listener, tree);

        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse N-Triples: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during N-Triples parsing: " + e.getMessage(), e);
        }
    }
}
