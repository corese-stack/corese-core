package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.impl.exception.InternalException;
import fr.inria.corese.core.next.impl.temp.literal.*;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * A utility class for converting between Corese-specific data types and next Corese API representations.
 * This class provides methods to convert Corese nodes to next Corese API values and vice versa.
 *
 * <p>The class includes two main methods:
 * <ul>
 * <li>{@link #coreseNodeToRdf4jValue(Node)}: Converts a Corese {@link Node} to a next Corese API {@link Value}.</li>
 * <li>{@link #convert(Value)}: Converts a next Corese API {@link Value} to a corresponding Corese {@link Node}.</li>
 * </ul>
 * </p>
 *
 * <p>Additionally, the {@link #convert(IDatatype)} method handles conversions from various Corese data types
 * (such as CoreseBoolean, CoreseDate, CoreseInteger, and CoreseLiteral) to RDF4J literals.</p>
 */
public class CoreseConverter {

    static CoreseAdaptedValueFactory factory = new CoreseAdaptedValueFactory();

    /**
     * Converts a Corese {@link Node} to an next API {@link Value}.
     * This method extracts the Corese datatype value and returns the corresponding next API value.
     *
     * @param corese_node the Corese {@link Node} to be converted (can be null)
     * @return the corresponding next API {@link Value} (can be null if input is null)
     */
    public static Value coreseNodeToRdf4jValue(Node corese_node) {
        if (corese_node == null) {
            return null;
        }
        return convert(corese_node.getDatatypeValue());
    }

    /**
     * Converts a Corese {@link IDatatype} to a next API {@link Value}.
     *
     * @param oldCoreseDatatype the Corese {@link IDatatype} to be converted
     * @return the corresponding next API {@link Value}
     * @throws InternalException if the provided datatype is not recognized
     */
    public static Value convert(IDatatype oldCoreseDatatype) {
        if (oldCoreseDatatype.isURI()) {
            return new CoreseIRI(oldCoreseDatatype.stringValue());
        }
        else {
            String datatypeStringURI  = oldCoreseDatatype.getDatatypeURI();
            IRI datatypeStringIRI = factory.createIRI(datatypeStringURI);
            return factory.createLiteral(oldCoreseDatatype.getLabel(), datatypeStringIRI);
        }
    }

    /**
     * Converts a next API {@link Value} to a corresponding Corese {@link Node}.
     *
     * @param value the next API {@link Value} to be converted
     * @return the corresponding Corese {@link Node}
     * @throws InternalException if the provided value is not recognized
     */
    public static Node convert(Value value) {

        if (value.isIRI()) {
            CoreseIRI coreseIRI = new CoreseIRI(value.stringValue());
            return coreseIRI.getCoreseNode();
        } else if (value.isLiteral()) {
            Literal literal = (Literal) value;

            if (literal instanceof AbstractCoreseNumber) {
                return ((AbstractCoreseNumber) literal).getCoreseNode();
            } else if (literal instanceof CoreseBoolean) {
                return ((CoreseBoolean) literal).getCoreseNode();
            } else if (literal instanceof CoreseDate) {
                return ((CoreseDate) literal).getCoreseNode();
            } else if (literal instanceof CoreseDatetime) {
                return ((CoreseDatetime) literal).getCoreseNode();
            } else if (literal instanceof CoreseDecimal) {
                return ((CoreseDecimal) literal).getCoreseNode();
            } else if (literal instanceof CoreseInteger) {
                return ((CoreseInteger) literal).getCoreseNode();
            } else if (literal instanceof CoreseDuration) {
                return ((CoreseDuration) literal).getCoreseNode();
            } else if (literal instanceof CoreseLanguageTaggedStringLiteral) {
                return ((CoreseLanguageTaggedStringLiteral) literal).getCoreseNode();
            } else if (literal instanceof CoreseTime) {
                return ((CoreseTime) literal).getCoreseNode();
            } else {
                CoreseTyped coreseTyped = new CoreseTyped(value.stringValue());
                return coreseTyped.getCoreseNode();
            }
        } else if (value.isResource()) {
            Resource resource = (Resource) value;
            if (resource instanceof CoreseIRI) {
                return ((CoreseIRI) resource).getCoreseNode();
            } else {
                throw new InternalException("Unexpected value: " + resource);
            }
        } else {
            throw new InternalException("Unexpected value: " + value);
        }
    }
}