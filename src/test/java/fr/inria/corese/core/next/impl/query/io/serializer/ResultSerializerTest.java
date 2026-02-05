package fr.inria.corese.core.next.impl.query.io.serializer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public interface ResultSerializerTest {

    @Test
    @DisplayName("Tests the serialization of an empty result")
    void emptyResult();

    @Test
    @DisplayName("Tests the serialization of results containing only URIs")
     void resultsWithUris();

    @Test
    @DisplayName("Tests the serialization of results containing only literals")
    void resultsWithLiterals();

    @Test
    @DisplayName("Tests the serialization of results with blank nodes")
    void resultsWithBlankNodes();

    @Test
    @DisplayName("Tests the serialization of results of at least 2 lines")
    void resultsWithMultipleLines();

    @Test
    @DisplayName("Tests the serialization of a result containing a literal that contains break lines")
    void resultsWithMultiLinesLiteral();
}
