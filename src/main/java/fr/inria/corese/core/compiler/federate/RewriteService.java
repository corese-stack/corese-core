package fr.inria.corese.core.compiler.federate;

import fr.inria.corese.core.sparql.triple.parser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrite service (uri) { } as values ?serv { (uri) } service ?serv { }
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 */
public class RewriteService {

    FederateVisitor vis;
    int count = 0;
    boolean export = false;
    String name = Service.SERVER_SEED;
    ArrayList<Variable> varList;
    ArrayList<Service> serviceList;

    RewriteService(FederateVisitor vis) {
        this.vis = vis;
        varList = new ArrayList<>();
        serviceList = new ArrayList<>();
    }

    void process(ASTQuery ast) {
        if (ast.hasMetadata(Metadata.Type.PUBLIC)) {
            export = true;
        }
        process(ast.getBody());
    }

    void process(Exp body) {
        for (int i = 0; i < body.size(); i++) {
            Exp exp = body.get(i);
            if (exp.isService()) {
                Service s = exp.getService();
                if (!s.getServiceName().isVariable()) {
                    Variable variable = new Variable(name + count++);
                    varList.add(variable);
                    serviceList.add(s);
                    s.setServiceName(variable);
                    // bind service variable with values
                    Values values = Values.create(variable, s.getServiceConstantList());
                    s.clearServiceList();
                    body.add(i, values);
                    i++;
                }
            } else {
                process(exp);
            }
        }
    }

    List<Variable> getVarList() {
        return varList;
    }

    List<Service> getServiceList() {
        return serviceList;
    }


}
