package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;

/** Decodes Unicode escapes in IRI references and character escapes in prefixed names. */
public final class SparqlIriEscapes {

    private SparqlIriEscapes() {
    }

    /** Decodes one layer of escapes from a grammar-validated IRI token. */
    public static String decode(String value) {
        if (value == null || !value.contains("\\")) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length());
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (character == '\\' && index + 1 < value.length()) {
                index = appendEscaped(result, value, index + 1);
            } else {
                result.append(character);
                index++;
            }
        }
        return result.toString();
    }

    private static int appendEscaped(StringBuilder result, String value, int escapeIndex) {
        char escape = value.charAt(escapeIndex);
        if (escape == 'u' || escape == 'U') {
            int length = escape == 'u' ? 4 : 8;
            if (escapeIndex + 1 + length <= value.length()) {
                long codePoint = Long.parseLong(
                        value.substring(escapeIndex + 1, escapeIndex + 1 + length), 16);
                if (codePoint > Character.MAX_CODE_POINT || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
                    throw new QuerySyntaxException(
                            "Code point U+" + Long.toHexString(codePoint).toUpperCase()
                            + " is not a valid Unicode scalar value in a SPARQL escape sequence");
                }
                result.appendCodePoint((int) codePoint);
                return escapeIndex + 1 + length;
            }
        }
        result.append(escape);
        return escapeIndex + 1;
    }
}
