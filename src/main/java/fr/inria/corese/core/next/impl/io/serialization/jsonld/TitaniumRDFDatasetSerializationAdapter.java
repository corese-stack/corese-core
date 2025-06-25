package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.rdf.*;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class TitaniumRDFDatasetSerializationAdapter implements RdfDataset {

    private static final Logger logger = LoggerFactory.getLogger(TitaniumRDFDatasetSerializationAdapter.class);
    private Model model;

    public TitaniumRDFDatasetSerializationAdapter(Model model) {
        this.model = model;
    }

    @Override
    public RdfGraph getDefaultGraph() {
        return null;
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
        return new HashSet<>(this.model.contexts().stream().map(this::toRdfResource).toList());
    }

    @Override
    public Optional<RdfGraph> getGraph(RdfResource graphName) {
        return Optional.of(new RdfGraph() {
            @Override
            public boolean contains(RdfTriple triple) {
                return model.contains(toResource(triple.getSubject()), toIRI(triple.getPredicate()), toValue(triple.getObject()));
            }

            @Override
            public List<RdfTriple> toList() {
                List<RdfTriple> result = new ArrayList<>();
                model.getStatements(null, null, null, toResource(graphName)).forEach(statement -> result.add(toRdfNQuad(statement)));
                return result;
            }
        });
    }

    @Override
    public int size() {
        return this.model.size();
    }

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

    private RdfResource toRdfResource(Resource resource) {
        return () -> {
            if (resource != null) {
                return resource.stringValue();
            } else {
                return null;
            }
        };
    }

    private RdfValue toRdfValue(Value value) {
        if (value.isResource()) {
            return toRdfResource((Resource) value);
        } else if (value.isLiteral()) {
            return toRdfLiteral((Literal) value);
        } else {
            throw new IllegalArgumentException("Unknown value type");
        }
    }

    private RdfLiteral toRdfLiteral(Literal literal) {
        return new RdfLiteral() {
            @Override
            public String getValue() {
                return literal.getLabel();
            }

            @Override
            public String getDatatype() {
                if (literal.getDatatype() != null) {
                    return literal.getDatatype().stringValue();
                } else {
                    return "";
                }
            }

            @Override
            public Optional<String> getLanguage() {
                return literal.getLanguage();
            }
        };
    }

    private Value toValue(RdfValue value) {
        if (value.isIRI()) {
            return toIRI((RdfResource) value);
        } else if (value.isLiteral()) {
            return toLiteral((RdfLiteral) value);
        } else if (value.isBlankNode()) {
            return toBNode((RdfResource) value);
        } else {
            throw new IllegalArgumentException("Unknown value type");
        }
    }

    private Resource toResource(RdfResource resource) {
        if (resource.isIRI()) {
            return toIRI(resource);
        } else if (resource.isBlankNode()) {
            return toBNode(resource);
        } else {
            throw new IllegalArgumentException("Unknown resource type");
        }
    }

    private IRI toIRI(RdfResource resource) {
        if(resource.isIRI()) {
            return stringToIRI(resource.getValue());
        }
        return null;
    }

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
                    logger.error("Literal couldn't be converted to XMLGregorianCalendar", e);
                }
                return null;
            }

            @Override
            public CoreDatatype getCoreDatatype() {
                return () -> stringToIRI(literal.getDatatype());
            }
        };
    }

    private IRI stringToIRI(String iri) {
        if (iri == null || !IRIUtils.isStandardIRI(iri)) {
            return null;
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
