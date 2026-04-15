package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.next.query.kgram.sparql.exceptions.CoreseDatatypeException;
import fr.inria.corese.core.next.query.kgram.sparql.storage.api.IStorage;

/**
 * <p>
 * Title: Corese</p>
 * <p>
 * Description: A Semantic Search Engine</p>
 * <p>
 * Copyright: Copyright INRIA (c) 2007</p>
 * <p>
 * Company: INRIA</p>
 * <p>
 * Project: Acacia</p>
 * <br>
 * Subsume String Literal XMLLiteral UndefLiteral and Boolean In Corese they
 * compare with &lt;= &lt; >= > but not with = !=
 * @deprecated @TODO replace by fr.inria.corese.core.next.data class
 */
public class CoreseStringLiteral extends CoreseStringableImpl {

    private IStorage manager;
    private int id;

    public CoreseStringLiteral() {
    }

    public CoreseStringLiteral(String value) {
        super(value);

    }

    @Override
    public int compare(IDatatype iod) throws CoreseDatatypeException {
        switch (iod.getCode()) {
            case LITERAL:
            case STRING:
            case BOOLEAN:
            case XMLLITERAL:
                return getLabel().compareTo(iod.getLabel());
        }
        throw new CoreseDatatypeException("Comaprison could be done");
    }

    @Override
    public boolean less(IDatatype iod) throws CoreseDatatypeException {
        switch (iod.getCode()) {
            case LITERAL:
            case STRING:
            case BOOLEAN:
            case XMLLITERAL:
                return getLabel().compareTo(iod.getLabel()) < 0;
            //case UNDEF: return getLabel().compareTo(iod.getLabel()) < 0;
        }
        throw new CoreseDatatypeException("Comaprison could be done");
    }

    @Override
    public boolean lessOrEqual(IDatatype iod) throws CoreseDatatypeException {
        switch (iod.getCode()) {
            case LITERAL:
            case STRING:
            case BOOLEAN:
            case XMLLITERAL:
                return getLabel().compareTo(iod.getLabel()) <= 0;
            //case UNDEF: return getLabel().compareTo(iod.getLabel()) <= 0;
        }
        throw new CoreseDatatypeException("Comaprison could be done");
    }

    @Override
    public boolean greater(IDatatype iod) throws CoreseDatatypeException {
        switch (iod.getCode()) {
            case LITERAL:
            case STRING:
            case BOOLEAN:
            case XMLLITERAL:
                return getLabel().compareTo(iod.getLabel()) > 0;
            //case UNDEF: return getLabel().compareTo(iod.getLabel()) > 0;
        }
        throw new CoreseDatatypeException("Comaprison could be done");
    }

    @Override
    public boolean greaterOrEqual(IDatatype iod) throws CoreseDatatypeException {
        switch (iod.getCode()) {
            case LITERAL:
            case STRING:
            case BOOLEAN:
            case XMLLITERAL:
                return getLabel().compareTo(iod.getLabel()) >= 0;
            //case UNDEF: return getLabel().compareTo(iod.getLabel()) >= 0;
        }
        throw new CoreseDatatypeException("Comaprison could be done");
    }

    @Override
    public void setValue(String str, int nid, IStorage mgr) {
        if (str == null || str.isEmpty()) {
            return;
        }
        if (mgr == null) {
            this.setValue(str);
            return;
        }

        this.setManager(mgr);
        this.id = nid;
        manager.write(this.id, str);
        setValue("");
    }

    @Override
    public String getLabel() {
        if (manager == null) {
            return value;
        } else {
            String s = manager.read(this.id);
            if (s == null) {
                logger.error("Read string [" + id + "] from file error!");
                return value;
            }
            return s;
        }
    }

    /**
     * @return the manager
     */
    public IStorage getManager() {
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(IStorage manager) {
        this.manager = manager;
    }

}
