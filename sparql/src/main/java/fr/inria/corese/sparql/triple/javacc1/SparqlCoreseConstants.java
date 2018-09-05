/* Generated By:JavaCC: Do not edit this line. SparqlCoreseConstants.java */
package fr.inria.corese.sparql.triple.javacc1;

public interface SparqlCoreseConstants {

  int EOF = 0;
  int WS = 6;
  int SINGLE_LINE_COMMENT = 7;
  int Q_IRIref = 8;
  int QNAME_NS = 9;
  int QNAME = 10;
  int BLANK_NODE_LABEL = 11;
  int VAR1 = 12;
  int VAR2 = 13;
  int ABASE = 14;
  int APREFIX = 15;
  int ATLIST = 16;
  int ATPATH = 17;
  int LANGTAG = 18;
  int A2Z = 19;
  int A2ZN = 20;
  int KW_A = 21;
  int INV = 22;
  int SHORT = 23;
  int SHORTALL = 24;
  int DEPTH = 25;
  int BREADTH = 26;
  int LIST = 27;
  int BASE = 28;
  int PREFIX = 29;
  int SELECT = 30;
  int DESCRIBE = 31;
  int CONSTRUCT = 32;
  int RULE = 33;
  int ASK = 34;
  int TEMPLATE = 35;
  int DISTINCT = 36;
  int SEPARATOR = 37;
  int REDUCED = 38;
  int LIMIT = 39;
  int OFFSET = 40;
  int ORDER = 41;
  int BY = 42;
  int RELAX = 43;
  int ASC = 44;
  int DESC = 45;
  int NAMED = 46;
  int FROM = 47;
  int WHERE = 48;
  int AND = 49;
  int GRAPH = 50;
  int SERVICE = 51;
  int REC = 52;
  int STATE = 53;
  int LEAF = 54;
  int OPTIONAL = 55;
  int UNION = 56;
  int MINUSP = 57;
  int NOT = 58;
  int SCOPE = 59;
  int LET = 60;
  int SET = 61;
  int FOR = 62;
  int LOOP = 63;
  int IF = 64;
  int THEN = 65;
  int ELSE = 66;
  int EXIST = 67;
  int FILTER = 68;
  int LOAD = 69;
  int CLEAR = 70;
  int DROP = 71;
  int CREATE = 72;
  int ADD = 73;
  int TO = 74;
  int MOVE = 75;
  int COPY = 76;
  int INSERT = 77;
  int DELETE = 78;
  int WITH = 79;
  int USING = 80;
  int DEFAUT = 81;
  int ALL = 82;
  int INTO = 83;
  int SILENT = 84;
  int DATA = 85;
  int BOUND = 86;
  int STR = 87;
  int DTYPE = 88;
  int LANG = 89;
  int LANGMATCHES = 90;
  int IS_URI = 91;
  int IS_IRI = 92;
  int IS_BLANK = 93;
  int IS_LITERAL = 94;
  int REGEX = 95;
  int TRUE = 96;
  int FALSE = 97;
  int ONE = 98;
  int S_MORE = 99;
  int MERGE = 100;
  int DISPLAY = 101;
  int PRAGMA = 102;
  int D_RDF = 103;
  int D_JSON = 104;
  int D_FLAT = 105;
  int D_ASQUERY = 106;
  int D_XML = 107;
  int D_BLANK = 108;
  int THRESHOLD = 109;
  int RESULT = 110;
  int PROJECTION = 111;
  int GROUP = 112;
  int FORMAT = 113;
  int BOX = 114;
  int IBOX = 115;
  int SBOX = 116;
  int HAVING = 117;
  int VALUES = 118;
  int BIND = 119;
  int UNDEF = 120;
  int COUNT = 121;
  int SOURCE = 122;
  int SCORE = 123;
  int AS = 124;
  int SORT = 125;
  int REVERSE = 126;
  int OR = 127;
  int OPTION = 128;
  int SORTED = 129;
  int ALL2 = 130;
  int CURRENT = 131;
  int ONE2 = 132;
  int DIRECT2 = 133;
  int COLON2 = 134;
  int DISTANCE = 135;
  int DEBUG = 136;
  int CHECK = 137;
  int NOSORT = 138;
  int TUPLE = 139;
  int TRIPLE = 140;
  int XPATH = 141;
  int FUNCTION = 142;
  int LAMBDA = 143;
  int QUERY = 144;
  int PACKAGE = 145;
  int IN = 146;
  int EQ2 = 147;
  int NE2 = 148;
  int BEGIN_WITH = 149;
  int STRICT_SPEC = 150;
  int SPEC = 151;
  int SAME = 152;
  int GENERALISATION = 153;
  int STRICT_GENERALISATION = 154;
  int EQ_LANG = 155;
  int SIMPLE_STRING = 156;
  int INTEGER = 157;
  int DECIMAL = 158;
  int DOUBLE = 159;
  int EXPONENT = 160;
  int QUOTE_3D = 161;
  int QUOTE_3S = 162;
  int ECHAR = 163;
  int STRING_LITERAL1 = 164;
  int STRING_LITERAL2 = 165;
  int STRING_LITERAL_LONG1 = 166;
  int STRING_LITERAL_LONG2 = 167;
  int DIGITS = 168;
  int HEX = 169;
  int LPAREN = 170;
  int RPAREN = 171;
  int LBRACE = 172;
  int RBRACE = 173;
  int LBRACKET = 174;
  int RBRACKET = 175;
  int ANON = 176;
  int SEMICOLON = 177;
  int COMMA = 178;
  int DOT = 179;
  int EQ = 180;
  int NE = 181;
  int GT = 182;
  int LT = 183;
  int LE = 184;
  int GE = 185;
  int BANG = 186;
  int TILDE = 187;
  int COLON = 188;
  int SC_OR = 189;
  int SC_AND = 190;
  int PLUS = 191;
  int MINUS = 192;
  int STAR = 193;
  int SLASH = 194;
  int QM = 195;
  int BAR = 196;
  int DATATYPE = 197;
  int AT = 198;
  int NCCHAR1p = 199;
  int NCCHAR1 = 200;
  int NCCHAR = 201;
  int NCNAME_PREFIX = 202;
  int NCNAME = 203;
  int BLANKNAME = 204;
  int VARNAME = 205;
  int LOCAL_ESC = 206;
  int PERCENT = 207;

