package fr.inria.corese.core.sparql.triple.parser.context;

import fr.inria.corese.core.kgram.api.core.PointerType;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

import java.util.*;

import static fr.inria.corese.core.sparql.triple.cst.LogKey.PREF;

/**
 * property -> value
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2021
 */
public class PropertyMap extends HashMap<String, IDatatype> {

    IDatatype getCreateList(String property) {
        IDatatype dt = get(property);
        if (dt == null) {
            dt = DatatypeMap.newList();
            put(property, dt);
        }
        return dt;
    }

    public List<String> getKeys() {
        return sort(keySet());
    }

    public StringBuilder display() {
        return display(new StringBuilder());
    }

    public StringBuilder display(StringBuilder sb) {
        int n = 0;
        for (String prop : getKeys()) {
            IDatatype dt = get(prop);
            if (dt != null) {
                sb.append(String.format("%s%s %s", (n++ == 0) ? "" : " ;\n", prop, pretty(dt)));
            }
        }
        sb.append(" .\n");
        return sb;
    }

    String pretty(IDatatype dt) {
        if (dt.isList()) {
            return dt.getContent();
        }

        if (dt.isPointer()) {
            if (dt.pointerType() == PointerType.MAPPINGS) {
                Mappings map = dt.getPointerObject().getMappings();
                return String.format("\"\"\"\n%s\"\"\"", map.toString(false, false, map.getDisplay()));
            } else if (dt.pointerType() == PointerType.UNDEF) {
                return String.format("\"\"\"\n%s\"\"\"", dt.getPointerObject().getPointerObject());
            }
        }

        if (dt.isLiteral() && dt.getLabel().startsWith(PREF)) {
            // ns:label
            return dt.getLabel();
        }

        return dt.toString();
    }

    ArrayList<String> sort(Collection<String> set) {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(set);
        Collections.sort(list);
        return list;
    }


}
