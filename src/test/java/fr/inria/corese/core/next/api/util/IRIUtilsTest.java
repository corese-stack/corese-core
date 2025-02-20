package fr.inria.corese.core.next.api.util;

import fr.inria.corese.core.next.api.model.util.IRIUtils;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;

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

    public void guessLocalNameTest() {
        assertEquals("name", IRIUtils.guessNamespace(uriSchema));
        assertEquals("string", IRIUtils.guessNamespace(uriWithFragment));
        assertEquals("view", IRIUtils.guessNamespace(uriWithQuery));
        assertEquals("string", IRIUtils.guessNamespace(uriWithPort));
        assertEquals("string", IRIUtils.guessNamespace(uriWithPortAndQuery));
        assertEquals("fragment", IRIUtils.guessNamespace(uriWithPortAndQueryAndFragment));
        assertEquals("string", IRIUtils.guessNamespace(uriWithPortAndFragment));
        assertEquals("tezukayama-bandai-2.html", IRIUtils.guessNamespace(uriToHTMLPage));
        assertEquals("tezukayama-bandai-2.html", IRIUtils.guessNamespace(uriToHTMLPageWithQuery));
        assertEquals("fragment", IRIUtils.guessNamespace(uriToHTMLPageWithQueryAndFragment));
        assertEquals("fragment", IRIUtils.guessNamespace(uriToHTMLPageWithFragment));
        assertEquals("", IRIUtils.guessNamespace(blankNode));
    }
}
