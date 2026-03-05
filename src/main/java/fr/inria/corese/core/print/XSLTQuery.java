package fr.inria.corese.core.print;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.compiler.eval.QuerySolver;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Exec a sparql query inside XSLT using KGRAM
 * 
 * Add this in xslt header:
 *   xmlns:xalan="http://xml.apache.org/xalan"
 *   xmlns:server="xalan://fr.inria.corese.kgtool.print.XSLTQuery"
 *   extension-element-prefixes="server"
 *   
 * Exec query using this code:
 *   &lt;xsl:variable name='res'  select='server:sparql($engine, $query)' />
 * 
 * Olivier Corby &amp; Fabien Gandon, Edelweiss INRIA 2011
 * 
 */
public class XSLTQuery {

	private static final Logger logger = LoggerFactory.getLogger(XSLTQuery.class);

	String xsl;
	QuerySolver exec;
	
	
	XSLTQuery(String x, QuerySolver e){
		xsl = x;
		exec = e;
	}
	
	String query(String query){
		try {
			Mappings map = exec.query(query);
			Query q = map.getQuery();
			ASTQuery ast =  q.getAST();
			
			String str = null;
			if (q.isConstruct() && map.getGraph()!=null){
				Graph g = (Graph) map.getGraph();
				RDFFormat p = RDFFormat.create(g, ast.getNSM());
				str = p.toString();
			}
			else {
				XMLFormat f = XMLFormat.create(map);
				str = f.toString();
			}
			
			return str;
		} 
		catch (EngineException e) {
			// TODO Auto-generated catch block
			logger.error("Operation failure", e);
		}
		return null;
	}
	

	
	
	 /**
	   * creates a new document by parsing a string containing some XML.
	   *@param p_XML XML representation of the document.
	   *@return the document that resulted from the parsing.
	   */
	  public Document parseXML(String p_XML) {
	    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = null;
	    fac.setNamespaceAware(true);
	    try {
	      builder = fac.newDocumentBuilder();
	    }
	    catch (ParserConfigurationException l_pce) {
	    	//l_pclogger.error("Operation failure", e);
	    	//logger.error(l_pce);
	    }

	    Document l_Doc = null;
	    try {
	      l_Doc = builder.parse(new InputSource(new StringReader(p_XML)));
	      l_Doc.normalize();
	    }
	    catch (Exception l_e) {
	    	//l_logger.error("Operation failure", e);
	    	//logger.error(l_e);
	    }
	    return (l_Doc);
	  }
	  

}
