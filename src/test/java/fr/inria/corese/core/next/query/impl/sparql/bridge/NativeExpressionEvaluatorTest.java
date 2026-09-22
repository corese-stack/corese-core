package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SPARQL REGEX built-in with XPath flag support.
 *
 */
@DisplayName("NativeExpressionEvaluator - XPath REGEX flag support")
class NativeExpressionEvaluatorTest {

    private boolean ask(String filter) {
        try (var repo = Repositories.create(MemoryStorageManager.builder().build());
             var conn = repo.getConnection()) {
            return conn.prepareBooleanQuery(
                    "PREFIX ex: <http://example.com/#> ASK { FILTER(" + filter + ") }").evaluate();
        }
    }


    @Test
    @DisplayName("REGEX with i flag matches case-insensitively")
    void iFlagMatchesCaseInsensitively() {
        assertTrue(ask("REGEX(\"ABCdefGHIjkl\", \"abc\", \"i\")"));
        assertTrue(ask("REGEX(\"abcDEFghiJKL\", \"ABC\", \"i\")"));
    }

    @Test
    @DisplayName("REGEX with s flag makes dot match newlines")
    void sFlagDotMatchesNewline() {
        assertTrue(ask("REGEX(\"a\\nb\", \"a.b\", \"s\")"));
        assertFalse(ask("REGEX(\"a\\nb\", \"a.b\")"));
    }


    @Test
    @DisplayName("REGEX with m flag makes ^ and $ match line boundaries")
    void mFlagAnchorMatchesLineBoundary() {
        assertTrue(ask("REGEX(\"foo\\nbar\", \"^bar$\", \"m\")"));
        assertFalse(ask("REGEX(\"foo\\nbar\", \"^bar$\")"));
    }


    @Test
    @DisplayName("REGEX with x flag ignores whitespace in pattern")
    void xFlagIgnoresWhitespace() {
        assertTrue(ask("REGEX(\"ac\", \" a c \", \"x\")"));
    }

    @Test
    @DisplayName("REGEX with x flag ignores newline and tab in pattern")
    void xFlagIgnoresNewlineAndTab() {
        assertTrue(ask("REGEX(\"ac\", \" a\\n\\tc \", \"x\")"));
    }

    @Test
    @DisplayName("REGEX with x flag preserves whitespace inside bracket expression")
    void xFlagPreservesWhitespaceInsideBrackets() {
        assertTrue(ask("REGEX(\"a\\nc\", \" a[\\\\n]c \", \"x\")"));
    }

    @Test
    @DisplayName("REGEX with x flag preserves hashes and whitespace in classes")
    void xFlagPreservesHashesAndClassWhitespace() {
        assertFalse(ask("REGEX(\"a\", \"a#b\", \"x\")"));
        assertTrue(ask("REGEX(\"a#b\", \" a # b \", \"x\")"));
        assertTrue(ask("REGEX(\"#\", \"[#]\", \"x\")"));
        assertTrue(ask("REGEX(\" \", \"[ a]\", \"x\")"));
        assertTrue(ask("REGEX(\"\\n\\t\\r\", \"^[ \\n\\t\\r]+$\", \"x\")"));
        assertFalse(ask("REGEX(\"ac\", \"a[ ]c\", \"x\")"));
    }

    @Test
    void xFlagRespectsEscapedBrackets() {
        assertTrue(ask("REGEX(\"[a]\", \"\\\\[ a \\\\]\", \"x\")"));
        assertTrue(ask("REGEX(\"] \", \"^[\\\\] ]+$\", \"x\")"));
    }

    @Test
    void qFlagOverridesOtherPatternFlags() {
        assertTrue(ask("REGEX(\" A#b \", \" a#b \", \"qxims\")"));
        assertFalse(ask("REGEX(\"ab\", \" a b \", \"qx\")"));
        assertFalse(ask("REGEX(\"a\\nb\", \"a.b\", \"qs\")"));
        assertFalse(ask("REGEX(\"a\\nb\", \"^b$\", \"qm\")"));
    }

    @Test
    void qFlagQuotesReplacement() {
        assertTrue(ask("REPLACE(\"a/b\", \"/\", \"$0\", \"q\") = \"a$0b\""));
        assertTrue(ask("REPLACE(\"a/b\", \"/\", \"$\", \"q\") = \"a$b\""));
        assertTrue(ask("REPLACE(\"a/b\", \"/\", \"\\\\\", \"q\") = \"a\\\\b\""));
        assertTrue(ask("REPLACE(\"a/b\", \"/\", \"$0\") = \"a/b\""));
    }

    @Test
    void xFlagAlsoAppliesToReplace() {
        assertTrue(ask("REPLACE(\"a b\", \"[ ]\", \"-\", \"x\") = \"a-b\""));
        assertTrue(ask("REPLACE(\"a#b\", \"#\", \"-\", \"x\") = \"a-b\""));
    }

    @Test
    @DisplayName("REGEX with q flag treats pattern as literal (no metacharacters)")
    void qFlagTreatsPatternAsLiteral() {
        assertTrue(ask("REGEX(\"a?+*.{}()[]c\", \"a?+*.{}()[]c\", \"q\")"));
        assertFalse(ask("REGEX(\"abc\", \"a?+*.{}()[]c\", \"q\")"));
    }

    @Test
    @DisplayName("REGEX with iq flags matches literal pattern case-insensitively")
    void iqFlagsMatchLiteralCaseInsensitively() {
        assertTrue(ask("REGEX(\"a?+*.{}()[]c\", \"a?+*.{}()[]C\", \"iq\")"));
        assertFalse(ask("REGEX(\"abc\", \"a?+*.{}()[]C\", \"iq\")"));
    }

    @Test
    @DisplayName("REGEX with [] expression matches 'b' in character class")
    void bracketExpressionMatchesB() {
        assertTrue(ask("REGEX(\"abc\", \"a[b\\\\n]c\")"));
    }

    @Test
    @DisplayName("REGEX with [] expression matches newline in character class")
    void bracketExpressionMatchesNewline() {
        assertTrue(ask("REGEX(\"a\\nc\", \"a[b\\\\n]c\")"));
    }

    @Test
    @DisplayName("REGEX with [] expression does not match unrelated characters")
    void bracketExpressionDoesNotMatchOther() {
        assertFalse(ask("REGEX(\"adc\", \"a[b\\\\n]c\")"));
    }
}
