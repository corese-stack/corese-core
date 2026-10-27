# Engine / AST dependency boundary

The query execution engine is independent of parser and SPARQL AST types.
`NextModuleBoundaryTest` enforces this invariant with zero exceptions across all
engine sources.

## Decoupling history

Previously, three source files in `next/query/impl/engine` retained AST dependencies:

- `model/Filter.java`: returned `TermAst` via `getFilterExpression()` and `coreseNextSource()`.
  These AST accessors were removed from the engine interface; the bridge implementation
  `AstBackedFilter` retains AST-level access internally where needed.
- `pattern/Exp.java`: exposed `getFilterExpression()` delegating to `Filter`. Removed as it
  had no engine callers.
- `pattern/QueryPlan.java` (formerly `Query.java`): retained `QueryAst ast` with `getAST()`, `getGlobalAST()`, and `setAST()`.
  Removed from `QueryPlan`; query builder compilation produces pure engine operator trees.

## Guard behavior

- The engine dependency set must be empty; no allowlist or exception mechanism exists.
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

## Syntax-tree ownership

AST traversal contracts (`AstVisitor`, `AbstractAstVisitor`, and `VisitableAst`)
reside in `sparql.ast.visitor`. Semantic validation depends on the syntax tree;
the syntax tree cannot depend on its parser or the execution engine. The same
boundary test protects this direction, including fully qualified references.

## Independent verification

`./gradlew checkNext --offline` compiles, documents, packages, and tests native
sources with their own dependency graph. Historical Corese classes, the JavaCC
parser, and historical runtime libraries are absent from the native classpaths.
`compileNextJava` treats every Java 21 compiler warning as an error.
