package fr.inria.corese.core.sparql.datatype.extension;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.IDatatypeList;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

import java.util.*;

import static fr.inria.corese.core.sparql.datatype.CoreseBoolean.FALSE;
import static fr.inria.corese.core.sparql.datatype.DatatypeMap.TRUE;

public class CoreseList extends CoreseExtension implements IDatatypeList {

    private static final String SEED = "_l_";
    private static final IDatatype dt = getGenericDatatype(IDatatype.LIST_DATATYPE);
    private static int count = 0;
    private List<IDatatype> list;

    public CoreseList(String value) {
        super(value);
    }

    public CoreseList() {
        super(SEED + count++);
    }

    public CoreseList(IDatatype[] dt) {
        super(SEED + count++);
        list = new ArrayList<>(dt.length);
        list.addAll(Arrays.asList(dt));
    }

    public CoreseList(List<IDatatype> vec) {
        super(SEED + count++);
        list = vec;
    }

    public static CoreseList create(List<IDatatype> vec) {
        return new CoreseList(vec);
    }

    public static CoreseList create(Collection<IDatatype> vec) {
        ArrayList<IDatatype> list = new ArrayList<IDatatype>(vec.size());
        list.addAll(vec);
        return new CoreseList(list);
    }

    // cannot add()
    public static CoreseList create(IDatatype[] dts) {
        return new CoreseList(dts);
    }

    public static CoreseList create() {
        return new CoreseList(new ArrayList<>());
    }

    @Override
    public IDatatype getDatatype() {
        return dt;
    }

