package fr.inria.corese.core.next.common.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RdfTextTest {

    @Test
    void stripsPairedIriBrackets() {
        assertEquals("https://example.org/resource", RdfText.stripAngleBrackets(
                "  <https://example.org/resource>  "));
    }

    @Test
    void preservesUnpairedIriBrackets() {
        assertEquals("<https://example.org/resource", RdfText.stripAngleBrackets(
                "<https://example.org/resource"));
    }

    @Test
    void stripsTrailingPrefixColon() {
        assertEquals("example", RdfText.stripTrailingColon(" example: "));
    }

    @Test
    void extractsLocaleIndependentIriLocalName() {
        assertEquals("resource", RdfText.localNameFromIriToken(
                "<https://example.org/RESOURCE>"));
    }

    @Test
    void stripsVariableMarkerAfterWhitespace() {
        assertEquals("variable", RdfText.stripVariableMarker("  ?variable  "));
    }
}
