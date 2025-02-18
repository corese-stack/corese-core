package fr.inria.corese.core.next.api.model;

import java.io.Serializable;

public interface Namespace extends Serializable, Comparable<Namespace> {

    String getPrefix();

    String getName();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

}
