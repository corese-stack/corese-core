package fr.inria.corese.core.print;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.datatype.RDF;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * This class provides functionality to convert a Graph object to a string
 * in N-Triples format.
 */
public class NTriplesFormat extends RDFFormat {
    private static final String SPACE = " ";
    private static final String OPEN_ANGLE = "<";
    private static final String CLOSE_ANGLE = ">";
    private static final String QUOTE = "\"";
    private static final String BLANK_NODE_PREFIX = "_:";

    /**
     * The graph to be formatted.
     */
    protected final Graph graph;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * @param graph the graph to be formatted
     */
    public NTriplesFormat(Graph graph) {
        super(graph, NSManager.create());
        this.graph = graph;
    }

    /**
     * Factory method to create a new NTriplesFormat instance.
     *
     * @param graph the graph to be formatted
     * @return a new NTriplesFormat instance
     */
    public static NTriplesFormat create(Graph graph) {
        return new NTriplesFormat(graph);
    }

    /**
     * Factory method to create a new NTriplesFormat instance.
     *
     * @param map the mappings to be formatted
     * @return a new NTriplesFormat instance
     */
    public static NTriplesFormat create(Mappings map) {
        return new NTriplesFormat((Graph) map.getGraph());
    }

    /**
     * Converts the graph to a string in N-Triples format.
     *
     * @return a string representation of the graph in N-Triples format
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Edge e : graph.getEdges()) {
            Edge edge = this.graph.getEdgeFactory().copy(e);

            sb.append(printNode(edge.getNode(0)))
                    .append(SPACE)
                    .append(printNode(edge.getEdgeNode()))
                    .append(SPACE)
                    .append(printNode(edge.getNode(1)))
                    .append(" .\n");
        }

        return sb.toString();
    }

    /**
     * Writes the graph to an output stream in N-Triples format.
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(OutputStream out) throws IOException {
        out.write(this.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a node to a string based on its type (URI or Literal).
     *
     * @param node the node to be formatted
     * @return a string representation of the node
     */
    public String printNode(Node node) {
        if (node.getDatatypeValue().isURI()) {
            return printURI(node);
        } else if (node.getDatatypeValue().isLiteral()) {
            return printDatatype(node);
        } else if (node.isBlank()) {
            return printBlank(node);
        } else {
            throw new IllegalArgumentException("Node " + node + " is not a URI, Literal, or blank node.");
        }
    }

    /**
     * Converts a URI node to a string.
     *
     * @param node the URI node to be formatted
     * @return a string representation of the URI node
     */
    private String printURI(Node node) {
        return OPEN_ANGLE + node.getLabel() + CLOSE_ANGLE;
    }

    /**
     * Converts a Literal node to a string.
     *
     * @param node the Literal node to be formatted
     * @return a string representation of the Literal node
     */
    private String printDatatype(Node node) {
        String label = escape(node.getLabel());
        String language = node.getDatatypeValue().getLang();
        String datatype = node.getDatatypeValue().getDatatypeURI();

        if (language != null && !language.isEmpty()) {
            return QUOTE + label + "\"@" + language;
        } else if (datatype != null && !datatype.isEmpty() && !datatype.equals(RDF.xsdstring)) {
            return QUOTE + label + "\"^^<" + datatype + CLOSE_ANGLE;
        } else {
            return QUOTE + label + QUOTE;
        }
    }

    /**
     * Converts a blank node to a string.
     *
     * @param node the blank node to be formatted
     * @return a string representation of the blank node
     */
    protected String printBlank(Node node) {
        // Les nœuds vides en N-Triples doivent commencer par "_:"
        return BLANK_NODE_PREFIX + node.getLabel();
    }

    /**
     * Escapes special characters in a string.
     *
     * @param str the string to be escaped
     * @return the escaped string
     */
    private String escape(String str) {
        StringBuilder escaped = new StringBuilder();
        for (char ch : str.toCharArray()) {
            switch (ch) {
                // Backslash
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\"': // Double quote
                    escaped.append("\\\"");
                    break;
                case '\n': // Line Feed
                    escaped.append("\\n");
                    break;
                case '\r': // Carriage Return
                    escaped.append("\\r");
                    break;
                case '\t': // Horizontal Tab
                    escaped.append("\\t");
                    break;
                case '\b': // Backspace
                    escaped.append("\\b");
                    break;
                case '\f': // Form Feed
                    escaped.append("\\f");
                    break;
                default:
                    // Uses UCHAR for specific characters and those outside the Char production of
                    // XML 1.1
                    if ((ch >= '\u0000' && ch <= '\u0007') || ch == '\u000B' || (ch >= '\u000E' && ch <= '\u001F')
                            || ch == '\u007F') {
                        escaped.append(String.format("\\u%04X", (int) ch));
                    } else {
                        // Uses the native representation for all other characters
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }
}
