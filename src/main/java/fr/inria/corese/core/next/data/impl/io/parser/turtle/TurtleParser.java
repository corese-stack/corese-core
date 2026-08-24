package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.support.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.generated.antlr.TurtleLexer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for Turtle RDF files.
 *
 */
public class TurtleParser extends AbstractRDFParser {

    /**
     * Constructor for TurtleParser that initializes the model and value
     * factory.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     */
    public TurtleParser(Model model, ValueFactory factory) {
        this(model, factory, new TurtleParserOptions.Builder().build());
    }

    /**
     * Constructor for TurtleParser that initializes the model, value factory,
     * and configuration options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     * @param config  configuration object for the parser
     */
    public TurtleParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.TURTLE;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    /**
     * Parses Turtle data from a {@link Reader} using ANTLR4.
     *
     * @param reader  The {@link Reader} to read the RDF data.
     * @param baseURI The base URI.
     * @throws ParsingException if a parsing or I/O error occurs.
     */
    @Override
    public void parse(Reader reader, String baseURI) throws ParsingException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            TurtleLexer turtleLexer = new TurtleLexer(charStream);

            TurtleErrorListener turtleErrorListener = new TurtleErrorListener();
            turtleLexer.removeErrorListeners();
            turtleLexer.addErrorListener(turtleErrorListener);

            CommonTokenStream tokens = new CommonTokenStream(turtleLexer);
            fr.inria.corese.core.next.generated.antlr.TurtleParser turtleParser = new fr.inria.corese.core.next.generated.antlr.TurtleParser(tokens);

            turtleParser.removeErrorListeners();
            turtleParser.addErrorListener(turtleErrorListener);

            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = parseTree(turtleParser, turtleErrorListener);

            IOOptions optionsWithBaseURI = new TurtleParserOptions.Builder()
                    .baseIRI(baseURI)
                    .build();
            TurtleListener listener = new TurtleListener(getModel(), getValueFactory(), optionsWithBaseURI);
            walker.walk(listener, tree);

        } catch (ParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingException("Failed to parse Turtle RDF: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingException("Unexpected error during Turtle parsing: " + e.getMessage(), e);
        }
    }

    private ParseTree parseTree(fr.inria.corese.core.next.generated.antlr.TurtleParser turtleParser, TurtleErrorListener turtleErrorListener) {
        try {
            ParseTree tree = turtleParser.turtleDoc();

            if (turtleErrorListener.hasErrors()) {
                String errorMsg = turtleErrorListener.getErrorMessage();
                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                    errorMsg = "Unknown syntax error detected";
                }
                throw new ParsingException("Syntax error in Turtle document: " + errorMsg);
            }
            return tree;
        } catch (RecognitionException e) {
            throw new ParsingException("Recognition error in Turtle document: " + e.getMessage());
        }
    }

    /**
     * A custom error listener to collect errors from the lexer and parser.
     */
    private static class TurtleErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        /**
         * Records syntax errors generated by ANTLR.
         *
         * @param recognizer         The recognizer that detected the error.
         * @param offendingSymbol    The symbol that caused the error.
         * @param line               The line number where the error occurred.
         * @param charPositionInLine The character position on the line.
         * @param msg                The error message.
         * @param e                  The recognition exception.
         */
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            if (msg == null || msg.trim().isEmpty()) {
                msg = "Unknown syntax error";
            }

            if ((msg.contains("token recognition error") || msg.contains("mismatched input"))
                    && offendingSymbol instanceof Token token) {
                String tokenText = token.getText();
                if (msg.contains("token recognition error") && tokenText != null && tokenText.contains("\"")) {
                    msg = "Invalid string literal - possibly unterminated or contains invalid escape sequence: " + msg;
                }
            }

            String error = "line " + line + ":" + charPositionInLine + " " + msg;
            errors.add(error);
        }

        /**
         * Checks if parsing errors have been found.
         *
         * @return `true` if the error list is not empty, otherwise `false`.
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * Returns a formatted error message containing all found errors.
         *
         * @return A {@link String} containing the error messages.
         */
        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "Unknown parsing error";
            }
            return String.join("; ", errors);
        }
    }
}