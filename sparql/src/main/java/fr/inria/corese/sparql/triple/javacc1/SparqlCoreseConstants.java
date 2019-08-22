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
  int BASE = 22;
  int PREFIX = 23;
  int SELECT = 24;
  int DESCRIBE = 25;
  int CONSTRUCT = 26;
  int RULE = 27;
  int ASK = 28;
  int TEMPLATE = 29;
  int DISTINCT = 30;
  int SEPARATOR = 31;
  int REDUCED = 32;
  int LIMIT = 33;
  int OFFSET = 34;
  int ORDER = 35;
  int BY = 36;
  int RELAX = 37;
  int ASC = 38;
  int DESC = 39;
  int NAMED = 40;
  int FROM = 41;
  int WHERE = 42;
  int GRAPH = 43;
  int SERVICE = 44;
  int OPTIONAL = 45;
  int UNION = 46;
  int MINUSP = 47;
  int NOT = 48;
  int SCOPE = 49;
  int LET = 50;
  int LETDYN = 51;
  int SET = 52;
  int FOR = 53;
  int LOOP = 54;
  int IF = 55;
  int THEN = 56;
  int ELSE = 57;
  int EXIST = 58;
  int FILTER = 59;
  int LOAD = 60;
  int CLEAR = 61;
  int DROP = 62;
  int CREATE = 63;
  int ADD = 64;
  int TO = 65;
  int MOVE = 66;
  int COPY = 67;
  int INSERT = 68;
  int DELETE = 69;
  int WITH = 70;
  int USING = 71;
  int DEFAUT = 72;
  int ALL = 73;
  int INTO = 74;
  int SILENT = 75;
  int DATA = 76;
  int ERROR = 77;
  int RETURN = 78;
  int AGGREGATE = 79;
  int UNNEST = 80;
  int MAP = 81;
  int MAPLIST = 82;
  int MAPFIND = 83;
  int MAPFINDLIST = 84;
  int MAPMERGE = 85;
  int MAPEVERY = 86;
  int MAPANY = 87;
  int FUNCALL = 88;
  int EVAL = 89;
  int METHOD = 90;
  int APPLY = 91;
  int REDUCE = 92;
  int SELF = 93;
  int BOUND = 94;
  int COALESCE = 95;
  int SAMETERM = 96;
  int STR = 97;
  int STRDT = 98;
  int STRLANG = 99;
  int BNODE = 100;
  int URI = 101;
  int IRI = 102;
  int UUID = 103;
  int STRUUID = 104;
  int DTYPE = 105;
  int LANG = 106;
  int LANGMATCHES = 107;
  int CONTAINS = 108;
  int STRSTARTS = 109;
  int STRENDS = 110;
  int STRLEN = 111;
  int SUBSTR = 112;
  int UCASE = 113;
  int LCASE = 114;
  int STRBEFORE = 115;
  int STRAFTER = 116;
  int ENCODE_FOR_URI = 117;
  int CONCAT = 118;
  int REPLACE = 119;
  int IS_URI = 120;
  int IS_IRI = 121;
  int IS_BLANK = 122;
  int IS_LITERAL = 123;
  int IS_NUMERIC = 124;
  int IS_EXTENSION = 125;
  int IS_UNDEFINED = 126;
  int IS_WELLFORMED = 127;
  int REGEX = 128;
  int RAND = 129;
  int ROUND = 130;
  int FLOOR = 131;
  int CEIL = 132;
  int POWER = 133;
  int ABS = 134;
  int NOW = 135;
  int YEAR = 136;
  int MONTH = 137;
  int DAY = 138;
  int HOURS = 139;
  int MINUTES = 140;
  int SECONDS = 141;
  int TIMEZONE = 142;
  int TZ = 143;
  int MD5 = 144;
  int SHA1 = 145;
  int SHA256 = 146;
  int SHA384 = 147;
  int SHA512 = 148;
  int GROUP_CONCAT = 149;
  int SUM = 150;
  int SAMPLE = 151;
  int AVG = 152;
  int MIN = 153;
  int MAX = 154;
  int TRUE = 155;
  int FALSE = 156;
  int S_MORE = 157;
  int PRAGMA = 158;
  int GROUP = 159;
  int FORMAT = 160;
  int BOX = 161;
  int IBOX = 162;
  int SBOX = 163;
  int HAVING = 164;
  int VALUES = 165;
  int BIND = 166;
  int UNDEF = 167;
  int COUNT = 168;
  int SCORE = 169;
  int AS = 170;
  int SORTED = 171;
  int ALL2 = 172;
  int CURRENT = 173;
  int DIRECT2 = 174;
  int COLON2 = 175;
  int DEBUG = 176;
  int CHECK = 177;
  int NOSORT = 178;
  int TUPLE = 179;
  int TRIPLE = 180;
  int XPATH = 181;
  int FUNCTION = 182;
  int LAMBDA = 183;
  int QUERY = 184;
  int PACKAGE = 185;
  int IN = 186;
  int EQ2 = 187;
  int NE2 = 188;
  int BEGIN_WITH = 189;
  int STRICT_SPEC = 190;
  int SPEC = 191;
  int SAME = 192;
  int GENERALISATION = 193;
  int STRICT_GENERALISATION = 194;
  int EQ_LANG = 195;
  int INTEGER = 196;
  int DECIMAL = 197;
  int DOUBLE = 198;
  int EXPONENT = 199;
  int QUOTE_3D = 200;
  int QUOTE_3S = 201;
  int ECHAR = 202;
  int STRING_LITERAL1 = 203;
  int STRING_LITERAL2 = 204;
  int STRING_LITERAL_LONG1 = 205;
  int STRING_LITERAL_LONG2 = 206;
  int DIGITS = 207;
  int HEX = 208;
  int LPAREN = 209;
  int RPAREN = 210;
  int LBRACE = 211;
  int RBRACE = 212;
  int LBRACKET = 213;
  int RBRACKET = 214;
  int ANON = 215;
  int SEMICOLON = 216;
  int COMMA = 217;
  int DOT = 218;
  int EQ = 219;
  int NE = 220;
  int GT = 221;
  int LT = 222;
  int LE = 223;
  int GE = 224;
  int BANG = 225;
  int TILDE = 226;
  int COLON = 227;
  int SC_OR = 228;
  int SC_AND = 229;
  int PLUS = 230;
  int MINUS = 231;
  int STAR = 232;
  int SLASH = 233;
  int QM = 234;
  int BAR = 235;
  int DATATYPE = 236;
  int AT = 237;
  int VAR3 = 238;
  int NCCHAR1p = 239;
  int NCCHAR1 = 240;
  int NCCHAR = 241;
  int NCNAME_PREFIX = 242;
  int NCNAME = 243;
  int BLANKNAME = 244;
  int VARNAME = 245;
  int LOCAL_ESC = 246;
  int PERCENT = 247;

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
    "\"graph\"",
    "\"service\"",
    "\"optional\"",
    "\"union\"",
    "\"minus\"",
    "\"not\"",
    "\"scope\"",
    "\"let\"",
    "\"letdyn\"",
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
    "\"error\"",
    "\"return\"",
    "\"aggregate\"",
    "\"unnest\"",
    "\"map\"",
    "\"maplist\"",
    "\"mapfind\"",
    "\"mapfindlist\"",
    "\"mapmerge\"",
    "\"mapevery\"",
    "\"mapany\"",
    "\"funcall\"",
    "\"eval\"",
    "\"method\"",
    "\"apply\"",
    "\"reduce\"",
    "\"self\"",
    "\"bound\"",
    "\"coalesce\"",
    "\"sameTerm\"",
    "\"str\"",
    "\"strdt\"",
    "\"strlang\"",
    "\"bnode\"",
    "\"uri\"",
    "\"iri\"",
    "\"uuid\"",
    "\"struuid\"",
    "\"datatype\"",
    "\"lang\"",
    "\"langmatches\"",
    "\"contains\"",
    "\"strstarts\"",
    "\"strends\"",
    "\"strlen\"",
    "\"substr\"",
    "\"ucase\"",
    "\"lcase\"",
    "\"strbefore\"",
    "\"strafter\"",
    "\"encode_for_uri\"",
    "\"concat\"",
    "\"replace\"",
    "\"isURI\"",
    "\"isIRI\"",
    "\"isBlank\"",
    "\"isLiteral\"",
    "\"isNumeric\"",
    "\"isExtension\"",
    "\"isUndefined\"",
    "\"isWellFormed\"",
    "\"regex\"",
    "\"rand\"",
    "\"round\"",
    "\"floor\"",
    "\"ceil\"",
    "\"power\"",
    "\"abs\"",
    "\"now\"",
    "\"year\"",
    "\"month\"",
    "\"day\"",
    "\"hours\"",
    "\"minutes\"",
    "\"seconds\"",
    "\"timezone\"",
    "\"tz\"",
    "\"md5\"",
    "\"sha1\"",
    "\"sha256\"",
    "\"sha384\"",
    "\"sha512\"",
    "\"group_concat\"",
    "\"sum\"",
    "\"sample\"",
    "\"avg\"",
    "\"min\"",
    "\"max\"",
    "\"true\"",
    "\"false\"",
    "\"more\"",
    "\"pragma\"",
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
    "\"score\"",
    "\"as\"",
    "\"sorted\"",
    "\"all::\"",
    "\"current::\"",
    "\"direct::\"",
    "\"::\"",
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
    "<VAR3>",
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
