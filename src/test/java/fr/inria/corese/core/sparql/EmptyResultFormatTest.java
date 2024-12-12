package fr.inria.corese.core.sparql;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.print.*;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests done to check the format for SELECT and INSERT
 */
public class EmptyResultFormatTest {

    @Test
    public void selectJSONTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("SELECT * {  }");
        JSONFormat resultJSON = JSONFormat.create(results);
        String resultString = resultJSON.toStringBuffer().toString();

        assertEquals("{\"head\": { \"vars\": []},\"results\": { \"bindings\": [{}] }}", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void insertJSONTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("INSERT DATA {  }");
        JSONFormat resultJSON = JSONFormat.create(results);
        String resultString = resultJSON.toStringBuffer().toString();

        assertEquals("{\"head\": { \"vars\": []},\"results\": { \"bindings\": [{}] }}", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void selectXMLTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("SELECT * {  }");
        XMLFormat resultXML = XMLFormat.create(results);
        String resultString = resultXML.toStringBuffer().toString();

        assertEquals("<?xml version=\"1.0\" ?><sparql xmlns='http://www.w3.org/2005/sparql-results#'><head></head><results><result></result></results></sparql>", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void insertXMLTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("INSERT DATA {  }");
        XMLFormat resultXML = XMLFormat.create(results);
        String resultString = resultXML.toStringBuffer().toString();

        assertEquals("<?xml version=\"1.0\" ?><sparql xmlns='http://www.w3.org/2005/sparql-results#'><head></head><results><result></result></results></sparql>", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void selectTSVTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("SELECT * {  }");
        TSVFormat resultTSV = TSVFormat.create(results);
        String resultString = resultTSV.toString();

        assertEquals("", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void insertTSVTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("INSERT DATA {  }");
        TSVFormat resultTSV = TSVFormat.create(results);
        String resultString = resultTSV.toString();

        assertEquals("", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void selectCSVTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("SELECT * {  }");
        CSVFormat resultCSV = CSVFormat.create(results);
        String resultString = resultCSV.toString();

        assertEquals("", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void insertCSVTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("INSERT DATA {  }");
        CSVFormat resultCSV = CSVFormat.create(results);
        String resultString = resultCSV.toString();

        assertEquals("", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void selectTripleTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("SELECT * {  }");
        TripleFormat resultTriple = TripleFormat.create(results);
        String resultString = resultTriple.toString();

        assertEquals("", resultString.trim().replaceAll("\n", ""));
    }

    @Test
    public void insertTripleTest() throws EngineException {
        Graph graph = Graph.create();
        QueryProcess queryProc = QueryProcess.create(graph);
        Mappings results = queryProc.query("INSERT DATA {  }");
        TripleFormat resultTriple = TripleFormat.create(results);
        String resultString = resultTriple.toString();

        assertEquals("", resultString.trim().replaceAll("\n", ""));
    }
}
