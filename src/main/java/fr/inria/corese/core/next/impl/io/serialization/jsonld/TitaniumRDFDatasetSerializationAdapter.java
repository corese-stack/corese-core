package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.rdf.*;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.*;

import static fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants.DEFAULT_GRAPH_IRI;

/**
 * Adapter class from Model to RdfDataset for usage in the JSON-LD serialization process using the titanium library.
 * @see <a href="https://github.com/filip26/titanium-rdf-api">Titanium RDF API</a>
 */
public class TitaniumRDFDatasetSerializationAdapter implements RdfDataset {

    private Model model;

    /**
     * Constructor for TitaniumRDFDatasetSerializationAdapter that initializes the model.
     *
     * @param model the model to be adapted
     */
    public TitaniumRDFDatasetSerializationAdapter(Model model) {
        this.model = model;
    }

    @Override
    public RdfGraph getDefaultGraph() {
        return new RdfGraph() {
            @Override
            public boolean contains(RdfTriple triple) {
                return model.contains(toResource(triple.getSubject()), toIRI(triple.getPredicate()), toValue(triple.getObject()));
            }

            @Override
            public List<RdfTriple> toList() {
                return model.stream().map(TitaniumRDFDatasetSerializationAdapter.this::toRdfTriple).toList();
            }
        };
    }

    @Override
    public RdfDataset add(RdfNQuad nquad) {
        return this;
    }

    @Override
    public RdfDataset add(RdfTriple triple) {
        return this;
    }

    @Override
    public List<RdfNQuad> toList() {
        List<Statement> resultStatement = this.model.stream().toList();
        return resultStatement.stream().map(this::toRdfNQuad).toList();
    }

    @Override
    public Set<RdfResource> getGraphNames() {
        HashSet<RdfResource> result = new HashSet<>();
        this.model.contexts().forEach(context -> {
            if(context != null) {
                result.add(toRdfResource(context));
            }
        });
        return result;
    }

    @Override
    public Optional<RdfGraph> getGraph(RdfResource graphName) {
        return Optional.of(new RdfGraph() {
            @Override
            public boolean contains(RdfTriple triple) {
                Resource graphResource = null;
                if (graphName != null && !graphName.getValue().equals(DEFAULT_GRAPH_IRI)) {
                    graphResource = toResource(graphName);
                }
                return model.contains(toResource(triple.getSubject()), toIRI(triple.getPredicate()), toValue(triple.getObject()), graphResource);
            }

            @Override
            public List<RdfTriple> toList() {
                List<RdfTriple> result = new ArrayList<>();
                Resource graphResource = null;
                if (graphName != null && !graphName.getValue().equals(DEFAULT_GRAPH_IRI)) {
                    graphResource = toResource(graphName);
                }
                model.getStatements(null, null, null, graphResource).forEach(statement -> result.add(toRdfNQuad(statement)));
                return result;
            }
        });
    }

    @Override
    public int size() {
        return this.model.size();
    }

    /**
     * Converts a Corese statement to a titanium RDF NQuad
     * @param statement the statement to convert
     * @return the converted statement
     */
    private RdfNQuad toRdfNQuad(Statement statement) {
        return new RdfNQuad() {
            @Override
            public Optional<RdfResource> getGraphName() {
                return Optional.of(toRdfResource(statement.getContext()));
            }

            @Override
            public RdfResource getSubject() {
                return toRdfResource(statement.getSubject());
            }

            @Override
            public RdfResource getPredicate() {
                return toRdfResource(statement.getPredicate());
            }

            @Override
            public RdfValue getObject() {
                return toRdfValue(statement.getObject());
            }
        };
    }

    /**
     * Converts a Corese statement to a titanium RDF triple
     * @param statement the statement to convert
     * @return the converted statement
     */
    private RdfTriple toRdfTriple(Statement statement) {
        return new RdfTriple() {
            @Override
            public RdfResource getSubject() {
                return toRdfResource(statement.getSubject());
            }

            @Override
            public RdfResource getPredicate() {
                return toRdfResource(statement.getPredicate());
            }

            @Override
            public RdfValue getObject() {
                return toRdfValue(statement.getObject());
            }
        };
    }