    @Override
    public IDatatypeList getList() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        getContent(sb);
        sb.append("\"^^").append(nsm().toPrefix(getDatatypeURI()));
        return sb.toString();
    }

    void getContent(StringBuilder sb) {
        sb.append("(");
        for (IDatatype datatype : list) {
            sb.append((datatype.isList()) ? datatype.getContent() : datatype);
            sb.append(" ");
        }
        sb.append(")");
    }

    @Override
    public String getContent() {
        StringBuilder sb = new StringBuilder();
        getContent(sb);
        return sb.toString();
    }

    @Override
    public String toSparql(boolean prefix, boolean xsd) {
        return toString();
    }

    @Override
    public boolean equalsWE(IDatatype dt) throws CoreseDatatypeException {
        if (dt.isList()) {
            return getValues().equals(dt.getValues());
        }
        return super.equalsWE(dt);
    }

    @Override
    public int compareTo(IDatatype dt) {
        if (dt.isList()) {
            int res = Integer.compare(size(), dt.size());
            if (res != 0) {
                return res;
            }
            int i = 0;
            for (IDatatype val1 : getValues()) {
                res = val1.compareTo(dt.get(i++));
                if (res != 0) {
                    return res;
                }
            }
            return 0;
        } else {
            return super.compareTo(dt);
        }
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public Object getNodeObject() {
        return list;
    }

    public void set(IDatatype[] dts) {
        list = Arrays.asList(dts);
    }

    @Override
    public List<IDatatype> getValues() {
        return list;
    }

    @Override
    public List<IDatatype> getValueList() {
        return list;
    }

    @Override
    public IDatatype toList() {
        return this;
    }

    @Override
    public Iterator<IDatatype> iterator() {
        return getValues().iterator();
    }

    @Override
    public Iterable<IDatatype> getLoop() {
        return list;
    }

    @Override
    public boolean isLoop() {
        return true;
    }

    @Override
    public IDatatype get(int n) {
        return list.get(n);
    }

    @Override
    public int size() {
        if (list == null) {
            return 0;
        } else {
            return list.size();
        }
    }

    @Override
    public boolean isTrue() {
        return list != null && !list.isEmpty();
    }

    @Override
    public boolean isTrueAble() {
        return true;
    }

    @Override
    public boolean booleanValue() {
        return isTrue();
    }

    @Override
    public IDatatype length() {
        return DatatypeMap.newInstance(list.size());
    }

    @Override
    public IDatatype first() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public IDatatype rest() {
        return rest(1, list.size());
    }

    @Override
    public IDatatype rest(IDatatype index) {
        return rest(index.intValue(), list.size());
    }

    @Override
    public IDatatype rest(IDatatype index, IDatatype last) {
        return rest(index.intValue(), list.size() - last.intValue());
    }

    IDatatype rest(int index, int size) {
        ArrayList<IDatatype> res = new ArrayList<>(list.size());
        for (int i = index; i < size; i++) {
            res.add(list.get(i));
        }
        return new CoreseList(res);
    }

    @Override
    public IDatatype add(IDatatype elem) {
        list.add(elem);
        return this;
    }

    @Override
    public IDatatype add(IDatatype n, IDatatype elem) {
        list.add(n.intValue(), elem);
        return this;
    }

    @Override
    public IDatatype addAll(IDatatype list) {
        getValueList().addAll(list.getValueList());
        return this;
    }

    @Override
    public IDatatype swap(IDatatype i1, IDatatype i2) {
        IDatatype swapDt = list.get(i1.intValue());
        list.set(i1.intValue(), list.get(i2.intValue()));
        list.set(i2.intValue(), swapDt);
        return this;
    }

    // copy
    @Override
    public IDatatype cons(IDatatype elem) {
        ArrayList<IDatatype> res = new ArrayList<>(list.size() + 1);
        res.add(elem);
        res.addAll(list);
        return new CoreseList(res);
    }

    @Override
    public IDatatypeList append(IDatatype dtlist) {
        if (!dtlist.isList()) {
            return null;
        }
        ArrayList<IDatatype> res = new ArrayList<>(list.size() + dtlist.size());
        res.addAll(list);
        res.addAll(dtlist.getValues());
        return new CoreseList(res);
    }

    // remove duplicates
    @Override
    public IDatatype merge(IDatatype dtlist) {
        if (!dtlist.isList()) {
            return null;
        }
        ArrayList<IDatatype> res = new ArrayList<>();
        CoreseList mergeList = new CoreseList(res);
        for (IDatatype dt1 : getValues()) {
            if (!mergeList.contains(dt1)) {
                mergeList.add(dt1);
            }
        }
        for (IDatatype dt2 : dtlist.getValues()) {
            if (!mergeList.contains(dt2)) {
                mergeList.add(dt2);
            }
        }
        return mergeList;
    }

    @Override
    public boolean contains(IDatatype dt) {
        for (IDatatype elem : getValues()) {
            if (dt.sameTerm(elem)) {
                return true;
            }
        }
        return false;
    }

    // this is a list, possibly list of lists
    // merge lists and remove duplicates
    @Override
    public IDatatype merge() {
        ArrayList<IDatatype> res = new ArrayList<>();

        for (IDatatype dt1 : list) {
            if (dt1.isList()) {
                for (IDatatype elem : dt1.getValues()) {
                    if (!res.contains(elem)) {
                        res.add(elem);
                    }
                }
            } else if (!res.contains(dt1)) {
                res.add(dt1);
            }
        }

        return new CoreseList(res);
    }

    @Override
    public IDatatype get(IDatatype n) {
        if (n.intValue() >= list.size()) {
            return null;
        }
        return list.get(n.intValue());
    }

    @Override
    public IDatatype last(IDatatype n) {
        int i = list.size() - n.intValue() - 1;
        if (i < 0) {
            return null;
        }
        return list.get(i);
    }

    @Override
    public IDatatype set(IDatatype n, IDatatype val) {
        if (n.intValue() >= list.size()) {
            return null;
        }
        list.set(n.intValue(), val);
        return val;
    }

    // modify
    @Override
    public IDatatype remove(IDatatype dt) {
        list.remove(dt);
        return this;
    }

    @Override
    public IDatatype remove(int n) {
        if (n >= list.size()) {
            return null;
        }
        list.remove(n);
        return this;
    }


    @Override
    public IDatatype reverse() {
        ArrayList<IDatatype> res = new ArrayList<>(list.size());
        int n = list.size() - 1;
        for (int i = 0; i < list.size(); i++) {
            res.add(list.get(n - i));
        }
        return new CoreseList(res);
    }

    // modify list
    @Override
    public IDatatype sort() {
        Collections.sort(list);
        return this;
    }

    @Override
    public IDatatype member(IDatatype elem) {
        return list.contains(elem) ? TRUE : FALSE;
    }

    // because list has its own compareTo (see above)
    @Override
    public int mapCompareTo(IDatatype dt) {
        if (dt.isList()) {
            return getLabel().compareTo(dt.getLabel());
        }
        return super.compareTo(dt);
    }

    /**
     * this is a list of json objects
     * each json object records copy of the list
     */
    @Override
    public IDatatype dispatch(String name) {
        if (size() > 1) {
            IDatatype dispatchList = duplicate();
            for (IDatatype datatype : this) {
                // each item record the list of items 
                datatype.set(name, dispatchList);
            }
        }
        return this;
    }

    @Override
    public IDatatypeList duplicate() {
        ArrayList<IDatatype> duplicateList = new ArrayList<>();
        for (IDatatype dt1 : this) {
            duplicateList.add(dt1.duplicate());
        }
        return new CoreseList(duplicateList);
    }

}
