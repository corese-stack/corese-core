package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.triple.parser.NSManager;

/**
 *
 * @author corby
 */
public interface XSD {

    String XSD = NSManager.XSD;
    String XSI = NSManager.XSI;
    String xsdboolean = XSD + "boolean";
    String xsdinteger = XSD + "integer";
    String xsdlong = XSD + "long";
    String xsdint = XSD + "int";
    String xsdshort = XSD + "short";
    String xsdbyte = XSD + "byte";
    String xsddecimal = XSD + "decimal";
    String xsdfloat = XSD + "float";
    String xsddouble = XSD + "double";
    String xsdnonNegativeInteger = XSD + "nonNegativeInteger";
    String xsdnonPositiveInteger = XSD + "nonPositiveInteger";
    String xsdpositiveInteger = XSD + "positiveInteger";
    String xsdnegativeInteger = XSD + "negativeInteger";
    String xsdunsignedLong = XSD + "unsignedLong";
    String xsdunsignedInt = XSD + "unsignedInt";
    String xsdunsignedShort = XSD + "unsignedShort";
    String xsdunsignedByte = XSD + "unsignedByte";
    String xsdduration = XSD + "duration";
    String xsddaytimeduration = XSD + "dayTimeDuration";
    String xsddate = XSD + "date";
    String xsddateTime = XSD + "dateTime";
    String xsdday = XSD + "gDay";
    String xsdmonth = XSD + "gMonth";
    String xsdyear = XSD + "gYear";
}
