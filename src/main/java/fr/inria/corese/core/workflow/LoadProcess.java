package fr.inria.corese.core.workflow;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.LoadFormat;
import fr.inria.corese.core.load.QueryLoad;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.exceptions.SafetyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load a directory.
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 */
public class LoadProcess extends WorkflowProcess {
    public static final String FILE = "file://";
    private static final Logger logger = LoggerFactory.getLogger(LoadProcess.class);
    private final Loader.format[] FORMATS = {Loader.format.TURTLE_FORMAT, Loader.format.RDFXML_FORMAT, Loader.format.JSONLD_FORMAT};
    String name;
    boolean rec = false;
    String text;
    Loader.format format = Loader.format.UNDEF_FORMAT;
    Loader.format requiredFormat = Loader.format.UNDEF_FORMAT;
    private boolean named = false;
    private PreProcessor processor;


    public LoadProcess(String path) {
        this.path = path;
    }

    public LoadProcess(String str, Loader.format format) {
        this.text = str;
        this.format = format;
        if (path == null) {
            path = "";
        }
    }

    public LoadProcess(String path, String name, boolean rec) {
        this.path = path;
        this.rec = rec;
        this.name = name;
    }

    public static LoadProcess createStringLoader(String str, Loader.format format) {
        return new LoadProcess(str, format);
    }

    void setRequiredFormat(String format) {
        Loader.format ft = LoadFormat.getDTFormat(format);
        requiredFormat = ft;
    }

    @Override
    void start(Data data) {
    }

    @Override
    void finish(Data data) {

    }

    @Override
    public Data run(Data data) throws EngineException {
        Graph g = data.getGraph();
        Load ld = Load.create(g);
        if (data.getDataManager() != null) {
            ld.setDataManager(data.getDataManager());
        }
        boolean isURL = true;
        try {
            if (getProcessor() != null) {
                process();
            }

            if (text != null && !hasMode()) {
                loadString(ld);
            } else {
                if (path.startsWith(FILE)) {
                    path = path.substring(FILE.length());
                    isURL = false;
                }

                if (isURL) {
                    // dbpedia return HTML by default
                    // if path has no suffix, set header accept format
                    ld.parseWithFormat(path, requiredFormat);
                } else {
                    ld.parseDir(path, name, rec);
                }
            }

        } catch (LoadException ex) {
            throw new EngineException(ex);
        }
        return new Data(this, null, g);
    }


    /**
     * Document is Shex, translate it to Shacl
     */
    void process() throws LoadException, SafetyException {
        if (text == null) {
            text = read(path);
        }

        text = getProcessor().translate(text);
    }

    /**
     * Try Turtle RDF/XML JSON-LD formats
     */
    void loadString(Load ld) throws LoadException {
        if (format == Loader.format.UNDEF_FORMAT) {
            for (Loader.format ft : FORMATS) {
                try {
                    ld.loadString(text, ft);
                    return;
                } catch (LoadException ex) {
                    // not right format
                    logger.warn("Load RDF string format: " + ft, ex);
                }
            }
        } else {
            ld.loadString(text, format);
        }
    }

    String read(String path) throws LoadException {
        return QueryLoad.create().readWE(path);
    }

    @Override
    public String stringValue(Data data) {
        return data.getGraph().toString();
    }

    /**
     * @return the processor
     */
    public PreProcessor getProcessor() {
        return processor;
    }

    /**
     * @param processor the processor to set
     */
    public void setProcessor(PreProcessor processor) {
        this.processor = processor;
    }

}
