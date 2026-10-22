package fr.inria.corese.core.sparql.datatype;

import java.math.BigDecimal;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * An implementation of the xsd:date datatype used by Corese
 */
public class CoreseDate extends CoreseDatatype {

    private final XMLGregorianCalendar cal;
    private final long epochMillis;
    private final boolean hasZone;
    private final boolean hasFastEpoch;
    private static final java.util.TimeZone UTC_ZONE = java.util.TimeZone.getTimeZone("UTC");
    private static final String TODAY = "today";
    private static final IDatatype.Datatype code = IDatatype.Datatype.DATE;
    private static final CoreseURI datatype = new CoreseURI(RDF.xsddate);
    String label;
       
    public CoreseDate() {
        this.cal = DatatypeMap.newXMLGregorianCalendar();
        setLabel(cal.toString());
        this.hasZone = (this.cal.getTimezone() != DatatypeConstants.FIELD_UNDEFINED);
        long em = 0;
        boolean fast = false;
        try {
            em = computeEpoch(this.cal, this.hasZone);
            fast = true;
        } catch (Exception e) {
            fast = false;
        }
        this.epochMillis = em;
        this.hasFastEpoch = fast;
    }

    public CoreseDate(XMLGregorianCalendar cal) {
        this.cal = cal;
        setLabel(cal.toString());
        this.hasZone = (this.cal.getTimezone() != DatatypeConstants.FIELD_UNDEFINED);
        long em = 0;
        boolean fast = false;
        try {
            em = computeEpoch(this.cal, this.hasZone);
            fast = true;
        } catch (Exception e) {
            fast = false;
        }
        this.epochMillis = em;
        this.hasFastEpoch = fast;
    }
      
    public CoreseDate(String label) {
        if (label.equals(TODAY)) {
            this.cal = DatatypeMap.newXMLGregorianCalendar();
            setLabel(cal.toString());
        } else {
            this.cal = DatatypeMap.newXMLGregorianCalendar(label);
            setLabel(label);
        }
        this.hasZone = (this.cal.getTimezone() != DatatypeConstants.FIELD_UNDEFINED);
        long em = 0;
        boolean fast = false;
        try {
            em = computeEpoch(this.cal, this.hasZone);
            fast = true;
        } catch (Exception e) {
            fast = false;
        }
        this.epochMillis = em;
        this.hasFastEpoch = fast;
    }

    private static long computeEpoch(XMLGregorianCalendar calendar, boolean zone) {
        if (zone) {
            return calendar.normalize().toGregorianCalendar().getTimeInMillis();
        }
        return calendar.toGregorianCalendar(UTC_ZONE, java.util.Locale.ROOT, null).getTimeInMillis();
    }


    @Override
    public IDatatype.Datatype getCode() {
        return code;
    }

