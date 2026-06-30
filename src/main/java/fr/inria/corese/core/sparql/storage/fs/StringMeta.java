package fr.inria.corese.core.sparql.storage.fs;

/**
 * Store meta infor of string stored in file
 *
 * StringMeta.java
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 13 janv. 2015
 */
public class StringMeta {

    private int id; // string id
    private int fid; // file id
    private int offset;
    private int length;

    public StringMeta(int id, int fid, int offset, int length) {
        this.id = id;
        this.fid = fid;
        this.offset = offset;
        this.length = length;
    }


    public int getFid() {
        return fid;
    }


    public long getOffset() {
        return offset;
    }


    public int getLength() {
        return length;
    }


    @Override
    public String toString() {
        return "String " + "[" + id + ", " + fid + ", " + offset + ", " + length + ']';
    }
}
