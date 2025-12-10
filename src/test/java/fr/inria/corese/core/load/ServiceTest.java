package fr.inria.corese.core.load;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceTest {
    private final Logger logger = LoggerFactory.getLogger(ServiceTest.class);
    private Service service;
    @BeforeEach
    public void setUp() {
        service = new Service("https://dbpedia.org/sparql");
    }

   @Test
   public void select() throws LoadException, EngineException {
       Mappings mappings = service.select("select distinct ?Concept where {[] a ?Concept} LIMIT 100");
       logger.info("returned mappings = {}", mappings);
       assertEquals(100, mappings.size());
   }
}
