package fr.inria.edelweiss.kgtool.load;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.edelweiss.kgenv.parser.Pragma;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgraph.query.QueryEngine;
import java.io.InputStream;
import java.net.MalformedURLException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

public class QueryLoad {

    private static Logger logger = LogManager.getLogger(QueryLoad.class);
    static final String HTTP = "http://";
    static final String FILE = "file://";
    static final String FTP = "ftp://";
    static final String[] PROTOCOLS = {HTTP, FILE, FTP};
    static final String NL = System.getProperty("line.separator");
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
    
    @Deprecated
    public void loadWE(String name) throws LoadException {
        parse(name);
    }
        
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
     
    @Deprecated
    public void load(String name) {
        String q = read(name);
        if (q != null) {
            try {
                Query qq = engine.defQuery(q);
                if (qq != null) {
                    qq.setPragma(Pragma.FILE, name);
                }
            } catch (EngineException e) {
                logger.error("Loading: " + name);
                e.printStackTrace();
            }
        }
    }
    
    
    @Deprecated
    public void load(Reader read) throws LoadException {
        parse(read);
    }

    public void parse(Reader read) throws LoadException {
        try {
            String q = read(read);
            if (q != null) {
                engine.defQuery(q);
            }
        } catch (IOException ex) {
            throw new LoadException(ex);
        } catch (EngineException ex) {
            throw new LoadException(ex);        
        }
    }

     boolean isURL(String path) {
        try {
            new URL(path);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }
    
    boolean isURL2(String name) {
        for (String s : PROTOCOLS) {
            if (name.startsWith(s)) {
                return true;
            }
        }
        return false;
    }
    
    @Deprecated
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

     @Deprecated
    public String read(String name) {
        String query = "";
        try {
            query = readWE(name);
        } catch (LoadException ex) {
            LogManager.getLogger(QueryLoad.class.getName()).log(Level.ERROR, "", ex);
        }
        if (query == "") {
            return null;
        }
        return query;
    }

    public String readWE(String name) throws LoadException {
        String query = "", str = "";
        Reader fr;
        try {
            if (isURL(name)) {
                URL url = new URL(name);
                fr = new InputStreamReader(url.openStream());
            } else {
                fr = new FileReader(name);
            }

            query = read(fr);
        } catch (IOException ex) {
            throw LoadException.create(ex).setPath(name);
        }
        if (query == "") {
            return null;
        }
        return query;
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
            //sb.append(NL);
        }
        return sb.toString();
    }

    public void write(String name, String str) {
        String query = "";
        try {
            Writer fr = new FileWriter(name);
            BufferedWriter fq = new BufferedWriter(fr);
            fq.write(str);
            fq.flush();
            fr.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }

    }
}
