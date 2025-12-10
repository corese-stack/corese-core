package fr.inria.corese.core.sparql.datatype.extension;

import fr.inria.corese.core.kgram.api.core.PointerType;
import fr.inria.corese.core.kgram.api.core.Pointerable;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.path.Path;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseUndefLiteral;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Extension IDatatype that contains LDScript Pointerable object
 * These objects implement an API that enables them to be processed by LDScript
 * statements such as for (?e in ?g), xt:gget(?e, "?x", 0) mainly speaking they are iterable
 * Pointerable objects have specific extension datatypes such as dt:graph
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 */
public class CoresePointer extends CoreseUndefLiteral {

    private static EnumMap<PointerType, IDatatype> map;

    static {
        init();
    }

    Pointerable pobject;

    public CoresePointer(Pointerable obj) {
        this(obj.getDatatypeLabel(), obj);
    }


    public CoresePointer(String name, Pointerable obj) {
        super(name);
        pobject = obj;
    }

    static void init() {
        map = new EnumMap<>(PointerType.class);
        for (PointerType type : PointerType.values()) {
            map.put(type, getGenericDatatype(type.getName()));
        }
    }

    public static IDatatype getDatatype(PointerType t) {
        return map.get(t);
    }

    @Override
    public IDatatype getDatatype() {
        return getDatatype(pointerType());
    }

    @Override
    public Pointerable getPointerObject() {
        return pobject;
    }

    @Override
    public PointerType pointerType() {
        if (pobject == null) {
            return PointerType.UNDEF;
        }
        return pobject.pointerType();
    }

    @Override
    public boolean isPointer() {
        return true;
    }

    @Override
    public boolean isExtension() {
        return pointerType() != PointerType.UNDEF;
    }

    @Override
    public boolean isUndefined() {
        return !isExtension();
    }

    @Override
    public Object getNodeObject() {
        // use case: pobject = PointerObject(object)
        return pobject.getPointerObject();
    }

    @Override
    public Path getPath() {
        if (pointerType() != PointerType.PATH || getPointerObject() == null) {
            return null;
        }
        return getPointerObject().getPathObject();
    }

    @Override
    public void setObject(Object obj) {
        if (obj instanceof Pointerable) {
            pobject = (Pointerable) obj;
        }
    }

    @Override
    public boolean isLoop() {
        if (pobject == null) {
            return false;
        }
        switch (pobject.pointerType()) {
            // expression must not be loopable in map(fun, exp)
            case EXPRESSION:
                return false;
            default:
                return true;
        }
    }

    @Override
    public int size() {
        return getPointerObject().size();
    }

    @Override
    public List<IDatatype> getValueList() {
        if (pobject == null) {
            return new ArrayList<>();
        }
        switch (pobject.pointerType()) {
            case EXPRESSION:
                return ((Expression) pobject).getValueList();

            case STATEMENT:
                return getValueList(pobject.getStatement());

            default:
                return super.getValueList();
        }
    }

    @Override
    public IDatatype getValue(String varString, int ind) {
        return DatatypeMap.getValue(pobject.getValue(varString, ind));
    }

    List<IDatatype> getValueList(Exp exp) {
        ArrayList<IDatatype> list = new ArrayList<>();
        for (Exp e : exp) {
            list.add(DatatypeMap.createObject(e));
        }
        return list;
    }

    @Override
    public Iterable getLoop() {
        if (pobject.pointerType() == PointerType.STATEMENT) {
            return getValueList();
        }
        return pobject.getLoop();
    }

    @Override
    public IDatatype display() {
        return DatatypeMap.createUndef(getContent(), getDatatypeURI());
    }

    public String display2() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(getContent()).append("\"");
        sb.append("^^").append(nsm().toPrefix(getDatatypeURI()));
        return sb.toString();
    }

    @Override
    public boolean equalsWE(IDatatype dt) throws CoreseDatatypeException {
        if (dt.isPointer()) {
            if (getPointerObject() == null || dt.getPointerObject() == null) {
                return getPointerObject() == dt.getPointerObject();
            }
            return getPointerObject().equals(dt.getPointerObject());
        }
        if (dt.isExtension()) {
            return false;
        }
        return super.equalsWE(dt);
    }

    public boolean equalsWE2(IDatatype dt) throws CoreseDatatypeException {
        if (dt.getCode() != IDatatype.Datatype.UNDEF || getDatatype() != dt.getDatatype()) {
            return super.equalsWE(dt);
        }
        if (getPointerObject() == null || dt.getPointerObject() == null) {
            return getPointerObject() == dt.getPointerObject();
        }
        return getPointerObject().equals(dt.getPointerObject());
    }

    /**
     * Pragma: they have same pointer type
     */
    @Override
    public int defaultCompare(IDatatype d2) {
        return getPointerObject().compare(d2.getPointerObject());
    }

    @Override
    public IDatatype set(IDatatype key, IDatatype value) {
        if (GRAPH_DATATYPE.equals(getDatatypeURI())) {
            getPointerObject().getTripleStore()
                    .set(key, value);
        }
        return value;
    }


}