    @Override
    public boolean isDate() {
        return true;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public IDatatype getDatatype() {
        return datatype;
    }

    @Override
    public String getLabel() {
        return label;
    }
    
    void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getNormalizedLabel() {
        return this.getLabel();
    }

    @Override
    public String getLowerCaseLabel() {
        return this.getLabel();
    }

    public IDatatype getYear() {
        return DatatypeMap.newInstance(this.cal.getYear(), RDF.xsdinteger);
    }

    public IDatatype getMonth() {
        return DatatypeMap.newInstance(this.cal.getMonth(), RDF.xsdinteger);
    }

    public IDatatype getDay() {
        return DatatypeMap.newInstance(this.cal.getDay(), RDF.xsdinteger);
    }

    public IDatatype getHour() {
        return DatatypeMap.newInstance(this.cal.getHour(), RDF.xsdinteger);
    }

    public IDatatype getMinute() {
        return DatatypeMap.newInstance(this.cal.getMinute(), RDF.xsdinteger);
    }

    public CoreseDecimal getSecond() {
        if (this.cal.getFractionalSecond() == null) {
            return new CoreseDecimal(this.cal.getSecond());
        }
        return new CoreseDecimal(BigDecimal.valueOf(this.cal.getSecond()).add(this.cal.getFractionalSecond()));
    }

    public IDatatype getTZ() {
        if (cal.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            return DatatypeMap.newLiteral("");
        } else if (cal.getTimezone() == 0) {
            return DatatypeMap.newLiteral("Z");
        } else {
            int tz = cal.getTimezone() / 60;
            String result;
            if (tz > 0) {
                result = String.format("+%02d:00", tz);
            } else {
                result = String.format("%03d:00", tz);
            }
            return DatatypeMap.newLiteral(result);
        }
    }

    public IDatatype getTimezone() {
        if (cal.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            return null;
        } else if (cal.getTimezone() == 0) {
            return DatatypeMap.newInstance("PT0S", RDF.xsddaytimeduration);
        } else {
            int tz = cal.getTimezone() / 60;
            String result;
            if (tz > 0) {
                result = String.format("+PT%dH", tz);
            } else {
                result = String.format("-PT%dH", Math.abs(tz));
            }
            return DatatypeMap.newInstance(result, RDF.xsddaytimeduration);
        }

    }

    public XMLGregorianCalendar getCalendar() {
        return cal;
    }

    int compare(XMLGregorianCalendar cal1, XMLGregorianCalendar cal2) throws CoreseDatatypeException {
        int res = cal1.compare(cal2);
        if (res == DatatypeConstants.INDETERMINATE) {
            throw failure();
        }

        return res;
    }

    // date vs dateTime
    void check(IDatatype icod) throws CoreseDatatypeException {
        if (DatatypeMap.SPARQLCompliant && this.getClass() != icod.getClass()) {
            throw failure();
        }
    }

    public int compareTo(CoreseDate dt) throws CoreseDatatypeException {
        if (this.hasFastEpoch && dt.hasFastEpoch && this.hasZone == dt.hasZone) {
            if (this.epochMillis < dt.epochMillis) {
                return -1;
            } else if (this.epochMillis > dt.epochMillis) {
                return 1;
            }
        }
        return this.compare(this.getCalendar(), dt.getCalendar());
    }

    @Override
    public IDatatype cast(String target) {
        if (RDF.xsddate.equals(target)) {
            return this;
        }
        return super.cast(target);
    }

    @Override
    public int compare(IDatatype iod) throws CoreseDatatypeException {
        if (iod.getCode() == IDatatype.Datatype.DATE || iod.getCode() == IDatatype.Datatype.DATETIME) {
            return this.compareTo((CoreseDate) iod);
        }
        throw failure();
    }

    @Override
    public boolean less(IDatatype iod) throws CoreseDatatypeException {
        if (iod.getCode() == IDatatype.Datatype.DATE || iod.getCode() == IDatatype.Datatype.DATETIME) {
            this.check(iod);
            return this.compareTo((CoreseDate) iod) < 0;
        }
        throw failure();
    }

    @Override
    public boolean lessOrEqual(IDatatype iod) throws CoreseDatatypeException {
        if (iod.getCode() == IDatatype.Datatype.DATE || iod.getCode() == IDatatype.Datatype.DATETIME) {
            this.check(iod);
            return this.compareTo((CoreseDate) iod) <= 0;
        }
        throw failure();
    }

    @Override
    public boolean greater(IDatatype iod) throws CoreseDatatypeException {
        if (iod.getCode() == IDatatype.Datatype.DATE || iod.getCode() == IDatatype.Datatype.DATETIME) {
            this.check(iod);
            return this.compareTo((CoreseDate) iod) > 0;
        }
        throw failure();
    }

    @Override
    public boolean greaterOrEqual(IDatatype iod) throws CoreseDatatypeException {
        if (iod.getCode() == IDatatype.Datatype.DATE || iod.getCode() == IDatatype.Datatype.DATETIME) {
            this.check(iod);
            return this.compareTo((CoreseDate) iod) >= 0;
        }
        throw failure();
    }

    @Override
    public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException {
        if (iod.getCode() == IDatatype.Datatype.DATE || iod.getCode() == IDatatype.Datatype.DATETIME) {
            return this.compareTo((CoreseDate) iod) == 0;
        }
        if (iod.isURI() || iod.isBlank() || iod.isTriple()) {
            return false;
        }
        throw failure();
    }

}
