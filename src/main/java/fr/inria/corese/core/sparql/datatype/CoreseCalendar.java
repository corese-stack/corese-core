package fr.inria.corese.core.sparql.datatype;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br> 
 * This class is used to create and manage a CoreseDate (xsd:DateTime)
 * <br>
 * @author Olivier Corby & Olivier Savoie
 */

public class CoreseCalendar extends GregorianCalendar {

    /**
     * Use to keep the class version, to be consistent with the interface
     * Serializable.java
     */
    private static final long serialVersionUID = 1L;

    boolean Z = false;
    private boolean bzone = false;
    String zone = "";

    CoreseCalendar() {
    }


    CoreseCalendar(int yy, int mm, int dd, int hh, int min, int ss) {
        super(yy, mm, dd, hh, min, ss);
    }

    public CoreseCalendar duplicate() {
        return new CoreseCalendar(get(YEAR), get(MONTH), get(DAY_OF_MONTH), get(HOUR_OF_DAY), get(MINUTE), get(SECOND));
        // return (CoreseCalendar) clone();
    }


    public int getRawOffset() {
        return getTimeZone().getRawOffset();
    }


    void setZ(boolean z) {
        this.Z = z;
    }


    void setDZone(String z) {
        this.zone = z;
    }


}
