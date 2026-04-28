// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

/*
 * Updated to SPARQL 1.1
 */

lexer grammar SparqlLexer;

options {
    caseInsensitive = true;
}

A options {
    caseInsensitive = false;
}: 'a';

ASC         : 'ASC';
ASK         : 'ASK';
BASE        : 'BASE';
BOUND       : 'BOUND';
BY          : 'BY';
CONSTRUCT   : 'CONSTRUCT';
DATATYPE    : 'DATATYPE';
DESC        : 'DESC';
DESCRIBE    : 'DESCRIBE';
DISTINCT    : 'DISTINCT';
FILTER      : 'FILTER';
FROM        : 'FROM';
GRAPH       : 'GRAPH';
LANG        : 'LANG';
LANGMATCHES : 'LANGMATCHES';
LIMIT       : 'LIMIT';
NAMED       : 'NAMED';
OFFSET      : 'OFFSET';
OPTIONAL    : 'OPTIONAL';
ORDER       : 'ORDER';
PREFIX      : 'PREFIX';
REDUCED     : 'REDUCED';
SELECT      : 'SELECT';
STR         : 'STR';
UNION       : 'UNION';
WHERE       : 'WHERE';
TRUE        : 'true';
FALSE       : 'false';

// SPARQL 1.1 new query keywords

AS          : 'AS';
BIND        : 'BIND';
EXISTS      : 'EXISTS';
GROUP       : 'GROUP';
HAVING      : 'HAVING';
IF          : 'IF';
IN          : 'IN';
MINUS       : 'MINUS';
NOT         : 'NOT';
NOW         : 'NOW';
SERVICE     : 'SERVICE';
SILENT      : 'SILENT';
UNDEF       : 'UNDEF';
VALUES      : 'VALUES';

// SPARQL 1.1 update keywords

ADD     : 'ADD';
ALL     : 'ALL';
CLEAR   : 'CLEAR';
COPY    : 'COPY';
CREATE  : 'CREATE';
DATA    : 'DATA';
DEFAULT : 'DEFAULT';
DELETE  : 'DELETE';
DROP    : 'DROP';
INSERT  : 'INSERT';
INTO    : 'INTO';
LOAD    : 'LOAD';
MOVE    : 'MOVE';
TO      : 'TO';
USING   : 'USING';
WITH    : 'WITH';

//  Built-in function keywords

ABS            : 'ABS';
AVG            : 'AVG';
BNODE          : 'BNODE';
CEIL           : 'CEIL';
COALESCE       : 'COALESCE';
CONCAT         : 'CONCAT';
CONTAINS       : 'CONTAINS';
COUNT          : 'COUNT';
DAY            : 'DAY';
ENCODE_FOR_URI : 'ENCODE_FOR_URI';
FLOOR          : 'FLOOR';
GROUP_CONCAT   : 'GROUP_CONCAT';
HOURS          : 'HOURS';
IRI            : 'IRI';
LCASE          : 'LCASE';
MAX            : 'MAX';
MD5            : 'MD5';
MIN            : 'MIN';
MINUTES        : 'MINUTES';
MONTH          : 'MONTH';
RAND           : 'RAND';
REGEX          : 'REGEX';
REPLACE        : 'REPLACE';
ROUND          : 'ROUND';
SAMPLE         : 'SAMPLE';
SECONDS        : 'SECONDS';
SEPARATOR      : 'SEPARATOR';
SHA1           : 'SHA1';
SHA256         : 'SHA256';
SHA384         : 'SHA384';
SHA512         : 'SHA512';
STRAFTER       : 'STRAFTER';
STRBEFORE      : 'STRBEFORE';
STRDT          : 'STRDT';
STRENDS        : 'STRENDS';
STRLANG        : 'STRLANG';
STRLEN         : 'STRLEN';
STRSTARTS      : 'STRSTARTS';
STRUUID        : 'STRUUID';
SUBSTR         : 'SUBSTR';
SUM            : 'SUM';
TIMEZONE       : 'TIMEZONE';
TZ             : 'TZ';
UCASE          : 'UCASE';
URI            : 'URI';
UUID           : 'UUID';
YEAR           : 'YEAR';

// isX / sameTerm (case-insensitive under global option)

IS_LITERAL : 'isLITERAL';
IS_BLANK   : 'isBLANK';
IS_URI     : 'isURI';
IS_IRI     : 'isIRI';
IS_NUMERIC : 'isNUMERIC';
SAME_TERM  : 'sameTerm';

// OPERATORS

COMMA            : ',';
DOT              : '.';
DOUBLE_AMP       : '&&';
DOUBLE_BAR       : '||';
DOUBLE_CARET     : '^^';
EQUAL            : '=';
EXCLAMATION      : '!';
GREATER          : '>';
GREATER_OR_EQUAL : '>=';
LESS             : '<';
LESS_OR_EQUAL    : '<=';
L_CURLY          : '{';
L_PAREN          : '(';
L_SQUARE         : '[';
MINUS_SIGN       : '-';
NOT_EQUAL        : '!=';
PLUS             : '+';
R_CURLY          : '}';
R_PAREN          : ')';
R_SQUARE         : ']';
SEMICOLON        : ';';
SLASH            : '/';
STAR             : '*';
CARET            : '^';
QUESTION         : '?';
PIPE             : '|';