  int DEFAULT = 0;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "<WS>",
    "<SINGLE_LINE_COMMENT>",
    "<Q_IRIref>",
    "<QNAME_NS>",
    "<QNAME>",
    "<BLANK_NODE_LABEL>",
    "<VAR1>",
    "<VAR2>",
    "\"@base\"",
    "\"@prefix\"",
    "\"@list\"",
    "\"@path\"",
    "<LANGTAG>",
    "<A2Z>",
    "<A2ZN>",
    "\"a\"",
    "\"i\"",
    "\"s\"",
    "<SHORTALL>",
    "\"d\"",
    "\"b\"",
    "\"list\"",
    "\"base\"",
    "\"prefix\"",
    "\"select\"",
    "\"describe\"",
    "\"construct\"",
    "\"rule\"",
    "\"ask\"",
    "\"template\"",
    "\"distinct\"",
    "\"separator\"",
    "\"reduced\"",
    "\"limit\"",
    "\"offset\"",
    "\"order\"",
    "\"by\"",
    "\"relax\"",
    "\"asc\"",
    "\"desc\"",
    "\"named\"",
    "\"from\"",
    "\"where\"",
    "\"and\"",
    "\"graph\"",
    "\"service\"",
    "\"rec\"",
    "\"state\"",
    "\"leaf\"",
    "\"optional\"",
    "\"union\"",
    "\"minus\"",
    "\"not\"",
    "\"scope\"",
    "\"let\"",
    "\"set\"",
    "\"for\"",
    "\"loop\"",
    "\"if\"",
    "\"then\"",
    "\"else\"",
    "\"exists\"",
    "\"filter\"",
    "\"load\"",
    "\"clear\"",
    "\"drop\"",
    "\"create\"",
    "\"add\"",
    "\"to\"",
    "\"move\"",
    "\"copy\"",
    "\"insert\"",
    "\"delete\"",
    "\"with\"",
    "\"using\"",
    "\"default\"",
    "\"all\"",
    "\"into\"",
    "\"silent\"",
    "\"data\"",
    "\"bound\"",
    "\"str\"",
    "\"datatype\"",
    "\"lang\"",
    "\"langmatches\"",
    "\"isURI\"",
    "\"isIRI\"",
    "\"isBlank\"",
    "\"isLiteral\"",
    "\"regex\"",
    "\"true\"",
    "\"false\"",
    "\"one\"",
    "\"more\"",
    "\"merge\"",
    "\"display\"",
    "\"pragma\"",
    "\"rdf\"",
    "\"json\"",
    "\"flat\"",
    "\"asquery\"",
    "\"xml\"",
    "\"blank\"",
    "\"threshold\"",
    "\"result\"",
    "\"projection\"",
    "\"group\"",
    "\"format\"",
    "\"box\"",
    "\"ibox\"",
    "\"sbox\"",
    "\"having\"",
    "\"values\"",
    "\"bind\"",
    "\"undef\"",
    "\"count\"",
    "\"source\"",
    "\"score\"",
    "\"as\"",
    "\"sort\"",
    "\"reverse\"",
    "\"or\"",
    "\"option\"",
    "\"sorted\"",
    "\"all::\"",
    "\"current::\"",
    "\"one::\"",
    "\"direct::\"",
    "\"::\"",
    "\"distance\"",
    "\"debug\"",
    "\"check\"",
    "\"nosort\"",
    "\"tuple\"",
    "\"triple\"",
    "\"xpath\"",
    "\"function\"",
    "\"lambda\"",
    "\"query\"",
    "\"package\"",
    "\"in\"",
    "\"==\"",
    "\"!==\"",
    "\"^\"",
    "\"<:\"",
    "\"<=:\"",
    "\"=:\"",
    "\">=:\"",
    "\">:\"",
    "\"~=\"",
    "<SIMPLE_STRING>",
    "<INTEGER>",
    "<DECIMAL>",
    "<DOUBLE>",
    "<EXPONENT>",
    "\"\\\"\\\"\\\"\"",
    "\"\\\'\\\'\\\'\"",
    "<ECHAR>",
    "<STRING_LITERAL1>",
    "<STRING_LITERAL2>",
    "<STRING_LITERAL_LONG1>",
    "<STRING_LITERAL_LONG2>",
    "<DIGITS>",
    "<HEX>",
    "\"(\"",
    "\")\"",
    "\"{\"",
    "\"}\"",
    "\"[\"",
    "\"]\"",
    "<ANON>",
    "\";\"",
    "\",\"",
    "\".\"",
    "\"=\"",
    "\"!=\"",
    "\">\"",
    "\"<\"",
    "\"<=\"",
    "\">=\"",
    "\"!\"",
    "\"~\"",
    "\":\"",
    "\"||\"",
    "\"&&\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"?\"",
    "\"|\"",
    "\"^^\"",
    "\"@\"",
    "<NCCHAR1p>",
    "<NCCHAR1>",
    "<NCCHAR>",
    "<NCNAME_PREFIX>",
    "<NCNAME>",
    "<BLANKNAME>",
    "<VARNAME>",
    "<LOCAL_ESC>",
    "<PERCENT>",
  };

}
