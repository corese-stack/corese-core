package fr.inria.corese.core.next.data.api.support.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IRIUtilsTest {

    private static final String URI_SCHEMA = "http://schema.org/test/test/name";
    private static final String URI_WITH_FRAGMENT = "http://www.w3.org/2001/XMLSchema#string";
    private static final String URI_WITH_QUERY = "https://www.saramin.co.kr/zf_user/company-info/view?csn=VUczUnpnZ1FjeGdCUWRCblp6ZzYxQT09";
    private static final String URI_WITH_PORT = "http://www.w3.org:80/2001/XMLSchema#string";
    private static final String URI_WITH_PORT_AND_QUERY = "http://www.w3.org:80/2001/XMLSchema?query=1#string";
    private static final String URI_WITH_PORT_AND_QUERY_AND_FRAGMENT = "http://www.w3.org:80/2001/XMLSchema?query=1#fragment";
    private static final String URI_WITH_PORT_AND_FRAGMENT = "http://www.w3.org:80/2001/XMLSchema#string";
    private static final String URI_TO_HTML_PAGE = "https://www.syuno-pit.biz/tezukayama-bandai-2.html";
    private static final String URI_TO_HTML_PAGE_WITH_QUERY = "https://www.syuno-pit.biz/tezukayama-bandai-2.html?query=1";
    private static final String URI_TO_HTML_PAGE_WITH_QUERY_AND_FRAGMENT = "https://www.syuno-pit.biz/tezukayama-bandai-2.html?query=1#fragment";
    private static final String URI_TO_HTML_PAGE_WITH_FRAGMENT = "https://www.syuno-pit.biz/tezukayama-bandai-2.html#fragment";
    private static final String BLANK_NODE = "_:n2d65906b09534cabb44314ff2e2b248axb4";
    private static final String URI_WITH_UNEXPECTED_CHARACTERS_OBJECT = "http://example.org/obj&quot;ect&apos";
    private static final String URI_WITH_UNEXPECTED_CHARACTERS_SUBJECT = "http://example.org/sub&ject";

    // Array of strings that should be recognized as correct IRIs. Some of them taken from the official IRI documentation.
    private static final String[] correctARIs = { URI_SCHEMA, URI_WITH_FRAGMENT, URI_WITH_QUERY, URI_WITH_PORT, URI_WITH_PORT_AND_QUERY, URI_WITH_PORT_AND_QUERY_AND_FRAGMENT, URI_WITH_PORT_AND_FRAGMENT, URI_TO_HTML_PAGE, URI_TO_HTML_PAGE_WITH_QUERY, URI_TO_HTML_PAGE_WITH_QUERY_AND_FRAGMENT, URI_TO_HTML_PAGE_WITH_FRAGMENT, "ftp://ftp.is.co.za/rfc/rfc1808.txt", "http://www.ietf.org/rfc/rfc2396.txt", "ldap://[2001:db8::7]/c=GB?objectClass?one", "mailto:John.Doe@example.com", "news:comp.infosystems.www.servers.unix", "tel:+1-816-555-1212", "telnet://192.0.2.16:80/", "urn:oasis:names:specification:docbook:dtd:xml:4.1.2", "http://foo.co.uk/", "http://regexr.com/foo.html?q=bar" };
    private static final String[] incorrectIRIs = {"test", "0123456789 +-.,!@#$%^&*()","12345 -98.7 3.141","555.123.4567\t+1-(800)","test\nstring","test\rstring","test\u0000string","   ","\u00A0","","  \t  ",                      // Only whitespace
     };
    @Test
    void guessNamespaceTest() {
        assertEquals("http://schema.org/test/test/", IRIUtils.guessNamespace(URI_SCHEMA));
        assertEquals("http://www.w3.org/2001/XMLSchema#", IRIUtils.guessNamespace(URI_WITH_FRAGMENT));
        assertEquals("https://www.saramin.co.kr/zf_user/company-info/", IRIUtils.guessNamespace(URI_WITH_QUERY));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(URI_WITH_PORT));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(URI_WITH_PORT_AND_QUERY));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(URI_WITH_PORT_AND_QUERY_AND_FRAGMENT));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(URI_WITH_PORT_AND_FRAGMENT));
        assertEquals("https://www.syuno-pit.biz/", IRIUtils.guessNamespace(URI_TO_HTML_PAGE));
        assertEquals("https://www.syuno-pit.biz/", IRIUtils.guessNamespace(URI_TO_HTML_PAGE_WITH_QUERY));
        assertEquals("https://www.syuno-pit.biz/tezukayama-bandai-2.html#", IRIUtils.guessNamespace(URI_TO_HTML_PAGE_WITH_QUERY_AND_FRAGMENT));
        assertEquals("https://www.syuno-pit.biz/tezukayama-bandai-2.html#", IRIUtils.guessNamespace(URI_TO_HTML_PAGE_WITH_FRAGMENT));
        assertEquals("", IRIUtils.guessNamespace(BLANK_NODE));
        assertEquals("http://www.w3.org/2001/XMLSchema#", IRIUtils.guessNamespace("http://www.w3.org/2001/XMLSchema#"));
        assertEquals("http://example.org/", IRIUtils.guessNamespace(URI_WITH_UNEXPECTED_CHARACTERS_OBJECT));
        assertEquals("http://example.org/", IRIUtils.guessNamespace(URI_WITH_UNEXPECTED_CHARACTERS_SUBJECT));
    }

    @Test
    void guessLocalNameTest() {
        assertEquals("name", IRIUtils.guessLocalName(URI_SCHEMA));
        assertEquals("string", IRIUtils.guessLocalName(URI_WITH_FRAGMENT));
        assertEquals("view", IRIUtils.guessLocalName(URI_WITH_QUERY));
        assertEquals("string", IRIUtils.guessLocalName(URI_WITH_PORT));
        assertEquals("string", IRIUtils.guessLocalName(URI_WITH_PORT_AND_QUERY));
        assertEquals("fragment", IRIUtils.guessLocalName(URI_WITH_PORT_AND_QUERY_AND_FRAGMENT));
        assertEquals("string", IRIUtils.guessLocalName(URI_WITH_PORT_AND_FRAGMENT));
        assertEquals("tezukayama-bandai-2.html", IRIUtils.guessLocalName(URI_TO_HTML_PAGE));
        assertEquals("tezukayama-bandai-2.html", IRIUtils.guessLocalName(URI_TO_HTML_PAGE_WITH_QUERY));
        assertEquals("fragment", IRIUtils.guessLocalName(URI_TO_HTML_PAGE_WITH_QUERY_AND_FRAGMENT));
        assertEquals("fragment", IRIUtils.guessLocalName(URI_TO_HTML_PAGE_WITH_FRAGMENT));
        assertEquals("", IRIUtils.guessLocalName(BLANK_NODE));
        assertEquals("obj&quot;ect&apos", IRIUtils.guessLocalName(URI_WITH_UNEXPECTED_CHARACTERS_OBJECT));
        assertEquals("sub&ject", IRIUtils.guessLocalName(URI_WITH_UNEXPECTED_CHARACTERS_SUBJECT));
    }

    @Test
    void isStandardIRITest() {
        for (String iri : correctARIs) {
            assertTrue(IRIUtils.isStandardIRI(iri));
        }
        for (String iri : incorrectIRIs) {
            assertFalse(IRIUtils.isStandardIRI(iri), "Expected '" + escapeForDisplay(iri) + "' to be an invalid IRI");
        }
    }

    @Test
    void isAbsoluteIRITest() {
        assertTrue(IRIUtils.isAbsoluteIRI("mailto://user@example.com"));
        assertTrue(IRIUtils.isAbsoluteIRI("mongodb://user:password@127.0.0.1:3307"));
        assertTrue(IRIUtils.isAbsoluteIRI("https://laconsole.dev"));
        assertTrue(IRIUtils.isAbsoluteIRI("http://127.0.0.1:3000"));
        assertTrue(IRIUtils.isAbsoluteIRI("urn:isbn:978-2-7654-0912-0"));
        assertTrue(IRIUtils.isAbsoluteIRI("urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6"));
        assertTrue(IRIUtils.isAbsoluteIRI("urn:ietf:rfc:2648"));
        assertTrue(IRIUtils.isAbsoluteIRI("https://www.w3.org/TR/rdf-sparql-query/#iriRefs"));
        assertTrue(IRIUtils.isAbsoluteIRI("https://ns.inria.fr/otherTest1/#"));
        assertTrue(IRIUtils.isAbsoluteIRI("https://www.w3.org/TR/rdf-sparql-query/#iriRefs"));
        assertTrue(IRIUtils.isAbsoluteIRI("http://xmlns.com/foaf/0.1/"));
        assertFalse(IRIUtils.isAbsoluteIRI("child/password@127.0.0.1:3307"));
        assertFalse(IRIUtils.isAbsoluteIRI("child/otherChild/otherotherchild/#patate"));
    }

    /**
     * Helper method to escape strings for display in test failure messages
     */
    private static String escapeForDisplay(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c < 0x20 || (c >= 0x7F && c <= 0x9F)) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }


}
