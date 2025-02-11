package fr.inria.corese.core.compiler.federate.util;

import fr.inria.corese.core.sparql.triple.parser.NSManager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class ResourceReader {

    public String readWE(String name) throws IOException {
        String query = "", str = "";
        Reader fr;
        if (NSManager.isResource(name)) {
            fr = new InputStreamReader(getClass().getResourceAsStream(NSManager.stripResource(name)));
        } else if (isURL(name)) {
            URL url = new URL(name);
            fr = new InputStreamReader(url.openStream());
        } else {
            fr = new FileReader(name);
        }
        query = read(fr);

        if (query.equals("")) {
            return null;
        }
        return query;
    }

    boolean isURL(String path) {
        try {
            new URL(path);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    public String getResource(String name) throws IOException {
        InputStream stream = ResourceReader.class.getResourceAsStream(name);
        if (stream == null) {
            throw new IOException(name);
        }
        Reader fr = new InputStreamReader(stream);
        return read(fr);
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
            if (isnl) {
                sb.append(System.getProperty("line.separator"));
            } else {
                isnl = true;
            }
            sb.append(str);
        }
        return sb.toString();
    }


}