    /**
     * Converts a Corese resource to a titanium RDF resource
     * @param resource the resource to convert
     * @return the converted resource
     */
    private RdfResource toRdfResource(Resource resource) {
        if (resource != null && (! (resource.isBNode() || resource.isIRI()))) {
            throw new SerializationException("Unknown resource type " + resource, "JSON-LD");
        } else if (resource == null) {
            return null;
        }
        return new RdfResource() {
            @Override
            public boolean isIRI() {
                return resource.isIRI();
            }

            @Override
            public boolean isBlankNode() {
                return resource.isBNode();
            }

            @Override
            public String getValue() {
                return resource.stringValue();
            }
        };
    }

    /**
     * Converts a Corese value to a titanium RDF value
     * @param value the value to convert
     * @return the converted value
     */
    private RdfValue toRdfValue(Value value) {
        if (value.isIRI()) {
            return toRdfIRI((IRI) value);
        } else if (value.isBNode()) {
            return toRdfBlankNode((BNode) value);
        } else if (value.isLiteral()) {
            return toRdfLiteral((Literal) value);
        } else {
            throw new SerializationException("Unknown value type " + value.stringValue(), "JSON-LD");
        }
    }

    /**
     * Converts a Corese IRI to a titanium RDF Resource
     * @param iri the IRI to convert
     * @return the converted IRI
     */
    private RdfResource toRdfIRI(IRI iri) {
        return new RdfResource() {
            @Override
            public boolean isIRI() {
                return true;
            }
            @Override
            public String getValue() {
                return iri.stringValue();
            }
        };
    }

    /**
     * Converts a Corese BNode to a titanium RDF Resource
     * @param bnode the BNode to convert
     * @return the converted BNode
     */
    private RdfResource toRdfBlankNode(BNode bnode) {
        return new RdfResource() {
            @Override
            public boolean isBlankNode() {
                return true;
            }
            @Override
            public String getValue() {
                return bnode.stringValue();
            }
        };
    }

    /**
     * Converts a Corese Literal to a titanium RDF Literal
     * @param literal the Literal to convert
     * @return the converted Literal
     */
    private RdfLiteral toRdfLiteral(Literal literal) {
        return new RdfLiteral() {
            @Override
            public boolean isLiteral() {
                return true;
            }

            @Override
            public String getValue() {
                return literal.getLabel();
            }

            @Override
            public String getDatatype() {
                if (literal.getDatatype() != null
                        && ! (literal.getDatatype().equals(XSD.xsdString.getIRI())
                            || (literal.getDatatype().equals(RDF.langString.getIRI())
                                && literal.getLanguage().isPresent())
                        )
                    ) {
                    return literal.getDatatype().stringValue();
                } else if (literal.getLanguage().isPresent()) {
                    return RDF.langString.getIRI().stringValue();
                } else {
                    return XSD.xsdString.getIRI().stringValue();
                }
            }

            @Override
            public Optional<String> getLanguage() {
                return literal.getLanguage();
            }
        };
    }

    /**
     * Convert a Titanium RdfValue to a Corese Value
     * @param value the Titanium RdfValue
     * @return the Corese Value
     */
    private Value toValue(RdfValue value) {
        if (value.isIRI()) {
            return toIRI((RdfResource) value);
        } else if (value.isLiteral()) {
            return toLiteral((RdfLiteral) value);
        } else if (value.isBlankNode()) {
            return toBNode((RdfResource) value);
        } else {
            throw new SerializationException("Unknown value type " + value.getValue(), "JSON-LD");
        }
    }

