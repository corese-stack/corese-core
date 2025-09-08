package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;

import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.TriGLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * An ANTLR4-based parser for Trig format.
 * This parser uses an ANTLR grammar to tokenize and parse Trig documents,
 * then a listener to build the RDF model.
 */
public class ANTLRTrigParser extends AbstractRDFParser {

    /**
     * Custom error listener to collect lexer and parser errors
     */
    private static class ErrorCollector extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            // Check if this is a string literal parsing error
            if (msg != null && (msg.contains("token recognition error") || msg.contains("mismatched input"))) {
                // Try to provide more context for string literal errors
                if (offendingSymbol instanceof Token) {
                    Token token = (Token) offendingSymbol;
                    String tokenText = token.getText();
                    if (msg.contains("token recognition error") && tokenText != null && tokenText.contains("\"")) {
                        msg = "Invalid string literal - possibly unterminated or contains invalid escape sequence: " + msg;
                    }
                }
            }

            String error = "line " + line + ":" + charPositionInLine + " " + msg;
            errors.add(error);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * Constructor for the ANTLRTrigParser.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     */
    public ANTLRTrigParser(Model model, ValueFactory factory) { super(model, factory); }

    /**
     * Constructor for the ANTLRTrigParser with configuration options.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param config  The configuration options for parsing.
     */
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

    /**
     * Parses Trig data from a Reader using ANTLR4.
     *
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI.
     * @throws ParsingErrorException if a parsing or I/O error occurs.
     */
    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            TriGLexer triGLexer = new TriGLexer(charStream);

            // Add error listener to catch lexer errors
            ErrorCollector errorCollector = new ErrorCollector();
            triGLexer.removeErrorListeners(); // Remove default console error listener
            triGLexer.addErrorListener(errorCollector);

            CommonTokenStream tokens = new CommonTokenStream(triGLexer);
            TriGParser triGParser = new TriGParser(tokens);

            // Add error listener to catch parser errors
            triGParser.removeErrorListeners(); // Remove default console error listener
            triGParser.addErrorListener(errorCollector);

            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = triGParser.trigDoc();

            // Check for lexer/parser errors before walking the tree
            if (errorCollector.hasErrors()) {
                throw new ParsingErrorException("Syntax error in TriG document: " + errorCollector.getErrorMessage());
            }

            TriGListerner listerner = new TriGListerner(getModel(), getValueFactory(), this.getConfig());
            walker.walk((ParseTreeListener) listerner, tree);
        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse TriG RDF: " + e.getMessage(), e);
        } catch (ParsingErrorException e) {
            // Re-throw parsing exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during TriG parsing: " + e.getMessage(), e);
        }
    }
}