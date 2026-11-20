package fr.inria.corese.core.next.data.impl.io.serializer.ntriples;

import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTriplesSerializerDatatypePolicyTest {

    private MockStatementFactory factory;
    private Resource subject;
    private IRI predicate;
    private Literal stringLiteral;

    @BeforeEach
    void setUp() {
        factory = new MockStatementFactory();
        subject = factory.createIRI("http://example.org/s");
        predicate = factory.createIRI("http://example.org/p");
        stringLiteral = factory.createLiteral("hello", XSDDatatype.STRING.getIRI(), null);
    }

    @Test
    @DisplayName("ALWAYS_TYPED policy from RDFSerializerOptions should write xsd:string datatype")
    void alwaysTypedPolicyWritesXsdStringDatatype() {
        Statement stmt = factory.createStatement(subject, predicate, stringLiteral);

        RDFSerializerOptions opts = RDFSerializerOptions.builder()
                .literalDatatypePolicy(LiteralDatatypePolicy.ALWAYS_TYPED)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(List.of(stmt), opts).write(writer);

        assertTrue(writer.toString().contains("XMLSchema#string"),
                "ALWAYS_TYPED should write the xsd:string datatype");
    }

    @Test
    @DisplayName("MINIMAL policy from RDFSerializerOptions should omit xsd:string datatype")
    void minimalPolicyOmitsXsdStringDatatype() {
        Statement stmt = factory.createStatement(subject, predicate, stringLiteral);

        RDFSerializerOptions opts = RDFSerializerOptions.builder()
                .literalDatatypePolicy(LiteralDatatypePolicy.MINIMAL)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(List.of(stmt), opts).write(writer);

        assertFalse(writer.toString().contains("XMLSchema#string"),
                "MINIMAL should omit the xsd:string datatype");
    }
}
