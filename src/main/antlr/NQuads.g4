grammar NQuads;

nquadsDoc
    : statement? (EOL* statement)* EOL*
    ;

statement
    : subject predicate object graphLabel? '.'
    ;

subject
    : IRIREF
    | BLANK_NODE_LABEL
    ;

predicate
    : IRIREF
    ;

object
    : IRIREF
    | BLANK_NODE_LABEL
    | literal
    ;

graphLabel
    : IRIREF
    | BLANK_NODE_LABEL
    ;

literal
    : STRING_LITERAL_QUOTE ('^^' IRIREF | LANGTAG)?
    ;

LANGTAG
    : '@' [a-zA-Z]+ ('-' [a-zA-Z0-9]+)*
    ;

EOL
    : [\u000D\u000A]+
    ;

IRIREF
//  	'<' ([^#x00-#x20<>"{}|^`\] | UCHAR)* '>'
    : '<' [a-zA-Z0-9-]+':' ((~( [\u0000-\u0020] | '<' | '>' | '"' | '{'| '}' | '|'| '^'| '`' | '\\' )) | UCHAR)* '>'
    ;

STRING_LITERAL_QUOTE
    : '"' ( ~( [\u0022] | [\u005C] | [\u000A] | [\u000D] ) | ECHAR | UCHAR )* '"'
    ;

BLANK_NODE_LABEL
//  	'_:' (PN_CHARS_U | [0-9]) ((PN_CHARS | '.')* PN_CHARS)?
    : '_:' (PN_CHARS_U | [0-9]) ((PN_CHARS | '.')* PN_CHARS)?
    ;

UCHAR
    : '\\u' HEX HEX HEX HEX
    | '\\U' HEX HEX HEX HEX HEX HEX HEX HEX
    ;

HEX
    : [0-9]
    | [A-F]
    | [a-f]
    ;

ECHAR
    : '\\' [tbnrf"'\\]
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
// PN_CHARS_BASE | '_' | ':'
    : PN_CHARS_BASE
    | '_'
//    | ':'
    ;

PN_CHARS
    : PN_CHARS_U
    | '-'
    | [0-9]
    | [\u00B7]
    | [\u0300-\u036F]
    | [\u203F-\u2040]
    ;

LC
    : '#' ~[\r\n]* -> channel(HIDDEN)
    ;

WS
    : ([\t\r\n\u000C] | ' ')+ -> skip
    ;