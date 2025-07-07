package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.rdf.*;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.exception.SerializationException;
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

import static fr.inria.corese.core.next.impl.common.util.SerializationConstants.DEFAULT_GRAPH_IRI;

/**
 * Adapter class from Model to RdfDataset for usage in the JSON-LD serialization process using the titanium library.
 */
public class TitaniumRDFDatasetSerializationAdapter implements RdfDataset {

    private static final Logger logger = LoggerFactory.getLogger(TitaniumRDFDatasetSerializationAdapter.class);
    private Model model;

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
        return new HashSet<>(this.model.contexts().stream().map( context -> {
              if (context == null) {
                  return new RdfResource() {
                      @Override
                      public boolean isIRI() {
                          return true;
                      }

                      @Override
                      public boolean isBlankNode() {
                          return false;
                      }

                      @Override
                      public String getValue() {
                          return DEFAULT_GRAPH_IRI;
                      }
                  };
              }
               return toRdfResource(context);
        }).toList());
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

    private RdfTriple toRdfTriple(Statement statement) {
        return new RdfTriple() {
            @Override
            public RdfResource getSubject() {
                logger.debug("getSubject: {}", statement.getSubject().stringValue());
                return toRdfResource(statement.getSubject());
            }

            @Override
            public RdfResource getPredicate() {
                logger.debug("getPredicate: {}", statement.getPredicate().stringValue());
                return toRdfResource(statement.getPredicate());
            }

            @Override
            public RdfValue getObject() {
                logger.debug("getObject: {}", statement.getObject().stringValue());
                return toRdfValue(statement.getObject());
            }
        };
    }

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

    private RdfValue toRdfValue(Value value) {
        if (value.isIRI()) {
            logger.debug("toRdfValue: {} -> IRI", value.stringValue());
            return toRdfIRI((IRI) value);
        } else if (value.isBNode()) {
            logger.debug("toRdfValue: {} -> BNode", value.stringValue());
            return toRdfBlankNode((BNode) value);
        } else if (value.isLiteral()) {
            logger.debug("toRdfValue: {} -> Literal", value.stringValue());
            return toRdfLiteral((Literal) value);
        } else {
            throw new SerializationException("Unknown value type " + value.stringValue(), "JSON-LD");
        }
    }

    private RdfResource toRdfIRI(IRI iri) {
        return new RdfResource() {
            @Override
            public boolean isIRI() {
                return true;
            }
            @Override
            public boolean isBlankNode() {
                return false;
            }
            @Override
            public String getValue() {
                return iri.stringValue();
            }
        };
    }

    private RdfResource toRdfBlankNode(BNode bnode) {
        return new RdfResource() {
            @Override
            public boolean isIRI() {
                return false;
            }
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

    private RdfLiteral toRdfLiteral(Literal literal) {
        logger.debug("toRdfLiteral: {} {} {}", literal.stringValue(), literal.getDatatype().stringValue(), literal.getLanguage());
        return new RdfLiteral() {
            @Override
            public String getValue() {
                return literal.getLabel();
            }

            @Override
            public String getDatatype() {
                if (literal.getDatatype() != null && !literal.getDatatype().equals(XSD.xsdString.getIRI())) {
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
            throw new SerializationException("Unknown value type " + value.getValue(), "JSON-LD");
        }
    }

    private Resource toResource(RdfResource resource) {
        if (resource.isIRI()) {
            return toIRI(resource);
        } else if (resource.isBlankNode()) {
            return toBNode(resource);
        } else {
            throw new SerializationException("Unknown resource type " + resource.getValue(), "JSON-LD");
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
        logger.debug("Converting literal: {}", literal);
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
