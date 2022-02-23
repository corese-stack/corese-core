module fr.inria.corese.corese_core {
    requires transitive fr.inria.corese.sparql;
    requires fr.inria.corese.compiler;
    requires org.slf4j;
    requires org.apache.commons.text;
    requires sesame.rio.api;
    requires java.xml;
    requires jsonld.java;
    requires semargl.core;
    requires sesame.model;
    requires arp;
    requires java.logging;
    requires java.sql;
    requires jakarta.ws.rs;
    requires java.management;
    requires commons.lang;
    requires semargl.rdfa;
    requires jdk.management;

    exports fr.inria.corese.core.load;
    exports fr.inria.corese.core;
    exports fr.inria.corese.core.query;
    exports fr.inria.corese.core.rule;
    exports fr.inria.corese.core.workflow;
    exports fr.inria.corese.core.transform;
    exports fr.inria.corese.core.util;
    exports fr.inria.corese.core.print;
    exports fr.inria.corese.core.pipe;
    exports fr.inria.corese.core.api;
    exports fr.inria.corese.core.edge;
    exports fr.inria.corese.core.logic;
    exports fr.inria.corese.core.producer;
    exports fr.inria.corese.core.shacl;
    exports fr.inria.corese.core.extension;
    exports fr.inria.corese.core.visitor.ldpath;
    exports fr.inria.corese.core.visitor.solver;
}
