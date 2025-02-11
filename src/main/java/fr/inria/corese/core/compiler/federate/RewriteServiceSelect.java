package fr.inria.corese.core.compiler.federate;

import fr.inria.corese.core.sparql.triple.parser.*;

/**
 * Rewrite service s { exp } as service s { select * where { exp } }
 */
public class RewriteServiceSelect {
    ASTQuery ast;

    RewriteServiceSelect(FederateVisitor vis) {
    }


    void process(ASTQuery ast) {
        this.ast = ast;
        process(ast.getBody());
    }

    void process(Exp body) {
        for (int i = 0; i < body.size(); i++) {
            Exp exp = body.get(i);
            if (exp.isService()) {
                Service s = exp.getService();
                if (!s.getBodyExp().isQuery()) {
                    Query q = getQuery(s);
                    s.setBodyExp(q);
                }
            }
        }
    }

    Query getQuery(Service s) {
        ASTQuery aa = ast.subCreate();
        aa.setBody(s.getBodyExp());
        aa.setSelectAll(true);
        if (ast.getMetaValue(Metadata.Type.LIMIT) != null) {
            aa.setLimit(ast.getLimit());
        }
        return Query.create(aa);
    }

}
