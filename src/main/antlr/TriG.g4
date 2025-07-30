// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

grammar TriG;

trigDoc
    : ( directive | block )* EOF
    ;

block
    : triplesOrGraph
    | wrappedGraph
    | triples2
    | Graph_w labelOrSubject wrappedGraph
    ;

triplesOrGraph
    : labelOrSubject (wrappedGraph | predicateObjectList '.')
    ;

triples2
    : blankNodePropertyList predicateObjectList? '.'
    | collection predicateObjectList '.'
    ;

wrappedGraph
    : '{' triplesBlock? '}'
    ;

triplesBlock
    : triples ('.' triplesBlock?)?
    ;

labelOrSubject
    : iri
    | blankNode
    ;

directive
    : prefixID
    | base
    | sparqlPrefix
    | sparqlBase
    ;

prefixID
    : '@prefix' PNAME_NS IRIREF '.'
    ;

base
    : '@base' IRIREF '.'
    ;

sparqlPrefix
    : Prefix_w PNAME_NS IRIREF
    ;

sparqlBase
    : Base_w IRIREF
    ;

triples
    : subject predicateObjectList
    | blankNodePropertyList predicateObjectList?
    ;

predicateObjectList
    : verb objectList (';' (verb objectList)?)*
    ;

objectList
    : object (',' object)*
    ;

verb
    : predicate
    | 'a'
    ;

subject
    : iri
    | blank
    ;

predicate
    : iri
    ;

object
    : iri
    | blank
    | blankNodePropertyList
    | literal
    ;

literal
    : rDFLiteral
    | numericLiteral
    | BooleanLiteral
    ;

blank
    : blankNode
    | collection
    ;

blankNodePropertyList
    : '[' predicateObjectList ']'
    ;

collection
    : '(' object* ')'
    ;

numericLiteral
    : INTEGER
    | DECIMAL
    | DOUBLE
    ;

rDFLiteral
    : string LANGTAG
    | string ('^^' iri)?
    ;

string
    : STRING_LITERAL_QUOTE
    | STRING_LITERAL_SINGLE_QUOTE
    | STRING_LITERAL_LONG_SINGLE_QUOTE
    | STRING_LITERAL_LONG_QUOTE
    ;

iri
    : prefixedName
    | IRIREF
    ;

prefixedName
    : PNAME_LN
    | PNAME_NS
    ;

blankNode
    : BLANK_NODE_LABEL
    | ANON
    ;

WS
    : (('\u0020' | '\u0009' | '\u000A' | '\u000D' ) )+ -> skip
    ;

// Terminals

Graph_w options { caseInsensitive=true; }
    : 'GRAPH'
    ;

Base_w options { caseInsensitive=true; }
    : 'BASE'
    ;

Prefix_w options { caseInsensitive=true; }
    : 'PREFIX'
    ;

BooleanLiteral
    : 'true'
    | 'false'
    ;

IRIREF
    : '<' (PN_CHARS | '.' | ':' | '#' | '@' | '%' | '&' | '$' | '!' | '\'' | '*' | '+' | '/' | '(' | ')' | '-' | ',' | '?' | '~' | UCHAR)* '>'
    ;

PNAME_NS
    : PN_PREFIX? ':'
    ;

PNAME_LN
    : PNAME_NS PN_LOCAL
    ;

BLANK_NODE_LABEL
    : '_:' (PN_CHARS_U | '0' .. '9') ((PN_CHARS | '.')* PN_CHARS)?
    ;

LANGTAG
    : '@' ('a'.. 'z' | 'A' .. 'Z')+ ('-' ('a'.. 'z' | 'A' .. 'Z' | '0' .. '9')* )*
    ;

INTEGER
    : ('+' | '-' )? ('0' .. '9')+
    ;

DECIMAL
    : ('+' | '-' )? ('0' .. '9')* '.' ('0' .. '9')+
    ;

DOUBLE
    : ('+' | '-' )? (('0' .. '9')+ '.' ('0' .. '9')* EXPONENT
    | '.' ('0' .. '9')+ EXPONENT
    | ('0' .. '9')+ EXPONENT)
    ;

EXPONENT
    : ('e' | 'E') ('+' | '-' )? ('0' .. '9')+
    ;

STRING_LITERAL_QUOTE
    : '"' ((~[\u0022\u005C\u0010\u0013]) | ECHAR | UCHAR)* '"'
    ;

STRING_LITERAL_SINGLE_QUOTE
    : '\'' ((~[\u0027\u005C\u0010\u0013]) | ECHAR | UCHAR)* '\''
    ;

STRING_LITERAL_LONG_SINGLE_QUOTE
    : '\'\'\'' (('\'' | '\'\'')? ( (~['\\] ) | ECHAR | UCHAR))* '\'\'\''
    ;

STRING_LITERAL_LONG_QUOTE
    : '"""' (('"' | '""')? ( (~["'] ) | ECHAR | UCHAR))* '"""'
    ;

UCHAR
    : '\\u' HEX HEX HEX HEX
    | '\\U' HEX HEX HEX HEX HEX HEX HEX HEX
    ;

ECHAR options { caseInsensitive=true; }
    : '\\' [tbnrf"'\\]
    ;

WHITESPACE
    : [\u0020\u0009\u000A\u000D]
    ;

ANON
    : '[' WHITESPACE* ']'
    ;

PN_CHARS_BASE
    : 'A' .. 'Z'
    | 'a' .. 'z'
    | '\u00C0' .. '\u00D6'
    | '\u00D8' .. '\u00F6'
    | '\u00F8' .. '\u02FF'
    | '\u0370' .. '\u037D'
    | '\u037F' .. '\u1FFF'
    | '\u200C' .. '\u200D'
    | '\u2070' .. '\u218F'
    | '\u2C00' .. '\u2FEF'
    | '\u3001' .. '\uD7FF'
    | '\uF900' .. '\uFDCF'
    | '\uFDF0' .. '\uFFFD'
//    | '\u10000' .. '\uEFFFF'
    ;

PN_CHARS_U
    : PN_CHARS_BASE
    | '_'
    ;

PN_CHARS
    : PN_CHARS_U
    | '-'
    | [0-9]
    | [\u00B7]
    | [\u0300-\u036F]
    | [\u203F-\u2040]
    ;

PN_PREFIX
    : PN_CHARS_BASE ((PN_CHARS | '.')* PN_CHARS)?
    ;

PN_LOCAL
    :  	(PN_CHARS_U | ':' | [0-9] | PLX) ((PN_CHARS | '.' | ':' | PLX)* (PN_CHARS | ':' | PLX))?
    ;

PLX
    : PERCENT
    | PN_LOCAL_ESC
    ;

PERCENT
    : '%' HEX HEX
    ;

HEX
    : [0-9a-fA-F]
    ;

PN_LOCAL_ESC
    : '\\' (
        '_'
        | '~'
        | '.'
        | '-'
        | '!'
        | '$'
        | '&'
        | '\''
        | '('
        | ')'
        | '*'
        | '+'
        | ','
        | ';'
        | '='
        | '/'
        | '?'
        | '#'
        | '@'
        | '%'
        )
    ;

LC
    : '#' ~[\r\n]+ -> channel(HIDDEN)
    ;