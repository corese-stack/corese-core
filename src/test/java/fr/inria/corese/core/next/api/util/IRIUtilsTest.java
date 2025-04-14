package fr.inria.corese.core.next.api.util;

import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.*;

public class IRIUtilsTest {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(IRIUtilsTest.class);

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

    // Array of strings that should be recognized as correct IRIs. Some of them taken from the official IRI documentation.
    private static final String[] correctARIs = { uriSchema, uriWithFragment, uriWithQuery, uriWithPort, uriWithPortAndQuery, uriWithPortAndQueryAndFragment, uriWithPortAndFragment, uriToHTMLPage, uriToHTMLPageWithQuery, uriToHTMLPageWithQueryAndFragment, uriToHTMLPageWithFragment, "ftp://ftp.is.co.za/rfc/rfc1808.txt", "http://www.ietf.org/rfc/rfc2396.txt", "ldap://[2001:db8::7]/c=GB?objectClass?one", "mailto:John.Doe@example.com", "news:comp.infosystems.www.servers.unix", "tel:+1-816-555-1212", "telnet://192.0.2.16:80/", "urn:oasis:names:specification:docbook:dtd:xml:4.1.2", "http://foo.co.uk/", "http://regexr.com/foo.html?q=bar" };
    private static final String[] incorrectIRIs = { "0123456789 +-.,!@#$%^&*();\\\\/|<>\\\"\\'", "12345 -98.7 3.141 .6180 9,000 +42", "555.123.4567\t+1-(800)-555-2468", "foodemo.net", "bar.ba.test.co.uk", "www.demo.com", "g.com", "g-.com", "com.g", "-g.com", "xn--d1ai6ai.xn--p1ai", "xn-fsqu00a.xn-0zwm56d", "xn--stackoverflow.com", "stackoverflow.xn--com", "stackoverflow.co.uk", "google.com.au", "-0-0o.com", "0-0o_.com" };

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
    }

    @Test
    public void isStandardIRITest() {
        for (String iri : correctARIs) {
            assertTrue(IRIUtils.isStandardIRI(iri));
        }
        for (String iri : incorrectIRIs) {
            assertFalse(IRIUtils.isStandardIRI(iri));
        }
    }

}
