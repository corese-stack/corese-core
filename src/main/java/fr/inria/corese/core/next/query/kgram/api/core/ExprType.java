package fr.inria.corese.core.next.query.kgram.api.core;

public interface ExprType {

    int UNDEF = -1;
    int UNBOUND = -2;
    // LDScript variable
    int LOCAL = -3;
    // SPARQL BGP variable
    int GLOBAL = -4;

    // abstract type
    int JOKER = 1;
    int ALTER = 2;

    // type
    int CONSTANT = 6;
    int VARIABLE = 7;
    int BOOLEAN = 8;
    int TERM = 9;
    int FUNCTION = 10;


    // boolean
    int AND = 11;
    int OR = 12;
    int NOT = 13;

    // function
    int BOUND = 14;
    int COUNT = 15;

    int SAMETERM = 17;
    int MIN = 20;
    int MAX = 21;
    int AVG = 22;
    int SUM = 23;
    int LANG = 27;
    int REGEX = 29;

    int DATATYPE = 30;
    int SELF = 32;
    int DEBUG = 33;
    int EXTERN = 35;
    int KGRAM = 36;
    int SQL = 37;
    int XPATH = 38;
    int SKIP = 39;

    int UNNEST = 41;
    int EXIST = 42;
    int STRDT = 43;
    int BNODE = 45;
    int COALESCE = 46;
    int IF = 47;

    int STRLEN = 51;
    int CONTAINS = 57;
    int CONCAT = 59;

    int YEAR = 60;
    int MONTH = 61;
    int DAY = 62;
    int HOURS = 63;
    int MINUTES = 64;
    int SECONDS = 65;


    int URI = 74;
    int TZ = 75;


    int TRIPLE = 88;
    int SUBJECT = 89;
    int PREDICATE = 90;
    int OBJECT = 91;


    int EQNE = 109;

    int EQ = 110;
    int NE = 111;
    int NEQ = 111;

    int GL = 112;
    int LE = 113;
    int GE = 114;
    int LT = 115;
    int GT = 116;

    int PLUS = 117;
    int MINUS = 118;
    int MULT = 119;

    int START = 122; // ^
    int IN = 123;
    int STAR = 125;


    int DISPLAY = 200;

    int NODE = 209;

    int LOAD = 212;

    int INDEX = 221;
    int ID = 223;
    int TEST = 224;


    int TURTLE = 227;

    int QNAME = 238;


    int AGGREGATE = 276;
    int STL_FORMAT = 277;

    int FORMAT = 283;

    int QUERY = 302;
    int EXTENSION = 303;

    int LIST = 309;
    int MAP = 310;
    int MAPLIST = 311;
    int APPLY = 312;
    int LET = 313;
    int ERROR = 315;
    int MAPEVERY = 316;
    int MAPANY = 317;
    int MAPFIND = 318;
    int MAPFINDLIST = 319;
    int MAPMERGE = 320;
    int SET = 322;
    int SEQUENCE = 323;
    int RETURN = 324;
    int EVAL = 325;
    int FUNCALL = 326;
    int FOR = 327;
    int REDUCE = 329;


    int STATIC = 336;
    int THROW = 338;

    int SLICE = 500;


}
