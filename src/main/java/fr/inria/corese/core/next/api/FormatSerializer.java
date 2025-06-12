package fr.inria.corese.core.next.api;

import fr.inria.corese.core.next.impl.exception.SerializationException;


public interface FormatSerializer {

    /**
     * Writes the RDF data (model/dataset) represented by this serializer instance
     * to the given in its specific serialization format.
     * The implementation must handle the formatting rules of the target RDF syntax.
     *
     * @param writer the which the serialized RDF data will be written.
     *               The writer will be flushed after writing.
     * @throws SerializationException if an error occurs during the serialization process,
     *                                such as an I/O error or if invalid data is encountered
     *                                that cannot be serialized to the target format.
     */
    void write(java.io.Writer writer) throws SerializationException;
}