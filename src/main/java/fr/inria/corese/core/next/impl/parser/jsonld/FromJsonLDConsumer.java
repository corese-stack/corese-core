package fr.inria.corese.core.next.impl.parser.jsonld;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import fr.inria.corese.core.next.api.*;

public class FromJsonLDConsumer implements RdfQuadConsumer {

    private Model model;
    private final ValueFactory valueFactory;

    FromJsonLDConsumer(Model model, ValueFactory valueFactory) {
        this.model = model;
        this.valueFactory = valueFactory;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String object, String datatype, String language, String direction, String graph) throws RdfConsumerException  {
        if(subject != null || predicate != null || object != null) {
            // Subject
            Resource subjectRes = null;
            if(RdfQuadConsumer.isBlank(subject)) {
                subjectRes = valueFactory.createBNode(subject);
            } else {
                subjectRes = valueFactory.createIRI(subject);
            }

            // Property
            IRI propertyRes = valueFactory.createIRI(predicate);

            // Object
            Value objectRes = null;
            if(RdfQuadConsumer.isValidObject(datatype, language, direction)) {
                if(RdfQuadConsumer.isLiteral( datatype, language, direction)) {
                    if(language != null) {
                        objectRes = valueFactory.createLiteral(object, language);
                    } else {
                        if (datatype != null) {
                            IRI datatypeIRI = this.valueFactory.createIRI(datatype);
                            objectRes = valueFactory.createLiteral(object, datatypeIRI);
                        } else {
                            objectRes = valueFactory.createLiteral(object);
                        }
                    }
                } else if(RdfQuadConsumer.isBlank(object)) {
                    objectRes = valueFactory.createBNode(object);
                } else {
                    objectRes = valueFactory.createIRI(object);
                }
            }
            this.model.add(subjectRes, propertyRes, objectRes);
        }
        return this;
    }
}
