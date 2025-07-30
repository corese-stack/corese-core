package fr.inria.corese.core.load;

import fr.inria.corese.core.compiler.parser.Pragma;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.query.QueryEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.sparql.triple.parser.Access;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;

import java.net.MalformedURLException;
import java.net.URL;


public class QueryLoad extends Load {

    private static final Logger logger = LoggerFactory.getLogger(QueryLoad.class);
    static final String NL = "\n";
    QueryEngine engine;

    QueryLoad() {
    }

    QueryLoad(QueryEngine e) {
        engine = e;
    }

    public static QueryLoad create() {
        return new QueryLoad();
    }

    public static QueryLoad create(QueryEngine e) {
        return new QueryLoad(e);
    }

    @Override
    @Deprecated
    public void loadWE(String name) throws LoadException {
        parse(name);
    }

    @Override
    public void parse(String name) throws LoadException {
        String q = readWE(name);
        if (q != null) {
            Query qq;
            try {
                qq = engine.defQuery(q);
            } catch (EngineException ex) {
                throw LoadException.create(ex).setPath(name);
            }
            if (qq != null) {
                qq.setPragma(Pragma.FILE, name);
            }
        }
    }

    public void load(Reader read) throws LoadException {
        parse(read);
    }

    public void parse(Reader read) throws LoadException {
        try {
            String q = read(read);
            if (q != null) {
                engine.defQuery(q);
            }
        } catch (IOException | EngineException ex) {
            throw new LoadException(ex);
        }
    }

    @Override
    boolean isURL(String path) {
        try {
            new URL(path);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    public String read(InputStream stream) throws IOException {
        return read(new InputStreamReader(stream));
    }

    public String readWE(InputStream stream) throws LoadException {
        try {
            return read(new InputStreamReader(stream));
        } catch (IOException ex) {
            throw new LoadException(ex);
        }
    }

    public String read(String name) {
        String query = "";
        try {
            query = readWE(name);
        } catch (LoadException ex) {
            LoggerFactory.getLogger(QueryLoad.class.getName()).error("", ex);
        }
        if (query == "") {
            return null;
        }
        return query;
    }

    public String readURL(String name) throws LoadException {
        return readWE(name);
    }

    public String readProtect(String name) throws LoadException {
        return readWE(name);
    }

    public String readWE(String name, boolean protect) throws LoadException {
        return readWE(name);
    }


    public String readWithAccess(String name) throws LoadException {
        check(Access.Feature.READ_WRITE, name, TermEval.READ_MESS);
        return readWE(name);
    }

    public String readWE(String name) throws LoadException {
        String query = "";
        try (Reader fr = getReaderForName(name)) {
            if (fr == null) {
                throw LoadException.create(new IOException("Could not obtain reader for: " + name)).setPath(name);
            }
            query = read(fr);
        } catch (IOException ex) {
            throw LoadException.create(ex).setPath(name);
        }
        if (query.isEmpty()) {
            return null;
        }
        return query;
    }


    /**
     * Helper method to get a Reader based on the name (resource, URL, or file path).
     * @param name The name of the resource/URL/file.
     * @return A Reader for the given name.
     * @throws IOException If an I/O error occurs or URL is malformed.
     */
    private Reader getReaderForName(String name) throws IOException {
        if (NSManager.isResource(name)) {
            InputStream stream = getClass().getResourceAsStream(NSManager.stripResource(name));
            if (stream == null) {
                throw new IOException("Resource not found: " + name);
            }
            return new InputStreamReader(stream);
        } else if (isURL(name)) {
            URL url = new URL(name);
            return new InputStreamReader(url.openStream());
        } else {
            return new FileReader(name);
        }
    }

    public String getResource(String name) throws IOException {
        InputStream stream = QueryLoad.class.getResourceAsStream(name);
        if (stream == null) {
            throw new IOException(name);
        }
        Reader fr = new InputStreamReader(stream);
        String str = read(fr);
        return str;
    }

    // TODO: clean
    public String basicParse(String path) throws EngineException {
        String pp = (path.endsWith("/")) ? path.substring(0, path.length() - 1) : path;
        String str = null;
        try {
            if (NSManager.isResource(pp)) {
                // @import <function/test.rq> within transformation such as st:turtle
                // the import uri is st:function/test.rq
                // consider it as a resource
                String name = NSManager.stripResource(pp);
                str = getResource(name);
            } else {
                str = readWE(pp);
            }
            return str;
        } catch (LoadException | IOException ex) {
            logger.error(ex.getMessage());
            throw new EngineException(ex);
        }
    }


    String read(Reader fr) throws IOException {
        BufferedReader fq = new BufferedReader(fr);
        StringBuilder sb = new StringBuilder();
        String str;
        boolean isnl = false;
        while (true) {
            str = fq.readLine();
            if (str == null) {
                fq.close();
                break;
            }
            if (isnl){
                sb.append(NL);
            }
            else {
                isnl = true;
            }
            sb.append(str);
        }
        return sb.toString();
    }


    public String writeTemp(String name, IDatatype dt) {
        try {
            File file = File.createTempFile(getName(name), getSuffix(name));
            writeToFile(file, dt);

            return file.toString();
        } catch (IOException e) {
            logger.error("Error writing to temporary file '{}': {}", name, e.getMessage(), e);
            return null;
        }
    }

    String getName(String name) {
        int index = name.indexOf(".");
        if (index == -1) {
            return name;
        }
        return name.substring(0, index);
    }

    String getSuffix(String name) {
        int index = name.indexOf(".");
        if (index == -1) {
            return ".txt";
        }
        return name.substring(index);
    }

    public void write(String name, IDatatype dt) {
        write(name, dt.stringValue());
    }

    public void write(String name, String str) {
        try (final FileWriter fr = new FileWriter(name);
             final BufferedWriter fq = new BufferedWriter(fr)) {
            fq.write(str);
            fq.flush();
        } catch (IOException e) {
            logger.error("Error writing to file '{}': {}", name, e.getMessage(), e);
        }
    }

    /**
     * Writes content to the specified file using a buffered writer.
     * Uses try-with-resources to ensure proper resource cleanup. The content
     * is written through the {@link #writeContentToBuffer(BufferedWriter, IDatatype)} method.
     *
     * @param file The file to write to
     * @param dt   The data content to be written
     * @throws IOException If an I/O error occurs during writing
     * @see java.io.BufferedWriter
     */
    private void writeToFile(File file, IDatatype dt) throws IOException {
        try (FileWriter fr = new FileWriter(file);
             BufferedWriter fq = new BufferedWriter(fr)) {
            writeContentToBuffer(fq, dt);
            fq.flush();
        }
    }

    /**
     * Helper method to write the content of an IDatatype to a BufferedWriter.
     * This method encapsulates the conditional writing logic to reduce nesting in the caller.
     *
     * @param fq The BufferedWriter to write to.
     * @param dt The IDatatype containing the content.
     * @throws IOException If an I/O error occurs during writing.
     */
    private void writeContentToBuffer(BufferedWriter fq, IDatatype dt) throws IOException {
        if (dt.isList()) {
            for (IDatatype elem : dt) {
                fq.write(elem.stringValue());
                fq.write(NL);
            }
        } else {
            fq.write(dt.stringValue());
        }
    }
}