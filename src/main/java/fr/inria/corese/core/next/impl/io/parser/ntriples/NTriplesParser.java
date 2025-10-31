package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.NTriplesLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.BailErrorStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * An ANTLR4-based parser for the N-Triples format.
 * This parser uses an ANTLR grammar to tokenize and parse N-Triples documents,
 * then a listener to build the RDF model.
 */
public class NTriplesParser extends AbstractRDFParser {

    /**
     * Constructor for the NTriplesParser.
     *
     * @param model   The RDF model to populate.
     * @param factory The value factory for creating RDF resources.
     */
    public NTriplesParser(Model model, ValueFactory factory) {
        this(model, factory, new NTriplesParserOptions.Builder().build());
    }

    /**
     * Constructor for the NTriplesParser with configuration options.
     *
     * @param model   The RDF model to populate.
     * @param factory The value factory for creating RDF resources.
     * @param config  The configuration options for parsing.
     */
    public NTriplesParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.NTRIPLES;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
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
            String input = charStream.toString();
            if (input.contains("@prefix")) {
                throw new ParsingErrorException("@prefix directives are not allowed in N-Triples");
            }
            if (input.contains("@base")) {
                throw new ParsingErrorException("@base directives are not allowed in N-Triples");
            }
            charStream = CharStreams.fromString(input);
            NTriplesLexer lexer = new NTriplesLexer(charStream);

            lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
            lexer.addErrorListener(new NTriplesErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            fr.inria.corese.core.next.impl.parser.antlr.NTriplesParser antlrParser = new fr.inria.corese.core.next.impl.parser.antlr.NTriplesParser(tokens);

            antlrParser.removeErrorListener(ConsoleErrorListener.INSTANCE);
            antlrParser.setErrorHandler(new BailErrorStrategy());
            antlrParser.addErrorListener(new NTriplesErrorListener());

            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = antlrParser.ntriplesDoc();

            NTriplesListener listener = new NTriplesListener(getModel(), getValueFactory(), getConfig());

            walker.walk(listener, tree);

        } catch (ParseCancellationException pce) {
            if (pce.getCause() instanceof ParsingErrorException cause) {
                throw cause;
            }
            throw new ParsingErrorException("Parsing cancelled due to a syntax error: " + pce.getMessage(), pce);
        } catch (IOException e) {
            throw new ParsingErrorException("Failed to read N-Triples input: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException("Invalid RDF data: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during N-Triples parsing: " + e.getMessage(), e);
        }
    }

    /**
     * Static inner class for a custom ANTLR error listener.
     * This class throws a ParsingErrorException whenever a syntax error
     * or lexical error is encountered.
     * This ensures that parsing failures are consistently reported
     * via the application's custom exception.
     */
    private static class NTriplesErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            String errorMessage = String.format("Syntax error at line %d:%d - %s",
                    line, charPositionInLine, msg);
            throw new ParseCancellationException(new ParsingErrorException(errorMessage, e));
        }
    }
}
