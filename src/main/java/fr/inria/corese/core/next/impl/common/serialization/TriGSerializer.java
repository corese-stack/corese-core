package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.impl.common.serialization.base.AbstractGraphSerializer;
import fr.inria.corese.core.next.impl.common.serialization.config.TriGConfig;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Serializes a {@link Model} to TriG format with comprehensive syntax support.
 * This class provides a method to write the declarations of a model to a {@link Writer}
 * in accordance with the TriG specification, taking into account configuration options.
 *
 * <p>This implementation handles:</p>
 * <ul>
 * <li>Declaration and usage of prefixes for IRIs, including auto-declaration and sorting.</li>
 * <li>The 'a' shortcut for 'rdf:type'.</li>
 * <li>Escaping of special characters in literals (single-line and multi-lines) and IRIs.</li>
 * <li>Basic pretty-printing (indentation, end-of-line dots).</li>
 * <li>Management of literal datatype policies (minimal or always typed).</li>
 * <li>Serialization of compact triples (semicolons, commas) to group subjects and predicates.</li>
 * <li>Serialization of nested blank nodes using the '[]' syntax.</li>
 * <li>Serialization of RDF collections (lists) using the '()' syntax.</li>
 * <li>Detection and prevention of infinite loops during serialization of nested blank nodes and lists.</li>
 * <li>Sorting of subjects and predicates if configured.</li>
 * <li><b>Serialization of named graphs using the `{}` syntax for TriG.</b></li>
 * </ul>
 * <p>Advanced features such as strict adherence to maximum line length
 * and generation of stable blank node identifiers are not fully implemented in this version.</p>
 */
public class TriGSerializer extends AbstractGraphSerializer {

    /**
     * Constructs a new {@code TriGSerializer} instance with the specified model and default configuration.
     * The default configuration is returned by {@link TriGConfig#defaultConfig()}.
     *
     * @param model the {@link Model} to serialize. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public TriGSerializer(Model model) {
        this(model, TriGConfig.defaultConfig());
    }

    /**
     * Constructs a new {@code TriGSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param config the {@link TriGConfig} to use for serialization. Must not be null.
     *               This config object should be an instance of {@code TriGConfig} or a subclass thereof.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    public TriGSerializer(Model model, TriGConfig config) {
        super(model, config);
        Objects.requireNonNull(config, "TriGConfig cannot be null");
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return "TriG"
     */
    @Override
    protected String getFormatName() {
        return "TriG";
    }

    /**
     * Helper method to safely cast the generic config to TriGConfig.
     * This should be called before accessing any methods specific to TriGConfig.
     *
     * @return The config cast to TriGConfig.
     * @throws IllegalStateException if the config is not an instance of TriGConfig.
     */
    private TriGConfig getTriGConfig() {
        if (!(config instanceof TriGConfig)) {
            throw new IllegalStateException("Current serializer configuration is not an instance of TriGConfig. " +
                    "TriGSerializer requires a TriGConfig instance.");
        }
        return (TriGConfig) config;
    }

