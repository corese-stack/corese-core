package fr.inria.corese.core.next.api;

import fr.inria.corese.core.next.impl.common.serialization.RdfFormat;

public interface ISerializerFactory {


    IRdfSerializer createSerializer(RdfFormat format, Model model, ISerializationConfig config);
}
