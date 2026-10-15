package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;

/**
 * Runs a line-oriented RDF grammar without retaining the complete input or its
 * parse tree. Parser instances are confined to one call; buffers and syntax
 * trees are bounded by the largest physical line, plus the reader buffer.
 */
public final class LineRDFParser {

    private LineRDFParser() { }

    /**
     * Parses and emits each complete physical line before reading the next one.
     * Valid prefixes may already have been added to the target when parsing fails;
     * callers requiring atomicity must supply a transactional target.
     *
     * @param source caller-owned reader, never closed here
     * @param lexer configured lexer, reused for each line
     * @param parser configured parser, reused for each line
     * @param document grammar entry point on {@code parser}
     * @param listener statement-emitting listener, shared across lines
     * @throws IOException if reading fails
     * @throws ParsingException if a line is invalid or the thread is interrupted
     */
    public static void parse(Reader source, Lexer lexer, Parser parser,
            Supplier<? extends ParseTree> document, ParseTreeListener listener) throws IOException {
        BufferedReader reader = source instanceof BufferedReader buffered ? buffered : new BufferedReader(source);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParseTreeWalker walker = new ParseTreeWalker();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            if (Thread.currentThread().isInterrupted()) {
                throw new ParsingException("RDF parsing was interrupted");
            }
            lexer.setInputStream(CharStreams.fromString(line));
            lexer.setLine(++lineNumber);
            tokens.setTokenSource(lexer);
            parser.setTokenStream(tokens);
            ParseTree tree = document.get();
            if (tokens.LA(1) != Token.EOF) {
                throw new ParsingException("Unexpected RDF content at line " + lineNumber
                        + ":" + tokens.LT(1).getCharPositionInLine());
            }
            walker.walk(listener, tree);
        }
    }
}