    /**
     * Implements the main statement writing logic for the TriG format.
     * Handles the serialization of named graphs.
     *
     * @param writer the {@link Writer} to which the statements will be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void doWriteStatements(Writer writer) throws IOException {
        TriGConfig trigConfig = getTriGConfig();

        if (trigConfig.includeContext()) {
            writeStatementsWithContext(writer);
        } else if (trigConfig.useCompactTriples() && trigConfig.groupBySubject()) {
            writeOptimizedStatements(writer);
        } else {
            writeSimpleStatements(writer);
        }
    }

    /**
     * Serializes statements, grouping them by named graph context.
     * Statements without a context are considered part of the default graph.
     * This method is used when {@code includeContext} is true.
     *
     * @param writer the {@link Writer} to which the statements will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeStatementsWithContext(Writer writer) throws IOException {
        TriGConfig trigConfig = getTriGConfig();

        Map<Resource, List<Statement>> byContext = new LinkedHashMap<>();
        model.stream()
                .filter(stmt -> !isConsumed(stmt.getSubject()))
                .forEach(stmt -> byContext.computeIfAbsent(stmt.getContext(), k -> new ArrayList<>()).add(stmt));

        for (Map.Entry<Resource, List<Statement>> contextEntry : byContext.entrySet()) {
            Resource context = contextEntry.getKey();
            List<Statement> statementsInContext = contextEntry.getValue();

            String initialIndent = "";
            String graphIndent = trigConfig.prettyPrint() ? trigConfig.getIndent() : "";

            if (context != null) {
                if (context.isIRI()) {
                    writeIRI(writer, (IRI) context);
                } else if (context.isBNode()) {
                    writeValue(writer, context);
                }
                writer.write(SerializationConstants.SPACE);
                writer.write(SerializationConstants.OPEN_BRACE);
                writer.write(trigConfig.getLineEnding());
                initialIndent = graphIndent;
            }

            Map<Resource, List<Statement>> bySubject = trigConfig.sortSubjects() ?
                    new TreeMap<>(Comparator.comparing(Resource::stringValue)) :
                    new LinkedHashMap<>();

            statementsInContext.forEach(stmt -> bySubject.computeIfAbsent(stmt.getSubject(), k -> new ArrayList<>()).add(stmt));

            for (Map.Entry<Resource, List<Statement>> subjectEntry : bySubject.entrySet()) {
                writer.write(initialIndent);
                writeValue(writer, subjectEntry.getKey());
                writer.write(SerializationConstants.SPACE);

                Map<IRI, List<Statement>> byPredicate = trigConfig.sortPredicates() ?
                        new TreeMap<>(Comparator.comparing(IRI::stringValue)) :
                        new LinkedHashMap<>();

                subjectEntry.getValue().forEach(stmt -> byPredicate.computeIfAbsent(stmt.getPredicate(), k -> new ArrayList<>()).add(stmt));

                boolean firstPredicate = true;
                for (Map.Entry<IRI, List<Statement>> predicateEntry : byPredicate.entrySet()) {
                    if (!firstPredicate) {
                        writer.write(SerializationConstants.SEMICOLON);
                        if (trigConfig.prettyPrint()) {
                            writer.write(trigConfig.getLineEnding() + initialIndent + trigConfig.getIndent());
                        } else {
                            writer.write(SerializationConstants.SPACE);
                        }
                    }
                    firstPredicate = false;

                    writePredicate(writer, predicateEntry.getKey());
                    writer.write(SerializationConstants.SPACE);

                    boolean firstObject = true;
                    for (Statement stmt : predicateEntry.getValue()) {
                        if (!firstObject) {
                            writer.write(SerializationConstants.COMMA);
                            if (trigConfig.prettyPrint()) {
                                writer.write(trigConfig.getLineEnding() + initialIndent + trigConfig.getIndent() + trigConfig.getIndent());
                            } else {
                                writer.write(SerializationConstants.SPACE);
                            }
                        }
                        firstObject = false;
                        writeValue(writer, stmt.getObject());
                    }
                }
                writer.write(SerializationConstants.SPACE + SerializationConstants.POINT);
                writer.write(trigConfig.getLineEnding());
            }

            if (context != null) {
                writer.write(SerializationConstants.CLOSE_BRACE);
                writer.write(SerializationConstants.SPACE);
                writer.write(SerializationConstants.POINT);
                writer.write(trigConfig.getLineEnding());
            }
            writer.write(trigConfig.getLineEnding());
        }
    }

    /**
     * Escapes characters in an IRI string for TriG output.
     * This method primarily focuses on control characters and problematic characters within angle brackets.
     *
     * @param iri The IRI to escape.
     * @return The escaped IRI.
     */
    @Override
    protected String escapeIRIString(String iri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iri.length(); i++) {
            char c = iri.charAt(i);
            if (c < 0x20 || c == 0x7F || c == SerializationConstants.LT.charAt(0) || c == SerializationConstants.GT.charAt(0) || c == SerializationConstants.QUOTE.charAt(0) || c == '{' || c == '}' || c == '|' || c == '^' || c == '`' || c == SerializationConstants.BACK_SLASH.charAt(0)) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in TriG string literals.
     * Handles backslashes, double quotes, and common control characters.
     * Unicode escape sequences are used for unprintable characters if `escapeUnicode` is true.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for a TriG literal.
     */
    @Override
    protected String escapeLiteralString(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                    sb.append(SerializationConstants.BACK_SLASH).append("n");
                    break;
                case '\r':
                    sb.append(SerializationConstants.BACK_SLASH).append("r");
                    break;
                case '\t':
                    sb.append(SerializationConstants.BACK_SLASH).append("t");
                    break;
                case '\b':
                    sb.append(SerializationConstants.BACK_SLASH).append("b");
                    break;
                case '\f':
                    sb.append(SerializationConstants.BACK_SLASH).append("f");
                    break;
                case '"':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.QUOTE);
                    break;
                case '\\':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.BACK_SLASH);
                    break;
                default:
                    if (config.escapeUnicode() && (c <= 0x1F || c == 0x7F || (c >= 0x80 && c <= 0xFFFF))) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (Character.isHighSurrogate(c)) {
                        int codePoint = value.codePointAt(i);
                        if (Character.isValidCodePoint(codePoint)) {
                            sb.append(String.format("\\U%08X", codePoint));
                            i++;
                        } else {
                            sb.append(c);
                        }
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in multi-line literals (triple-quotes).
     * Primarily used to escape occurrences of `"""` within the literal.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for a TriG multi-line literal.
     */
    @Override
    protected String escapeMultilineLiteralString(String value) {
        return value.replace(SerializationConstants.QUOTE + SerializationConstants.QUOTE + SerializationConstants.QUOTE,
                SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE +
                        SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE +
                        SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE);
    }
}
