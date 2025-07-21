package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.impl.temp.CoreseNodeAdapter;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Interface for Corese datatype adapter.
 * This interface is to be used to apply the adapter design pattern around classes representing literals.
 *
 */
public interface CoreseDatatypeAdapter extends CoreseNodeAdapter {

    /**
     * Returns the adapted object.
     *
     * @return the adapted object
     */
    IDatatype getIDatatype();
}
