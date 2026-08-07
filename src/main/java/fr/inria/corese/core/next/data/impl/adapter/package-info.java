/**
 * Corese-legacy adapters: bridge between the {@code next} data model and
 * the existing Corese internal types (IDatatype, Edge, Node).
 *
 * <p>Nothing outside this package should depend on legacy Corese internals;
 * dependencies on {@code fr.inria.corese.core.sparql} belong here and must
 * not leak into the API or other impl packages.</p>
 */
package fr.inria.corese.core.next.data.impl.adapter;
