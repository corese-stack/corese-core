package fr.inria.corese.core.next.data.impl.io.parser.nquads;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.support.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.generated.antlr.NQuadsLexer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
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
public class NQuadsParser extends AbstractRDFParser {

    /**
     * Constructor for the NQuadsParser. Will generate a
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     */
    public NQuadsParser(Model model, ValueFactory factory) {
        this(model, factory, new NQuadsParserOptions.Builder().build());
    }

    /**
     * Constructor for the NQuadsParser with configuration options.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param config  The configuration options for parsing.
     */
    public NQuadsParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.NQUADS;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    /**
     * Parses N-Quads data from a Reader using ANTLR4.
     *
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI (ignored for N-Quads as all URIs are absolute).
     * @throws ParsingException if a parsing or I/O error occurs.
     */
    @Override
    public void parse(Reader reader, String baseURI) throws ParsingException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);

            NQuadsLexer lexer = new NQuadsLexer(charStream);
            configureErrorHandling(lexer);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            fr.inria.corese.core.next.generated.antlr.NQuadsParser parser = new fr.inria.corese.core.next.generated.antlr.NQuadsParser(tokens);
            configureErrorHandling(parser);

            ParseTree tree = parser.nquadsDoc();

            ParseTreeWalker walker = new ParseTreeWalker();
            NQuadsListener listener = new NQuadsListener(getModel(), getValueFactory(), getConfig());
            walker.walk(listener, tree);

        } catch (ParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingException("Failed to read N-Quads input: " + e.getMessage(), e);
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    /**
     * Configures error handling for lexer or parser.
     * Replaces default error listeners with strict N-Quads error listener.
     *
     * @param recognizer Lexer or parser to configure
     */
    private void configureErrorHandling(Recognizer<?, ?> recognizer) {
        recognizer.removeErrorListeners();
        recognizer.addErrorListener(NQuadsErrorListener.INSTANCE);
    }

    /**
     * Unwraps nested exceptions to find and re-throw ParsingException.
     *
     * @param exception Exception to unwrap
     * @return ParsingException if found in cause chain
     * @throws ParsingException always, either original or wrapped
     */
    private ParsingException unwrapException(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ParsingException pe) {
                return pe;
            }
            current = current.getCause();
        }
        String msg = (exception != null && exception.getMessage() != null) ? exception.getMessage() : "Unknown error";
        return new ParsingException("Unexpected error during N-Quads parsing: " + msg, exception);
    }

    /**
     * Custom ANTLR ErrorListener that throws a ParsingException on any syntax error.
     * This ensures that parsing failures are immediately reported as application-specific exceptions.
     */
    private static class NQuadsErrorListener extends BaseErrorListener {

        static final NQuadsErrorListener INSTANCE = new NQuadsErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            throw new ParsingException(
                    String.format("Syntax error in N-Quads at line %d:%d - %s",
                            line, charPositionInLine, msg));
        }
    }
}