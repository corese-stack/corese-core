/**
 * Semantic validation applied to the AST after syntactic parsing.
 *
 * <p>Rules check constraints the ANTLR grammar cannot express — for example,
 * GROUP BY scope and type safety of built-in function arguments. Syntax
 * errors belong in the parser package, not here.</p>
 */
package fr.inria.corese.core.next.query.impl.parser.semantic;
