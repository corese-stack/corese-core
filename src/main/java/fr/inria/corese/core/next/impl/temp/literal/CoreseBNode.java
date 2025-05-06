package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.base.model.AbstractBNode;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * An implementation of a blank node (BNode) used by Corese.
 * A blank node (BNode) represents an unnamed node in an RDF graph, typically used to represent resources that do not have a globally unique identifier (IRI).
 * This class extends {@link AbstractBNode} and provides constructors for creating a BNode either from a Corese blank node object or a given string identifier.
 */
public class CoreseBNode extends AbstractBNode {
    /**
     * The Corese object representing the blank node in the old API.
     */
    private final fr.inria.corese.core.sparql.datatype.CoreseBlankNode coreseObject;

    /**
     * The unique identifier (ID) for the blank node.
     */
    private String id;

    /**
     * Constructs a {@link CoreseBNode} instance from an {@link IDatatype} Corese object.
     * The Corese object should be an instance of {@link fr.inria.corese.core.sparql.datatype.CoreseBlankNode}.
     *
     * @param coreseObject The {@link IDatatype} Corese object representing the blank node.
     * @throws IncorrectOperationException If the provided {@link IDatatype} is not a valid {@link fr.inria.corese.core.sparql.datatype.CoreseBlankNode}.
     */
    public CoreseBNode(IDatatype coreseObject) {
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseBlankNode) {
            this.coreseObject = ( fr.inria.corese.core.sparql.datatype.CoreseBlankNode) coreseObject;
            this.id = this.coreseObject.getID();
        }
        else {
            throw new IncorrectOperationException("Cannot create CoreseLiteral from a non-literal Corese object");
        }
    }

    /**
     * Constructs a {@link CoreseBNode} instance from a string identifier.
     * This constructor creates a {@link fr.inria.corese.core.sparql.datatype.CoreseBlankNode} from the provided string id.
     *
     * @param id The unique identifier for the blank node.
     */
    public CoreseBNode(String id) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseBlankNode(id));
        this.id = id;
    }

    /**
     * Returns the unique identifier of the blank node.
     *
     * @return The ID of the blank node.
     */
    @Override
    public String getID() {
        return id;
    }
}
