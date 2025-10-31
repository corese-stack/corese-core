package fr.inria.corese.core.next.impl.io.parser.rdfa;

import org.junit.jupiter.api.Test;

public class RDFaParserTest {

    @Test
    public void basicDocTest() {
        String docString = """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:dc="http://purl.org/dc/elements/1.1/">
<head>
	<title>Test 0001</title>
</head>
<body>
	<p>This photo was taken by <span class="author" about="photo1.jpg" property="dc:creator">Mark Birbeck</span>.</p>
</body>
</html>""";

        /*
        	<http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/photo1.jpg> <http://purl.org/dc/elements/1.1/creator> "Mark Birbeck" .
         */
    }
}
