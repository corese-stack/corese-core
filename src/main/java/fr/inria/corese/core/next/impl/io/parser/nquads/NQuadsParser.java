package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractParser;
import fr.inria.corese.core.next.api.io.IOConfig;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A simplified parser for N-Quads format.
 * This parser reads N-Quads line by line and attempts to parse
 * subject, predicate, object, and an optional graph, then adds them to the model.
 *
 * IMPORTANT LIMITATION: This is a basic implementation. The `splitNqLine` method
 * is simplistic and WILL NOT correctly handle complex RDF terms, especially literals
 * containing spaces or intricate escaping, when trying to differentiate the object from the graph.
 * For production use, a more robust N-Quads parsing library would be highly recommended,
 * ideally one that uses a proper state machine or lexer.
 */
public class NQuadsParser extends AbstractParser {

    private static final Logger logger = LoggerFactory.getLogger(NQuadsParser.class);

    public NQuadsParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    public NQuadsParser(Model model, ValueFactory factory, IOConfig config) {
        super(model, factory);
        setConfig(config);
    }

    @Override
    public RdfFormat getRDFFormat() {
        return RdfFormat.NQUADS;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            int lineNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }


                if (!line.endsWith(".")) {
                    logger.warn("Line {} does not end with a period and will be skipped: {}", lineNumber, line);

                    continue;
                }


                line = line.substring(0, line.length() - 1).trim();

