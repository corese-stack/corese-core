package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RDFa10ParserTest {

    private static final ValueFactory factory = new CoreseAdaptedValueFactory();

}
