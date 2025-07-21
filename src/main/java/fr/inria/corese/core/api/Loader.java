package fr.inria.corese.core.api;

import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.load.LoadException;
import java.io.InputStream;

public interface Loader {
    public enum format {
        RDFXML_FORMAT,
        RDFA_FORMAT,
        TURTLE_FORMAT,
        NT_FORMAT,
        JSONLD_FORMAT,
        RULE_FORMAT,
        QUERY_FORMAT,
        UNDEF_FORMAT,
        TRIG_FORMAT,
        NQUADS_FORMAT,
        WORKFLOW_FORMAT,
        OWL_FORMAT,
        XML_FORMAT,
        JSON_FORMAT
    }

    static final String JSONLD_FORMAT_STR = "application/ld+json";
    static final String JSON_FORMAT_STR = "application/json";
    static final String RDFXML_FORMAT_STR = "application/rdf+xml";
    static final String NQUADS_FORMAT_STR = "text/n-quads";
    static final String TRIG_FORMAT_STR = "text/trig";
    static final String NT_FORMAT_STR = "text/n3";
    static final String TURTLE_FORMAT_STR = "text/turtle";
    static final String HTML_FORMAT_STR = "text/html";
    static final String ALL_FORMAT_STR =
            "text/turtle;q=1.0, application/rdf+xml;q=0.9, application/ld+json;q=0.7, application/json;q=0.6";
    
	void init(Object o);
	
	boolean isRule(String path);
                  
        void parse(String path) throws LoadException;
       
        void parse(String path, String source) throws LoadException;
       
        void parse(String path, String source, String base, Loader.format format) throws LoadException;
		        
	
	@Deprecated
        void load(String path);
	
	@Deprecated
        void load(String path, String source);
        
	@Deprecated
        void load(String path, String base, String source, Loader.format format) throws LoadException;

	@Deprecated
        void load(InputStream stream, String str) throws LoadException;
	
	@Deprecated
	void loadWE(String path) throws LoadException;
	
	@Deprecated
	void loadWE(String path, String source) throws LoadException;
	
	RuleEngine getRuleEngine();

    format getFormat(String path);


}
