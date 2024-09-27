module fr.inria.corese.corese_core {
    requires org.apache.commons.text;
    requires java.xml;
    requires semargl.core;
    requires arp; //
    requires java.logging;
    requires java.sql;
    requires jakarta.ws.rs;
    requires java.management;
    requires java.desktop;
    requires semargl.rdfa; //
    requires jdk.management;
    requires org.json; //
    requires org.apache.commons.lang3;
    requires shexjava.core;
    requires junit; //

    exports fr.inria.corese.core.load;
    exports fr.inria.corese.core.load.result;
    exports fr.inria.corese.core;
    exports fr.inria.corese.core.query;
    exports fr.inria.corese.core.rule;
    exports fr.inria.corese.core.workflow;
    exports fr.inria.corese.core.transform;
    exports fr.inria.corese.core.util;
    exports fr.inria.corese.core.index;
    exports fr.inria.corese.core.print;
    exports fr.inria.corese.core.print.rdfc10;
    exports fr.inria.corese.core.api;
    exports fr.inria.corese.core.edge;
    exports fr.inria.corese.core.logic;
    exports fr.inria.corese.core.producer;
    exports fr.inria.corese.core.shacl;
    exports fr.inria.corese.core.extension;
    exports fr.inria.corese.core.visitor.ldpath;
    exports fr.inria.corese.core.visitor.solver;
    exports fr.inria.corese.core.storage;
    exports fr.inria.corese.core.storage.api.dataManager;

    exports fr.inria.corese.core.compiler.parser;
    exports fr.inria.corese.core.compiler.eval;
    exports fr.inria.corese.core.compiler.api;
    exports fr.inria.corese.core.compiler.result;
    exports fr.inria.corese.core.compiler.federate;

    exports fr.inria.corese.core.shex.shacl;

    exports fr.inria.corese.core.kgram.core;
    exports fr.inria.corese.core.kgram.api.core;
    exports fr.inria.corese.core.kgram.api.query;
    exports fr.inria.corese.core.kgram.filter;
    exports fr.inria.corese.core.kgram.event;
    exports fr.inria.corese.core.kgram.tool;
    exports fr.inria.corese.core.kgram.sorter.core;
    exports fr.inria.corese.core.kgram.path;
    exports fr.inria.corese.core.kgram.sorter.impl.qpv1;

    exports fr.inria.corese.core.sparql.triple.parser;
    exports fr.inria.corese.core.sparql.triple.parser.visitor;
    exports fr.inria.corese.core.sparql.triple.parser.context;
    exports fr.inria.corese.core.sparql.exceptions;
    exports fr.inria.corese.core.sparql.datatype;
    exports fr.inria.corese.core.sparql.datatype.extension;
    exports fr.inria.corese.core.sparql.api;
    exports fr.inria.corese.core.sparql.triple.cst;
    exports fr.inria.corese.core.sparql.triple.update;
    exports fr.inria.corese.core.sparql.triple.function.script;
    exports fr.inria.corese.core.sparql.triple.function.extension;
    exports fr.inria.corese.core.sparql.triple.function.term;
    exports fr.inria.corese.core.sparql.triple.function.proxy;
    exports fr.inria.corese.core.sparql.compiler.java;
    exports fr.inria.corese.core.sparql.datatype.function;
    exports fr.inria.corese.core.sparql.storage.api;
    exports fr.inria.corese.core.sparql.storage.util;
    exports fr.inria.corese.core.sparql.triple.printer;
    exports fr.inria.corese.core.sparql.triple.api;
    exports fr.inria.corese.core.sparql.triple.function.core;
    exports fr.inria.corese.core.sparql.storage.fs;
}