                processLine(line, lineNumber);
            }
        } catch (Exception e) {
            if (e instanceof ParsingErrorException) {
                throw (ParsingErrorException) e;
            }
            throw new ParsingErrorException("Error reading N-Quads input: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a single N-Quads line, parsing its components and adding them to the model.
     * This method attempts to differentiate between N-Triples (default graph) and N-Quads (named graph).
     *
     * @param line       The N-Quads line to process.
     * @param lineNumber The line number for error reporting.
     * @throws ParsingErrorException if the line cannot be parsed according to N-Quads syntax.
     */
    private void processLine(String line, int lineNumber) throws ParsingErrorException {
        try {
            String[] parts = splitNqLine(line);

            if (parts.length < 3) {
                logger.error("Invalid N-Quads line (less than 3 parts) at line {}: {}", lineNumber, line);
                throw new ParsingErrorException("Invalid N-Quads line at line " + lineNumber + ": " + line);
            }
            if (parts.length > 4) {
                logger.warn("Line {} has more than 4 parts after initial split. This likely indicates a literal with spaces that was not handled correctly by the simple parser: {}", lineNumber, line);
                throw new ParsingErrorException("Invalid N-Quads line (too many parts, likely literal parsing issue) at line " + lineNumber + ": " + line);
            }

            Resource subject = parseResource(parts[0]);
            IRI predicate = parseIRI(parts[1]);
            Value object = parseValue(parts[2]);

            Resource graph = null;
            if (parts.length == 4) {
                graph = parseResource(parts[3]);
            }


            if (graph != null) {
                getModel().add(subject, predicate, object, graph);
            } else {

                getModel().add(subject, predicate, object);
            }

        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException("Error parsing N-Quads line " + lineNumber + ": " + line, e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error processing N-Quads line " + lineNumber + ": " + line, e);
        }
    }

    /**
     * A very simplistic split for N-Quads. It tries to identify the main components.
     * This will fail for complex literals containing spaces or quoted parts within,
     * especially when a graph IRI is also present.
     * A more robust parser would use a proper state machine or regex for parsing RDF terms.
     * It attempts to differentiate between a 3-part triple (default graph) and a 4-part quad (named graph).
     */
    private String[] splitNqLine(String line) {


        List<String> parts = new ArrayList<>();
        int currentPos = 0;

        for (int i = 0; i < 4; i++) {
            line = line.substring(currentPos).trim();
            if (line.isEmpty()) break;

            String term;
            if (line.startsWith("<")) {
                int endIndex = line.indexOf('>');
                if (endIndex == -1) throw new IllegalArgumentException("Unclosed IRI: " + line);
                term = line.substring(0, endIndex + 1);
                currentPos = term.length();
            } else if (line.startsWith("_:")) {
                int endIndex = line.indexOf(' ');
                if (endIndex == -1) endIndex = line.length();
                term = line.substring(0, endIndex);
                currentPos = term.length();
            } else if (line.startsWith("\"")) {
                int firstQuote = line.indexOf('"');
                int lastQuote = -1;

                boolean inEscape = false;
                for (int j = firstQuote + 1; j < line.length(); j++) {
                    char c = line.charAt(j);
                    if (c == '\\' && !inEscape) {
                        inEscape = true;
                    } else if (c == '"' && !inEscape) {
                        lastQuote = j;
                        break;
                    } else {
                        inEscape = false;
                    }
                }

                if (lastQuote == -1) throw new IllegalArgumentException("Unclosed literal: " + line);
                term = line.substring(0, lastQuote + 1);
                currentPos = term.length();


                String remainder = line.substring(lastQuote + 1).trim();
                if (remainder.startsWith("^^<")) {
                    int dtEndIndex = remainder.indexOf('>');
                    if (dtEndIndex == -1) throw new IllegalArgumentException("Unclosed datatype URI: " + remainder);
                    term += remainder.substring(0, dtEndIndex + 1);
                    currentPos += (dtEndIndex + 1) + (remainder.length() - remainder.trim().length());
                } else if (remainder.startsWith("@")) {
                    int langEndIndex = remainder.indexOf(' ');
                    if (langEndIndex == -1) langEndIndex = remainder.length();
                    term += remainder.substring(0, langEndIndex);
                    currentPos += (langEndIndex) + (remainder.length() - remainder.trim().length());
                }
            } else {

                throw new IllegalArgumentException("Malformed RDF term: " + line);
            }

            parts.add(term.trim());
            line = line.substring(currentPos);
            currentPos = 0;
        }

        return parts.toArray(new String[0]);
    }


    /**
     * Parses an N-Triples/N-Quads resource part (IRI or Blank Node).
     */
    private Resource parseResource(String part) {
        if (part.startsWith("<") && part.endsWith(">")) {
            return getValueFactory().createIRI(part.substring(1, part.length() - 1));
        } else if (part.startsWith("_:")) {
            return getValueFactory().createBNode(part.substring(2));
        }
        throw new IllegalArgumentException("Invalid N-Quads resource: " + part);
    }

    /**
     * Parses an N-Triples/N-Quads IRI part.
     */
    private IRI parseIRI(String part) {
        if (part.startsWith("<") && part.endsWith(">")) {
            return getValueFactory().createIRI(part.substring(1, part.length() - 1));
        }
        throw new IllegalArgumentException("Invalid N-Quads IRI: " + part);
    }

    /**
     * Parses an N-Triples/N-Quads value part (IRI, Blank Node, or Literal).
     * This is a highly simplified literal parser and does not handle complex escaping well.
     */
    private Value parseValue(String part) {
        if (part.startsWith("<") && part.endsWith(">")) {
            return getValueFactory().createIRI(part.substring(1, part.length() - 1));
        } else if (part.startsWith("_:")) {
            return getValueFactory().createBNode(part.substring(2));
        } else if (part.startsWith("\"")) {
            int lastQuote = part.lastIndexOf('"');
            if (lastQuote == -1 || lastQuote == 0) {
                throw new IllegalArgumentException("Invalid N-Quads literal: " + part);
            }
            String literalValue = part.substring(1, lastQuote);

            String remainder = part.substring(lastQuote + 1);

            if (remainder.startsWith("^^<") && remainder.endsWith(">")) {
                String datatypeUri = remainder.substring(3, remainder.length() - 1);
                return getValueFactory().createLiteral(literalValue, getValueFactory().createIRI(datatypeUri));
            } else if (remainder.startsWith("@")) {
                String lang = remainder.substring(1);
                return getValueFactory().createLiteral(literalValue, lang);
            } else if (remainder.isEmpty()) {
                return getValueFactory().createLiteral(literalValue);
            }
            throw new IllegalArgumentException("Invalid N-Quads literal format: " + part);
        }
        throw new IllegalArgumentException("Invalid N-Quads value: " + part);
    }
}