    /**
     * Convert a Titanium RdfResource to a Corese Resource
     * @param resource the Titanium RdfResource
     * @return the Corese Resource
     */
    private Resource toResource(RdfResource resource) {
        if (resource.isIRI()) {
            return toIRI(resource);
        } else if (resource.isBlankNode()) {
            return toBNode(resource);
        } else {
            throw new SerializationException("Unknown resource type " + resource.getValue(), "JSON-LD");
        }
    }

    /**
     * Convert a Titanium RdfResource to a Corese IRI
     * @param resource the Titanium RdfResource
     * @return the Corese IRI
     */
    private IRI toIRI(RdfResource resource) {
        if(resource.isIRI()) {
            return stringToIRI(resource.getValue());
        }
        return null;
    }

    /**
     * Convert a Titanium RdfResource to a Corese BNode
     * @param resource the Titanium RdfResource
     * @return the Corese BNode
     */
    private BNode toBNode(RdfResource resource) {
        if(resource.isBlankNode()) {
            return new BNode() {
                @Override
                public String stringValue() {
                    return resource.getValue();
                }

                @Override
                public String getID() {
                    return resource.getValue();
                }
            };
        }
        return null;
    }

    /**
     * Convert a Titanium RdfLiteral to a Corese Literal
     * @param literal the Titanium RdfLiteral
     * @return the Corese Literal
     */
    private Literal toLiteral(RdfLiteral literal) {
        return new Literal() {
            @Override
            public String stringValue() {
                return literal.getValue();
            }

            @Override
            public String getLabel() {
                return literal.getValue();
            }

            @Override
            public Optional<String> getLanguage() {
                return literal.getLanguage();
            }

            @Override
            public IRI getDatatype() {
                return stringToIRI(literal.getDatatype());
            }

            @Override
            public boolean booleanValue() {
                return literal.getValue().equalsIgnoreCase("true");
            }

            @Override
            public byte byteValue() {
                return Byte.parseByte(literal.getValue());
            }

            @Override
            public short shortValue() {
                return Short.parseShort(literal.getValue());
            }

            @Override
            public int intValue() {
                return Integer.parseInt(literal.getValue());
            }

            @Override
            public long longValue() {
                return Long.parseLong(literal.getValue());
            }

            @Override
            public BigInteger integerValue() {
                return BigInteger.valueOf(Long.parseLong(literal.getValue()));
            }

            @Override
            public BigDecimal decimalValue() {
                return BigDecimal.valueOf(Double.parseDouble(literal.getValue()));
            }

            @Override
            public float floatValue() {
                return Float.parseFloat(literal.getValue());
            }

            @Override
            public double doubleValue() {
                return Double.parseDouble(literal.getValue());
            }

            @Override
            public TemporalAccessor temporalAccessorValue() {
                return LocalDateTime.parse(literal.getValue());
            }

            @Override
            public TemporalAmount temporalAmountValue() {
                return Duration.parse(literal.getValue());
            }

            @Override
            public XMLGregorianCalendar calendarValue() {
                try {
                    return DatatypeFactory.newInstance().newXMLGregorianCalendar(literal.getValue());
                } catch (DatatypeConfigurationException e) {
                    throw new SerializationException("Literal couldn't be converted to XMLGregorianCalendar", "JSON-LD", e);
                }
            }

            @Override
            public CoreDatatype getCoreDatatype() {
                return () -> stringToIRI(literal.getDatatype());
            }
        };
    }

    /**
     * Converts a string to an IRI.
     *
     * @param iri the string to convert
     * @return the IRI
     */
    private IRI stringToIRI(String iri) {
        if (iri == null || !IRIUtils.isStandardIRI(iri)) {
            throw new SerializationException("Invalid IRI: " + iri, "JSON-LD");
        }
        return new IRI() {
            @Override
            public String getNamespace() {
                return IRIUtils.guessNamespace(iri);
            }

            @Override
            public String getLocalName() {
                return IRIUtils.guessLocalName(iri);
            }

            @Override
            public String stringValue() {
                return iri;
            }
        };
    }
}
