# Engine / AST dependency boundary

The query execution engine is independent of parser and SPARQL AST types.
`NextModuleBoundaryTest` enforces this invariant with zero exceptions across all
engine sources.

## Decoupling history

Previously, three source files in `next/query/impl/engine` retained AST dependencies:

- `model/Filter.java`: returned `TermAst` via `getFilterExpression()` and `coreseNextSource()`.
  These AST accessors were removed from the engine interface; the bridge implementation
  `NextFilterFromAst` retains AST-level access internally where needed.
- `pattern/Exp.java`: exposed `getFilterExpression()` delegating to `Filter`. Removed as it
  had no engine callers.
- `pattern/Query.java`: retained `QueryAst ast` with `getAST()`, `getGlobalAST()`, and `setAST()`.
  Removed from `Query`; query builder compilation produces pure engine operator trees.

## Guard behavior

- Zero exceptions: `EXISTING_ENGINE_AST_DEPENDENCIES` is empty.
- Parser dependencies have no exceptions.
- Both the actual `query.impl.sparql.ast` package and the historical
  `query.impl.ast` package are checked.
- Imports, wildcard imports, static references and fully qualified references
  are detected by the source-reference scanner.
- Any new dependency on parser or AST packages within `query/impl/engine` causes
  `NextModuleBoundaryTest.queryEngineMustNotAddParserOrAstDependencies` to fail immediately.

## Validation

```bash
./gradlew test --tests '*NextModuleBoundaryTest'
./gradlew check -x test
```
