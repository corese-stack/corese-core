# Engine / AST dependency boundary

The target is an execution engine independent of parser and SPARQL AST types.
The current engine still exposes three dependencies. `NextModuleBoundaryTest`
inventories them as exact source-path / referenced-type pairs, rather than
exempting whole files or packages.

## Existing exceptions

Paths below are relative to `next/query/impl/engine`.

| Source | AST type | Current use and callers |
| --- | --- | --- |
| `model/Filter.java` | `TermAst` | Return types of `getFilterExpression()` and `coreseNextSource()`. `NextFilterFromAst` implements both in the bridge. `Exp` delegates the first; `AstBackedExprTest` checks its result. No production caller of `coreseNextSource()` was found in `next`. |
| `pattern/Exp.java` | `TermAst` | Return type of `getFilterExpression()`, delegating to `Filter`. No production caller of this accessor was found in `next`. |
| `pattern/Query.java` | `QueryAst` | Field `ast` and accessors `getAST()`, `getGlobalAST()`, `setAST()`. `CoreseAstQueryBuilder` sets the source AST for each query form; `QueryTest` tests the accessors. No external production reader of these accessors was found in `next`. |

This is a source-reference inventory, not a statement that these APIs can be
removed without checking downstream clients. Bridge implementations may still
own AST objects internally after the engine contracts stop exposing their types.

## Guard behavior

- Parser dependencies have no exceptions.
- Both the actual `query.impl.sparql.ast` package and the historical
  `query.impl.ast` package are checked.
- Imports, wildcard imports, static references and fully qualified references
  are detected by the existing source-reference matching approach.
- Adding a type to an already listed file is rejected.
- Adding an existing AST type to another file is rejected.
- Removing a dependency requires removing its exception in the same change.

The source scanner also sees fully qualified names in comments and strings. It
does not perform bytecode or transitive dependency analysis. Its regression test
uses synthetic sources to check new files, new types, wildcard imports and fully
qualified references outside imports.

## Follow-up: remove the exceptions

1. Check repository-wide and downstream uses of the source-AST accessors before
   removing them; assess compatibility of these implementation classes.
2. Remove the unused `Exp` forwarding accessor and the AST accessors from the
   engine `Filter` contract. Keep source-AST access inside the bridge where needed;
   adapt `AstBackedExprTest` to test bridge behavior rather than an engine AST API.
3. Remove AST retention from `Query` if no required consumer exists. If diagnostics
   need source information, keep it in a bridge-owned structure with an explicit
   lifecycle, rather than replacing typed accessors with `Object` or unsafe casts.
4. Remove the corresponding builder writes and accessor-only tests. Preserve
   behavioral tests for query forms, expressions and subqueries.
5. Delete each exception as its dependency disappears, then restore the strict
   no-AST-dependencies invariant in the engine package documentation.

Do not introduce new runtime behavior as part of this follow-up. Validate with
the architecture tests, the full core test suite and a comparison against the
existing W3C baseline.

## Validation

```bash
./gradlew test --tests '*NextModuleBoundaryTest'
./gradlew check -x test
```