// MISC

IRI_REF: '<' (~('<' | '>' | '"' | '{' | '}' | '|' | '^' | '\\' | '`' | '\u0000'..'\u0020'))* '>';

PNAME_NS: PN_PREFIX? ':';

PNAME_LN: PNAME_NS PN_LOCAL;

BLANK_NODE_LABEL: '_:' (PN_CHARS_U | DIGIT) ((PN_CHARS | '.')* PN_CHARS)?;

VAR1: '?' VARNAME;

VAR2: '$' VARNAME;

LANGTAG: '@' PN_CHARS_BASE+ ('-' (PN_CHARS_BASE | DIGIT)+)*;

INTEGER: DIGIT+;

DECIMAL: DIGIT+ '.' DIGIT* | '.' DIGIT+;

DOUBLE: DIGIT+ '.' DIGIT* EXPONENT | '.' DIGIT+ EXPONENT | DIGIT+ EXPONENT;

INTEGER_POSITIVE: '+' INTEGER;

DECIMAL_POSITIVE: '+' DECIMAL;

DOUBLE_POSITIVE: '+' DOUBLE;

INTEGER_NEGATIVE: '-' INTEGER;

DECIMAL_NEGATIVE: '-' DECIMAL;

DOUBLE_NEGATIVE: '-' DOUBLE;

fragment EXPONENT: 'E' ('+' | '-')? DIGIT+;

STRING_LITERAL1: '\'' (~('\u0027' | '\u005C' | '\u000A' | '\u000D') | ECHAR | UCHAR)* '\'';

STRING_LITERAL2: '"' (~('\u0022' | '\u005C' | '\u000A' | '\u000D') | ECHAR | UCHAR)* '"';

STRING_LITERAL_LONG1: '\'\'\'' (('\'' | '\'\'')? (~('\'' | '\\') | ECHAR | UCHAR))* '\'\'\'';

STRING_LITERAL_LONG2: '"""' (('"' | '""')? (~('"' | '\\') | ECHAR | UCHAR))* '"""';

fragment ECHAR options {
    caseInsensitive = false;
}: '\\' ('t' | 'b' | 'n' | 'r' | 'f' | '"' | '\'' | '\\');

NIL: '(' WS* ')';

ANON: '[' WS* ']';

fragment PN_CHARS_U: PN_CHARS_BASE | '_';

VARNAME: (PN_CHARS_U | DIGIT) (PN_CHARS_U | DIGIT | '\u00B7' | '\u0300'..'\u036F' | '\u203F'..'\u2040')*;

fragment PN_CHARS
    : PN_CHARS_U
    | '-'
    | DIGIT
    | '\u00B7'
    | '\u0300'..'\u036F'
    | '\u203F'..'\u2040'
    ;

PN_PREFIX: PN_CHARS_BASE ((PN_CHARS | '.')* PN_CHARS)?;

fragment PN_LOCAL: (PN_CHARS_U | ':' | DIGIT | PLX) ((PN_CHARS | '.' | ':' | PLX)* (PN_CHARS | ':' | PLX))?;

fragment PLX: PERCENT | PN_LOCAL_ESC;

fragment PERCENT options {
    caseInsensitive = false;
}: '%' HEX HEX;

fragment HEX options {
    caseInsensitive = false;
}: '0'..'9' | 'A'..'F' | 'a'..'f';

fragment PN_LOCAL_ESC: '\\' ('_' | '~' | '.' | '-' | '!' | '$' | '&' | '\'' | '(' | ')' | '*' | '+' | ',' | ';' | '=' | '/' | '?' | '#' | '@' | '%');

fragment UCHAR options {
    caseInsensitive = false;
}: '\\u' HEX HEX HEX HEX | '\\U' HEX HEX HEX HEX HEX HEX HEX HEX;

fragment PN_CHARS_BASE options {
    caseInsensitive = false;
}:
    'A'..'Z'
    | 'a'..'z'
    | '\u00C0'..'\u00D6'
    | '\u00D8'..'\u00F6'
    | '\u00F8'..'\u02FF'
    | '\u0370'..'\u037D'
    | '\u037F'..'\u1FFF'
    | '\u200C'..'\u200D'
    | '\u2070'..'\u218F'
    | '\u2C00'..'\u2FEF'
    | '\u3001'..'\uD7FF'
    | '\uF900'..'\uFDCF'
    | '\uFDF0'..'\uFFFD'
    ;

fragment DIGIT: '0'..'9';

COMMENT: '#' ~[\r\n]* ('\r'? '\n' | EOF) -> channel(HIDDEN);

WS: [ \t\n\r]+ -> channel(HIDDEN);
