package fr.inria.corese.core.next.data.api.support.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IRIUtilsTest {

    private static final String uriSchema = "http://schema.org/test/test/name";
    private static final String uriWithFragment = "http://www.w3.org/2001/XMLSchema#string";
    private static final String uriWithQuery = "https://www.saramin.co.kr/zf_user/company-info/view?csn=VUczUnpnZ1FjeGdCUWRCblp6ZzYxQT09";
    private static final String uriWithPort = "http://www.w3.org:80/2001/XMLSchema#string";
    private static final String uriWithPortAndQuery = "http://www.w3.org:80/2001/XMLSchema?query=1#string";
    private static final String uriWithPortAndQueryAndFragment = "http://www.w3.org:80/2001/XMLSchema?query=1#fragment";
    private static final String uriWithPortAndFragment = "http://www.w3.org:80/2001/XMLSchema#string";
    private static final String uriToHTMLPage = "https://www.syuno-pit.biz/tezukayama-bandai-2.html";
    private static final String uriToHTMLPageWithQuery = "https://www.syuno-pit.biz/tezukayama-bandai-2.html?query=1";
    private static final String uriToHTMLPageWithQueryAndFragment = "https://www.syuno-pit.biz/tezukayama-bandai-2.html?query=1#fragment";
    private static final String uriToHTMLPageWithFragment = "https://www.syuno-pit.biz/tezukayama-bandai-2.html#fragment";
    private static final String blankNode = "_:n2d65906b09534cabb44314ff2e2b248axb4";
    private static final String uriWithUnexpectedCharactersObject = "http://example.org/obj&quot;ect&apos";
    private static final String uriWithUnexpectedCharactersSubject = "http://example.org/sub&ject";

    // Array of strings that should be recognized as correct IRIs. Some of them taken from the official IRI documentation.
    private static final String[] correctARIs = { uriSchema, uriWithFragment, uriWithQuery, uriWithPort, uriWithPortAndQuery, uriWithPortAndQueryAndFragment, uriWithPortAndFragment, uriToHTMLPage, uriToHTMLPageWithQuery, uriToHTMLPageWithQueryAndFragment, uriToHTMLPageWithFragment, "ftp://ftp.is.co.za/rfc/rfc1808.txt", "http://www.ietf.org/rfc/rfc2396.txt", "ldap://[2001:db8::7]/c=GB?objectClass?one", "mailto:John.Doe@example.com", "news:comp.infosystems.www.servers.unix", "tel:+1-816-555-1212", "telnet://192.0.2.16:80/", "urn:oasis:names:specification:docbook:dtd:xml:4.1.2", "http://foo.co.uk/", "http://regexr.com/foo.html?q=bar" };
    private static final String[] incorrectIRIs = {"test", "0123456789 +-.,!@#$%^&*()","12345 -98.7 3.141","555.123.4567\t+1-(800)","test\nstring","test\rstring","test\u0000string","   ","\u00A0","","  \t  ",                      // Only whitespace
     };
    @Test
    public void guessNamespaceTest() {
        assertEquals("http://schema.org/test/test/", IRIUtils.guessNamespace(uriSchema));
        assertEquals("http://www.w3.org/2001/XMLSchema#", IRIUtils.guessNamespace(uriWithFragment));
        assertEquals("https://www.saramin.co.kr/zf_user/company-info/", IRIUtils.guessNamespace(uriWithQuery));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(uriWithPort));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(uriWithPortAndQuery));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(uriWithPortAndQueryAndFragment));
        assertEquals("http://www.w3.org:80/2001/XMLSchema#", IRIUtils.guessNamespace(uriWithPortAndFragment));
        assertEquals("https://www.syuno-pit.biz/", IRIUtils.guessNamespace(uriToHTMLPage));
        assertEquals("https://www.syuno-pit.biz/", IRIUtils.guessNamespace(uriToHTMLPageWithQuery));
        assertEquals("https://www.syuno-pit.biz/tezukayama-bandai-2.html#", IRIUtils.guessNamespace(uriToHTMLPageWithQueryAndFragment));
        assertEquals("https://www.syuno-pit.biz/tezukayama-bandai-2.html#", IRIUtils.guessNamespace(uriToHTMLPageWithFragment));
        assertEquals("", IRIUtils.guessNamespace(blankNode));
        assertEquals("http://www.w3.org/2001/XMLSchema#", IRIUtils.guessNamespace("http://www.w3.org/2001/XMLSchema#"));
        assertEquals("http://example.org/", IRIUtils.guessNamespace(uriWithUnexpectedCharactersObject));
        assertEquals("http://example.org/", IRIUtils.guessNamespace(uriWithUnexpectedCharactersSubject));
    }

    @Test
    public void guessLocalNameTest() {
        assertEquals("name", IRIUtils.guessLocalName(uriSchema));
        assertEquals("string", IRIUtils.guessLocalName(uriWithFragment));
        assertEquals("view", IRIUtils.guessLocalName(uriWithQuery));
        assertEquals("string", IRIUtils.guessLocalName(uriWithPort));
        assertEquals("string", IRIUtils.guessLocalName(uriWithPortAndQuery));
        assertEquals("fragment", IRIUtils.guessLocalName(uriWithPortAndQueryAndFragment));
        assertEquals("string", IRIUtils.guessLocalName(uriWithPortAndFragment));
        assertEquals("tezukayama-bandai-2.html", IRIUtils.guessLocalName(uriToHTMLPage));
        assertEquals("tezukayama-bandai-2.html", IRIUtils.guessLocalName(uriToHTMLPageWithQuery));
        assertEquals("fragment", IRIUtils.guessLocalName(uriToHTMLPageWithQueryAndFragment));
        assertEquals("fragment", IRIUtils.guessLocalName(uriToHTMLPageWithFragment));
        assertEquals("", IRIUtils.guessLocalName(blankNode));
        assertEquals("obj&quot;ect&apos", IRIUtils.guessLocalName(uriWithUnexpectedCharactersObject));
        assertEquals("sub&ject", IRIUtils.guessLocalName(uriWithUnexpectedCharactersSubject));
    }

    @Test
    public void isStandardIRITest() {
        for (String iri : correctARIs) {
            assertTrue(IRIUtils.isStandardIRI(iri));
        }
        for (String iri : incorrectIRIs) {
            assertFalse(IRIUtils.isStandardIRI(iri), "Expected '" + escapeForDisplay(iri) + "' to be an invalid IRI");
        }
    }

    @Test
    public void isAbsoluteIRITest() {
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
