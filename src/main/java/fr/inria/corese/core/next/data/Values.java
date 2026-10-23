package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.factory.NativeValueFactory;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Triple;
import fr.inria.corese.core.next.data.api.term.Value;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

/**
 * Public entry point for creating RDF terms and statements independently of a
 * repository.
 *
 * <p>All static methods delegate to the thread-safe shared default {@link ValueFactory}.
 * When working inside a specific repository or storage backend, prefer obtaining the factory
 * from the repository itself so backend-optimized implementations can be used.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IRI person = Values.iri("http://schema.org/Person");
 * IRI name = Values.iri("http://schema.org/name");
 * BNode alice = Values.bnode();
 * Literal label = Values.literal("Alice", "en");
 * Statement stmt = Values.statement(alice, name, label);
 * }</pre>
 */
public final class Values {

    private static final ValueFactory DEFAULT_FACTORY = new NativeValueFactory();

    private Values() {
    }

    /**
     * Returns Corese's shared default RDF value factory.
     *
     * @return the default value factory
     */
    public static ValueFactory factory() {
        return DEFAULT_FACTORY;
    }

    /**
     * Creates an IRI from a full IRI string.
     *
     * @param iri valid IRI string
     * @return IRI instance
     */
    public static IRI iri(String iri) {
        return DEFAULT_FACTORY.createIRI(iri);
    }

    /**
     * Creates an IRI by concatenating a namespace and a local name.
     *
     * @param namespace namespace prefix or base URI
     * @param localName local name
     * @return IRI instance
     */
    public static IRI iri(String namespace, String localName) {
        return DEFAULT_FACTORY.createIRI(namespace, localName);
    }

    /**
     * Creates an anonymous blank node with an automatically generated identifier.
     *
     * @return unique blank node
     */
    public static BNode bnode() {
        return DEFAULT_FACTORY.createBNode();
    }

    /**
     * Creates a blank node with the given identifier.
     *
     * @param nodeId blank node label or identifier
     * @return blank node instance
     */
    public static BNode bnode(String nodeId) {
        return DEFAULT_FACTORY.createBNode(nodeId);
    }

    /**
     * Creates an untyped plain literal (xsd:string).
     *
     * @param label lexical value
     * @return Literal instance
     */
    public static Literal literal(String label) {
        return DEFAULT_FACTORY.createLiteral(label);
    }

    /**
     * Creates a language-tagged literal.
     *
     * @param label lexical value
     * @param language BCP-47 language tag
     * @return Literal instance typed as rdf:langString
     */
    public static Literal literal(String label, String language) {
        return DEFAULT_FACTORY.createLiteral(label, language);
    }

    /**
     * Creates a typed literal with an explicit datatype IRI.
     *
     * @param label lexical value
     * @param datatype datatype IRI
     * @return Literal instance
     */
    public static Literal literal(String label, IRI datatype) {
        return DEFAULT_FACTORY.createLiteral(label, datatype);
    }

    /**
     * Creates a literal typed as xsd:boolean.
     *
     * @param value boolean value
     * @return Literal instance
     */
    public static Literal literal(boolean value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:byte.
     *
     * @param value byte value
     * @return Literal instance
     */
    public static Literal literal(byte value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:short.
     *
     * @param value short value
     * @return Literal instance
     */
    public static Literal literal(short value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:int.
     *
     * @param value int value
     * @return Literal instance
     */
    public static Literal literal(int value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:long.
     *
     * @param value long value
     * @return Literal instance
     */
    public static Literal literal(long value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:float.
     *
     * @param value float value
     * @return Literal instance
     */
    public static Literal literal(float value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:double.
     *
     * @param value double value
     * @return Literal instance
     */
    public static Literal literal(double value) {
        return DEFAULT_FACTORY.createLiteral(value);
    }

    /**
     * Creates a literal typed as xsd:decimal.
     *
     * @param decimal BigDecimal value
     * @return Literal instance
     */
    public static Literal literal(BigDecimal decimal) {
        return DEFAULT_FACTORY.createLiteral(decimal);
    }

    /**
     * Creates a literal typed as xsd:integer.
     *
     * @param integer BigInteger value
     * @return Literal instance
     */
    public static Literal literal(BigInteger integer) {
        return DEFAULT_FACTORY.createLiteral(integer);
    }

    /**
     * Creates a literal typed as xsd:dateTime from a temporal accessor.
     *
     * @param temporal temporal value
     * @return Literal instance
     */
    public static Literal literal(TemporalAccessor temporal) {
        return DEFAULT_FACTORY.createLiteral(temporal);
    }

    /**
     * Creates a literal typed as xsd:duration from a temporal amount.
     *
     * @param amount temporal amount value
     * @return Literal instance
     */
    public static Literal literal(TemporalAmount amount) {
        return DEFAULT_FACTORY.createLiteral(amount);
    }

    /**
     * Creates a literal typed as xsd:dateTime from an XMLGregorianCalendar.
     *
     * @param calendar XMLGregorianCalendar value
     * @return Literal instance
     */
    public static Literal literal(XMLGregorianCalendar calendar) {
        return DEFAULT_FACTORY.createLiteral(calendar);
    }

    /**
     * Creates an RDF statement in the default graph (context is {@code null}).
     *
     * @param subject statement subject (Resource)
     * @param predicate statement predicate (IRI)
     * @param object statement object (Value)
     * @return Statement instance
     */
    public static Statement statement(Resource subject, IRI predicate, Value object) {
        return DEFAULT_FACTORY.createStatement(subject, predicate, object);
    }

    /**
     * Creates an RDF quad statement in the designated named graph context.
     *
     * @param subject statement subject (Resource)
     * @param predicate statement predicate (IRI)
     * @param object statement object (Value)
     * @param context graph context (Resource), or {@code null} for default graph
     * @return Statement instance
     */
    public static Statement statement(Resource subject, IRI predicate, Value object, Resource context) {
        return DEFAULT_FACTORY.createStatement(subject, predicate, object, context);
    }

    /**
     * Creates an RDF-star embedded triple term.
     *
     * @param subject triple subject (Resource)
     * @param predicate triple predicate (IRI)
     * @param object triple object (Value)
     * @return Triple instance
     */
    public static Triple triple(Resource subject, IRI predicate, Value object) {
        return DEFAULT_FACTORY.createTriple(subject, predicate, object);
    }
}
