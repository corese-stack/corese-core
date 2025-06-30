package fr.inria.corese.core.next.impl.io.parser.ntriples;

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

/**
 * A simplified parser for N-Triples format.
 * This parser reads N-Triples line by line and attempts to parse
 * subject, predicate, and object, then adds them to the model.
 * It's a basic implementation and might not handle all edge cases or
 * complex RDF term structures (like intricate literal escaping) perfectly.
 * For production use, a more robust N-Triples parsing library would be recommended.
 */
public class NTriplesParser extends AbstractParser {

    private static final Logger logger = LoggerFactory.getLogger(NTriplesParser.class);

    public NTriplesParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    public NTriplesParser(Model model, ValueFactory factory, IOConfig config) {
        super(model, factory);
        setConfig(config);
    }

    @Override
    public RdfFormat getRDFFormat() {
        return RdfFormat.NTRIPLES;
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
                    logger.warn("Line {} does not end with a period: {}", lineNumber, line);
                }

                line = line.substring(0, line.length() - 1).trim();

                processLine(line, lineNumber);

            }
        } catch (Exception e) {

            if (e instanceof ParsingErrorException) {
                throw (ParsingErrorException) e;
            }
            throw new ParsingErrorException("Error reading N-Triples input: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a single N-Triples line, parsing its components and adding them to the model.
     *
     * @param line       The N-Triples line to process.
     * @param lineNumber The line number for error reporting.
     * @throws ParsingErrorException if the line cannot be parsed according to N-Triples syntax.
     */
    private void processLine(String line, int lineNumber) throws ParsingErrorException {
        try {
            String[] parts = splitNtLine(line);

            if (parts.length < 3) {
                logger.error("Invalid N-Triples line (less than 3 parts) at line {}: {}", lineNumber, line);
                throw new ParsingErrorException("Invalid N-Triples line at line " + lineNumber + ": " + line);
            }

            Resource subject = parseResource(parts[0]);
            IRI predicate = parseIRI(parts[1]);
            Value object = parseValue(parts[2]);

            getModel().add(subject, predicate, object);

        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException("Error parsing N-Triples line " + lineNumber + ": " + line, e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error processing N-Triples line " + lineNumber + ": " + line, e);
        }
    }

    /**
     * A very simplistic split for N-Triples. It tries to identify the main components.
     * This will fail for complex literals containing spaces or quoted parts within.
     * A more robust parser would use a proper state machine or regex for parsing RDF terms.
     */
    private String[] splitNtLine(String line) {
        int firstSpace = line.indexOf(' ');
        if (firstSpace == -1) return new String[0];
        String subjectPart = line.substring(0, firstSpace).trim();
        String remaining = line.substring(firstSpace).trim();

        int secondSpace = remaining.indexOf(' ');
        if (secondSpace == -1) return new String[0];
        String predicatePart = remaining.substring(0, secondSpace).trim();
        String objectPart = remaining.substring(secondSpace).trim();

        return new String[]{subjectPart, predicatePart, objectPart};
    }

    /**
     * Parses an N-Triples resource part (IRI or Blank Node).
     */
    private Resource parseResource(String part) {
        if (part.startsWith("<") && part.endsWith(">")) {
            return getValueFactory().createIRI(part.substring(1, part.length() - 1));
        } else if (part.startsWith("_:")) {
            return getValueFactory().createBNode(part.substring(2));
        }
        throw new IllegalArgumentException("Invalid N-Triples resource: " + part);
    }

    /**
     * Parses an N-Triples IRI part.
     */
    private IRI parseIRI(String part) {
        if (part.startsWith("<") && part.endsWith(">")) {
            return getValueFactory().createIRI(part.substring(1, part.length() - 1));
        }
        throw new IllegalArgumentException("Invalid N-Triples IRI: " + part);
    }

    /**
     * Parses an N-Triples value part (IRI, Blank Node, or Literal).
     * This is a highly simplified literal parser.
     */
    private Value parseValue(String part) {
        if (part.startsWith("<") && part.endsWith(">")) {
            return getValueFactory().createIRI(part.substring(1, part.length() - 1));
        } else if (part.startsWith("_:")) {
            return getValueFactory().createBNode(part.substring(2));
        } else if (part.startsWith("\"")) {
            int lastQuote = part.lastIndexOf('"');
            if (lastQuote == -1 || lastQuote == 0) {
                throw new IllegalArgumentException("Invalid N-Triples literal: " + part);
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
            throw new IllegalArgumentException("Invalid N-Triples literal format: " + part);
        }
        throw new IllegalArgumentException("Invalid N-Triples value: " + part);
    }
}
