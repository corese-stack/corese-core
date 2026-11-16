package fr.inria.corese.core.next.data.impl.io.serializer.support;

/** Turtle prefix-name validation, independent of XML namespace-name rules. */
final class TurtleNames {

    private TurtleNames() {
    }

    /** Accepts the empty default prefix and the Turtle PN_PREFIX production. */
    static boolean isPrefix(String prefix) {
        if (prefix.isEmpty()) {
            return true;
        }
        int first = prefix.codePointAt(0);
        if (!isBase(first) || prefix.endsWith(".")) {
            return false;
        }
        return prefix.codePoints().skip(1).allMatch(TurtleNames::isPrefixCharacter);
    }

    private static boolean isPrefixCharacter(int cp) {
        return isBase(cp) || cp == '_' || cp == '-' || cp == '.'
                || isWithin(cp, '0', '9') || cp == 0xB7
                || isWithin(cp, 0x0300, 0x036F)
                || isWithin(cp, 0x203F, 0x2040);
    }

    private static boolean isBase(int cp) {
        return isWithin(cp, 'A', 'Z') || isWithin(cp, 'a', 'z')
                || isWithin(cp, 0xC0, 0xD6) || isWithin(cp, 0xD8, 0xF6)
                || isWithin(cp, 0xF8, 0x2FF) || isWithin(cp, 0x370, 0x37D)
                || isWithin(cp, 0x37F, 0x1FFF) || isWithin(cp, 0x200C, 0x200D)
                || isWithin(cp, 0x2070, 0x218F) || isWithin(cp, 0x2C00, 0x2FEF)
                || isWithin(cp, 0x3001, 0xD7FF) || isWithin(cp, 0xF900, 0xFDCF)
                || isWithin(cp, 0xFDF0, 0xFFFD) || isWithin(cp, 0x10000, 0xEFFFF);
    }

    private static boolean isWithin(int cp, int first, int last) {
        return cp >= first && cp <= last;
    }
}
