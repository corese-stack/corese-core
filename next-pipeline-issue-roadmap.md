# Roadmap des futures PR Corese-next

Ce document sert de carte de travail pour organiser les prochaines PR autour de
la pipeline Corese-next.

Il relie trois sources :

- les PR et issues ouvertes dans `corese-stack/corese-core` ;
- le travail deja fait dans la branche `feature/472-bridge-storage-manager-producer` ;
- les limites visibles dans le code `next` actuel.

## 1. Contexte actuel

### Branche d'integration

La branche de travail / integration du chantier est :

```text
feature/corese-next
```

Les PR de feature doivent donc viser `feature/corese-next`, sauf quand une PR
est volontairement empilee sur une autre PR en cours.

### PR actuellement ouvertes

| PR | Branche | Base | Role |
| --- | --- | --- | --- |
| [#482](https://github.com/corese-stack/corese-core/pull/482) | `feature/472-bridge-storage-manager-producer` | `feature/corese-next` | Connecter KGRAM next au nouveau `StorageManager` pour SPO/BGP (Fusionnée / Merged) |
| [#484](https://github.com/corese-stack/corese-core/pull/484) | `feature/473-autonomous-next-query-pipeline` | `feature/corese-next` | Premiere pipeline autonome next, réalignée sur `feature/corese-next` après merge de #482 (draft/review pédagogique en cours) |
| [#483](https://github.com/corese-stack/corese-core/pull/483) | `feature/470-bridge-add-minusoptional-conversion` | `feature/corese-next` | Conversion bridge de `MINUS` / `OPTIONAL` (Fusionnée / Merged) |
| [#463](https://github.com/corese-stack/corese-core/pull/463) | `feature/335-property-path-parser-and-ast` | `feature/corese-next` | Parser/AST des property paths (En cours de review / merge par Abdessamad, non bloquante pour le rebase de #484) |

### Issues ouvertes liees a la roadmap

| Issue | Sujet | Role dans cette roadmap |
| --- | --- | --- |
| [#472](https://github.com/corese-stack/corese-core/issues/472) | Producer KGRAM -> StorageManager | Couverte par #482, correspond a `A1` |
| [#473](https://github.com/corese-stack/corese-core/issues/473) | Execution autonome des types de requetes via next | Couverte par #484, correspond a `B1`, `B2`, debut `B3`, `B5`, `E1`, `F2` |
| [#470](https://github.com/corese-stack/corese-core/issues/470) | Conversion `MINUS` / `OPTIONAL` | Couverte par #483, prepare `C2` et `C4` |
| [#453](https://github.com/corese-stack/corese-core/issues/453) | `EXISTS` / `NOT EXISTS` dans les filtres | Liee a `C1` |
| [#474](https://github.com/corese-stack/corese-core/issues/474) | Mecanisme d'appel de fonctions | Liee a `C1`, `C5`, `D1` |
| [#335](https://github.com/corese-stack/corese-core/issues/335) | Parser/AST des property paths | Couverte par #463 cote parser/AST, prepare `C8` |
| [#328](https://github.com/corese-stack/corese-core/issues/328) | Parser/AST `INSERT` / `DELETE` | Liee a `E1` et `E4` |
| [#373](https://github.com/corese-stack/corese-core/issues/373) | Parser/AST `DROP` | Liee a `E2` |
| [#374](https://github.com/corese-stack/corese-core/issues/374) | Parser/AST `COPY` | Liee a `E3` |
| [#375](https://github.com/corese-stack/corese-core/issues/375) | Parser/AST `MOVE` | Liee a `E3` |
| [#376](https://github.com/corese-stack/corese-core/issues/376) | Parser/AST `ADD` | Liee a `E3` |

### Travail deja fait dans #482

La branche courante contient notamment :

- `feat(next): connect KGRAM producer to StorageManager`
- `fix(next): restore KGRAM fail semantics`
- `fix(next): honor named dataset for graph patterns`
- `refactor(next): centralize storage KGRAM value conversion`
- `test(next): cover storage producer edge cases`
- `test(next): avoid order-dependent storage assertion`

Ces commits apportent :

- un `StorageManagerProducer` pour interroger le nouveau storage depuis KGRAM ;
- un `StorageManagerEdge` pour exposer les `Statement` storage comme `Edge`
  KGRAM next ;
- un helper `StorageManagerKgramValues` pour centraliser les conversions entre
  `Value`, `IDatatype` et `Node` ;
- le support SPO/BGP simple, y compris les bindings deja presents ;
- le respect de `GRAPH <g>` et des restrictions `FROM` / dataset nomme ;
- un fail-fast explicite pour paths/regex non supportes ;
- des tests sur SPO, BGP, graphes, dataset, conversions et cas RDF impossibles.

### Limites visibles dans le code

Le code actuel montre encore plusieurs zones explicitement non supportees :

- paths/regex dans `StorageManagerProducer#getEdges(... Regex ...)` ;
- `VALUES`, `SELECT` expressions, `GROUP BY`, `HAVING`, `REDUCED` dans
  `CoreseAstQueryBuilder` ;
- execution runtime complete de `FILTER`, `OPTIONAL`, `UNION`, `BIND`,
  `SERVICE`, `MINUS` selon le niveau attendu ;
- updates plus larges que le support minimal ;
- hooks `Producer` encore a clarifier : `start`, `finish`, `getGraph`,
  conversions, `isBindable` ;
- dependances legacy encore presentes : `IDatatype`, legacy `Binding`,
  legacy-backed values.

## 2. Plan operationnel dans l'ordre

Cette section donne l'ordre concret des actions a executer. Les epics detailles
sont plus bas.

### Phase 1 - Terminer #482

- [x] Attendre que les checks de #482 soient verts.
- [x] Verifier que le commentaire Copilot sur l'assertion dependante de l'ordre est bien corrige par le commit `0cffbf617`.
- [x] Resoudre manuellement la conversation Copilot si GitHub ne le fait pas tout seul.
- [x] Demander ou reprendre la review humaine finale sur #482.
- [x] Merger #482 dans `feature/corese-next`.

### Phase 2 - Realigner les PR existantes

- [x] Mettre a jour localement `feature/corese-next`.
- [x] Rebaser #483 sur `feature/corese-next` si necessaire.
- [x] Rebaser #484 sur `feature/corese-next`, car elle etait basee
  sur la branche #482.
- [x] Rebaser #463 sur `feature/corese-next` si necessaire.

### Phase 3 - Traiter #483

- [x] Verifier #483 : conversion `MINUS` / `OPTIONAL` (PR approuvée).
- [x] Relancer les tests bridge/parser concernes.
- [x] Verifier que #483 compile l'AST vers KGRAM sans promettre encore
  l'execution runtime complete.
- [x] Merger #483 dans `feature/corese-next`.

### Phase 4 - Reprendre #484

- [x] Relancer les tests de #484 apres rebase.
- [x] Relire le diff de #484 apres disparition du diff de #482.
- [x] Nettoyer les erreurs unsupported et les placeholders locaux au scope #484
      apres rebase.
- [ ] Decider si #484 reste une seule PR ou si elle doit etre decoupee.
- [ ] Si #484 reste une seule PR, garder le scope "premiere pipeline autonome
  next" et clarifier dans la PR que ce n'est pas encore l'API publique finale.
- [ ] Si #484 est trop grosse, la decouper en PR plus petites :
  - configuration Gradle/ANTLR ;
  - `UnsupportedQueryFeatureException` ;
  - `INSERT DATA` / `DELETE DATA` ;
  - `CONSTRUCT` / `DESCRIBE` ;
  - `NextSparqlPipelineExecutor`.
- [ ] Faire la review finale de #484.
- [ ] Merger #484 dans `feature/corese-next`.

### Phase 5 - Traiter #463

- [x] Verifier #463 : parser/AST des property paths (PR approuvée).
- [x] S'assurer que la PR ne promet que parser/AST, pas runtime.
- [x] Ajouter ou verifier les tests parser/AST.
- [ ] Merger #463 dans `feature/corese-next` (Demandé à Abdessamad).

### Phase 6 - Creer les nouvelles issues structurantes

- [ ] Creer l'issue `[NEXT][KGRAM] Clarifier le comportement des hooks Producer`.
- [ ] Creer l'issue `[NEXT][KGRAM] Supporter les graphes variables`.
- [ ] Creer l'issue `[NEXT][SPARQL API] Definir l'API haut niveau`.
- [ ] Creer l'issue `[NEXT][RUNTIME] Decouper l'executor temporaire`.
- [ ] Creer l'issue `[NEXT][TESTS] Creer une suite end-to-end SPARQL next`.

### Phase 7 - Etendre le runtime SPARQL

- [ ] Faire une PR `GRAPH ?g` / graphes variables.
- [ ] Faire une PR `FILTER` end to end.
- [ ] Faire une PR `OPTIONAL` runtime end to end.
- [ ] Faire une PR `MINUS` runtime end to end.
- [ ] Faire une PR `UNION` runtime end to end.
- [ ] Faire une PR `BIND` runtime end to end.
- [ ] Faire une PR `VALUES` runtime end to end.
- [ ] Faire une PR `ORDER BY / LIMIT / OFFSET` end to end.
- [ ] Faire une PR `CONSTRUCT` avance avec blank nodes.
- [ ] Faire une PR `DESCRIBE` stabilise.
- [ ] Faire une PR `property paths` runtime.

### Phase 8 - SPARQL Update

- [ ] Stabiliser `INSERT DATA` / `DELETE DATA`.
- [ ] Ajouter `CLEAR`, `CREATE`, `DROP`, `LOAD`.
- [ ] Ajouter `COPY`, `MOVE`, `ADD`.
- [ ] Ajouter `DELETE/INSERT WHERE`.
- [ ] Ajouter une strategie transactionnelle si necessaire.

### Phase 9 - Dette technique

- [ ] Remplacer progressivement les RDF values legacy-backed.
- [ ] Reduire la dependance a `IDatatype` dans KGRAM next.
- [ ] Nettoyer les placeholders `return null` / `Not supported yet` restants
      hors scope #484, notamment dans la dette KGRAM-next historique.
- [ ] Isoler `GraphStorageManager` comme backend de compatibilite legacy.
- [ ] Corriger l'erreur JaCoCo / Java major version 70 en CI.

### Resume court

```text
1. Finaliser #482
2. Merger #482 dans feature/corese-next
3. Rebaser #484 sur feature/corese-next
4. Reprendre #483 et #463
5. Decider si #484 reste entiere ou est decoupee
6. Creer les nouvelles issues runtime/API/tests
7. Avancer feature par feature
```

## 3. Strategie pour #484

La PR [#484](https://github.com/corese-stack/corese-core/pull/484) contient deja
beaucoup du travail de l'issue [#473](https://github.com/corese-stack/corese-core/issues/473).
Elle ne doit pas etre jetee : elle doit etre traitee comme une PR empilee.

Contenu actuel repere :

- configuration Gradle/IDE des sources ANTLR generees ;
- `UnsupportedQueryFeatureException` ;
- AST/parser pour `INSERT DATA` et `DELETE DATA` ;
- support `CONSTRUCT` dans `CoreseAstQueryBuilder` ;
- `NextSparqlPipelineExecutor` ;
- tests end-to-end SELECT, ASK, CONSTRUCT, DESCRIBE, INSERT DATA, DELETE DATA.

Plan conseille :

1. Garder #484 en draft tant que le scope n'est pas stabilise.
2. Merger #482 dans `feature/corese-next`. (fait)
3. Rebaser #484 sur `feature/corese-next`. (fait)
4. Relancer les tests end-to-end. (fait localement)
5. Decider si #484 reste une seule PR de demonstration autonome ou si elle est
   decoupee avant revue finale.

Decoupage possible si #484 est jugee trop large :

- PR courte 1 : configuration Gradle/IDE des sources ANTLR generees.
- PR courte 2 : `UnsupportedQueryFeatureException` et erreurs unsupported propres.
- PR courte 3 : `INSERT DATA` / `DELETE DATA` parser + AST + execution minimale.
- PR courte 4 : `CONSTRUCT` / `DESCRIBE` end-to-end minimal.
- PR courte 5 : `NextSparqlPipelineExecutor` comme executor interne temporaire.

Decision recommandee :
Garder #484 comme PR stacked pour l'instant, puis reevaluer apres rebase sur
`feature/corese-next`. Si le diff reste lisible et coherent avec #473, on peut
la garder. Si la revue devient difficile, on splitte selon les blocs ci-dessus.

## 4. Backlog organise par epics

### Epic A - Stabiliser la pipeline StorageManager/KGRAM

#### A1. `[NEXT][KGRAM] Finaliser le producer StorageManager SPO/BGP`

References :

- Issue existante : [#472](https://github.com/corese-stack/corese-core/issues/472)
- PR existante : [#482](https://github.com/corese-stack/corese-core/pull/482)

Description courte :
Stabiliser la PR actuelle : KGRAM peut lire des triples depuis `StorageManager`
et produire des `Mappings` pour des BGP simples.

Sous-issues :

- Valider les commentaires de review.
- Garder les tests non dependants de l'ordre des resultats storage.
- Documenter clairement les limites actuelles : BGP uniquement, pas paths,
  pas service, pas values.

Critere de sortie :
La PR #482 est mergee et les tests `StorageManagerProducerTest` /
`StorageManagerEdgeTest` passent en CI.

#### A2. `[NEXT][KGRAM] Clarifier le comportement des hooks Producer`

Description courte :
Faire une passe dediee sur les methodes heritees de `Producer` qui retournent
encore `null` ou des implementations minimales.

Sous-issues :

- Revoir `start(Query)` et `finish(Query)` : confirmer qu'ils ne portent pas
  encore de transaction.
- Revoir `getGraph()` : decider s'il doit rester `null`, exposer une facade,
  ou etre explicitement unsupported.
- Revoir `getNode`, `getValue`, `getDatatypeValue`, `isBindable`.
- Ajouter des tests quand un comportement est attendu par KGRAM.

Pourquoi :
Ces methodes viennent en partie de l'ancien contrat Producer. Elles ne bloquent
pas le SPO/BGP, mais elles doivent etre explicites avant d'elargir la pipeline.

#### A3. `[NEXT][KGRAM] Supporter les graphes variables`

Description courte :
Stabiliser `GRAPH ?g { ... }` de bout en bout avec `getGraphNodes(...)`.

Sous-issues :

- Tester `GRAPH ?g { ?s ?p ?o }`.
- Verifier l'interaction avec `FROM NAMED`.
- Verifier l'interaction avec les graphes par defaut.
- Confirmer le comportement avec graphes nommes IRI et blank nodes selon le
  modele RDF/SPARQL supporte.

Critere de sortie :
Les graphes variables retournent des mappings corrects et respectent le dataset
actif.

### Epic B - Executer les formes de requetes SPARQL end to end

#### B1. `[NEXT][QUERY] Executer SELECT via parser -> AST -> KGRAM -> StorageManager`

References :

- Issue existante : [#473](https://github.com/corese-stack/corese-core/issues/473)
- PR existante : [#484](https://github.com/corese-stack/corese-core/pull/484)

Description courte :
Faire de `SELECT` la premiere forme de requete end-to-end stable.

Sous-issues :

- `SELECT * WHERE { ?s ?p ?o }`
- projection explicite simple : `SELECT ?s ?o`
- projection avec variables absentes : erreur claire
- `DISTINCT`
- `ORDER BY`, `LIMIT`, `OFFSET`
- dataset `FROM` / `FROM NAMED`

Critere de sortie :
Une API interne peut executer SELECT sans fallback `QueryProcess`.

#### B2. `[NEXT][QUERY] Executer ASK end to end`

References :

- Issue existante : [#473](https://github.com/corese-stack/corese-core/issues/473)
- PR existante : [#484](https://github.com/corese-stack/corese-core/pull/484)

Description courte :
Executer les requetes ASK avec la nouvelle pipeline.

Sous-issues :

- ASK vrai/faux sur BGP simple.
- ASK avec dataset.
- ASK avec BGP vide.
- Verifier les clauses actuellement rejetees : values, group by, having,
  distinct/reduced.

Critere de sortie :
ASK retourne un booleen coherent avec les mappings produits par KGRAM.

#### B3. `[NEXT][QUERY] Executer CONSTRUCT end to end`

References :

- Issue existante : [#473](https://github.com/corese-stack/corese-core/issues/473)
- PR existante : [#484](https://github.com/corese-stack/corese-core/pull/484),
  selon son scope final.

Description courte :
Produire un resultat graphe depuis un template CONSTRUCT.

Sous-issues :

- Template triple simple.
- Template avec plusieurs triples.
- Variables non bindees dans le template.
- Graphes nommes si supportes dans la forme actuelle.
- Conversion `Mappings -> Statement`.

Critere de sortie :
CONSTRUCT retourne un `GraphQueryResult`/modele next sans fallback legacy.

#### B4. `[NEXT][CONSTRUCT] Supporter les blank nodes dans les templates`

Description courte :
Respecter la regle SPARQL : les blank nodes du template CONSTRUCT doivent etre
recrees par solution mapping.

Sous-issues :

- Identifier les blank nodes du template.
- Allouer des blank nodes frais par mapping.
- Tester plusieurs solutions qui utilisent le meme blank node de template.

Pourquoi :
Ce point est assez specifique pour meriter une PR separee de CONSTRUCT simple.

#### B5. `[NEXT][QUERY] Definir et stabiliser DESCRIBE`

References :

- Issue existante : [#473](https://github.com/corese-stack/corese-core/issues/473)
- PR existante : [#484](https://github.com/corese-stack/corese-core/pull/484),
  selon son scope final.

Description courte :
Remplacer la strategie minimale actuelle de DESCRIBE par un comportement decide
et teste.

Sous-issues :

- `DESCRIBE <iri>`.
- `DESCRIBE ?s WHERE { ... }`.
- `DESCRIBE *`.
- Triples sortants seulement ou sortants + entrants.
- Comportement avec graphes nommes.

Critere de sortie :
La semantique DESCRIBE est documentee et stable.

### Epic C - Ajouter les patterns SPARQL 1.1 dans le runtime

#### C1. `[NEXT][FILTER] Executer FILTER end to end`

References :

- Issue liee : [#453](https://github.com/corese-stack/corese-core/issues/453)
  pour `EXISTS` / `NOT EXISTS` dans les filtres.
- Issue liee : [#474](https://github.com/corese-stack/corese-core/issues/474)
  pour le mecanisme d'appel de fonctions.

Description courte :
Faire fonctionner les filtres compiles par `WhereCompiler` et
`SparqlAstToExpression` dans la pipeline StorageManager.

Sous-issues :

- Comparaisons simples : `=`, `!=`, `<`, `>`.
- `BOUND`, `isIRI`, `isBlank`, `isLiteral`.
- operations booleennes `&&`, `||`, `!`.
- fonctions string simples : `STR`, `LANG`, `DATATYPE`.
- erreurs d'evaluation SPARQL.

Critere de sortie :
Un BGP filtre produit les memes mappings attendus que la semantique SPARQL.

#### C2. `[NEXT][OPTIONAL] Executer OPTIONAL end to end`

References :

- Issue existante : [#470](https://github.com/corese-stack/corese-core/issues/470)
- PR existante : [#483](https://github.com/corese-stack/corese-core/pull/483)
  pour la conversion bridge.

Description courte :
Activer le left join KGRAM pour `OPTIONAL`.

Sous-issues :

- optional simple.
- optional qui matche.
- optional qui ne matche pas.
- optional avec variable deja bindee.
- optional + filter.

#### C3. `[NEXT][UNION] Executer UNION end to end`

Description courte :
Executer les branches `UNION` compilees par `WhereCompiler`.

Sous-issues :

- deux branches simples.
- variables communes.
- variables presentes dans une seule branche.
- interaction avec projection explicite.

#### C4. `[NEXT][MINUS] Executer MINUS end to end`

References :

- Issue existante : [#470](https://github.com/corese-stack/corese-core/issues/470)
- PR existante : [#483](https://github.com/corese-stack/corese-core/pull/483)
  pour la conversion bridge.

Description courte :
Verifier et stabiliser le comportement de `MINUS`.

Sous-issues :

- minus simple.
- variables partagees.
- absence de variables partagees.
- interaction avec graphes nommes.

#### C5. `[NEXT][BIND] Executer BIND end to end`

References :

- Issue liee : [#474](https://github.com/corese-stack/corese-core/issues/474)
  pour le mecanisme d'appel de fonctions.

Description courte :
Produire de nouvelles bindings depuis une expression SPARQL.

Sous-issues :

- expression constante.
- expression dependante d'une variable.
- variable deja bindee : comportement d'erreur.
- interaction avec projection.

#### C6. `[NEXT][VALUES] Executer VALUES end to end`

Description courte :
Remplacer les rejets actuels `VALUES is not supported yet` par une vraie
integration runtime.

Sous-issues :

- values inline dans WHERE.
- values clause hors WHERE.
- `UNDEF`.
- plusieurs lignes de values.
- interaction avec BGP et FILTER.

Pourquoi :
`CoreseAstQueryBuilder` rejette explicitement `VALUES` aujourd'hui.

#### C7. `[NEXT][SERVICE] Definir le support SERVICE`

Description courte :
Decider si `SERVICE` est supporte dans next maintenant, et sous quelle forme.

Sous-issues :

- `SERVICE <endpoint>`.
- `SERVICE SILENT`.
- erreurs reseau.
- streaming ou materialisation.
- politique de securite/configuration.

Critere de sortie :
Soit SERVICE fonctionne, soit l'erreur unsupported est explicite et testee.

#### C8. `[NEXT][PROPERTY PATHS] Supporter les property paths`

References :

- Issue existante : [#335](https://github.com/corese-stack/corese-core/issues/335)
- PR existante : [#463](https://github.com/corese-stack/corese-core/pull/463)
  pour parser/AST.
- Nouvelle issue runtime conseillee : execution KGRAM/storage des property
  paths.

Description courte :
Implementer le chemin `Regex`/path aujourd'hui explicitement unsupported dans
`StorageManagerProducer#getEdges(... Regex ...)`.

Sous-issues :

- paths simples `/`.
- alternatives `|`.
- inverse `^`.
- cardinalites `*`, `+`, `?`.
- detection de cycles.
- tests de non-regression sur le fail-fast actuel.

Pourquoi :
Copilot avait raison sur ce point dans la PR #482 : mieux vaut fail-fast tant
que ce n'est pas implemente. La prochaine etape est de le supporter vraiment.

### Epic D - Solution modifiers et expressions avancees

#### D1. `[NEXT][SELECT] Supporter les expressions de projection`

References :

- Issue liee : [#474](https://github.com/corese-stack/corese-core/issues/474)
  pour le mecanisme d'appel de fonctions.

Description courte :
Supporter `SELECT (expr AS ?x)` aujourd'hui rejete par
`CoreseAstQueryBuilder`.

Sous-issues :

- alias simple.
- reutilisation de variables.
- erreur si alias deja visible.
- interaction avec ORDER BY.

#### D2. `[NEXT][ORDER] Stabiliser ORDER BY, LIMIT, OFFSET`

Description courte :
Verifier les modifiers deja copies dans `Query` et ajouter les tests runtime
end-to-end.

Sous-issues :

- order ascendant.
- order descendant.
- order sur variable non projetee.
- limit seul.
- offset seul.
- limit + offset.

#### D3. `[NEXT][AGGREGATES] Supporter GROUP BY / HAVING / aggregates`

Description courte :
Ajouter les agregats SPARQL dans la pipeline next.

Sous-issues :

- `COUNT`.
- `SUM`, `MIN`, `MAX`, `AVG`.
- `GROUP BY`.
- `HAVING`.
- projection d'agregats.
- erreurs de scope.

Pourquoi :
Le parser et les validations existent deja en partie, mais le bridge/runtime
rejette encore `GROUP BY` et `HAVING`.

#### D4. `[NEXT][REDUCED] Decider la politique REDUCED`

Description courte :
Decider si `REDUCED` est implemente, ignore de facon documentee, ou rejete.

Sous-issues :

- politique compatible SPARQL.
- tests.
- documentation dans l'API.

### Epic E - Updates SPARQL

#### E1. `[NEXT][UPDATE] Stabiliser INSERT DATA / DELETE DATA`

References :

- Issue existante : [#328](https://github.com/corese-stack/corese-core/issues/328)
- PR partiellement liee : [#484](https://github.com/corese-stack/corese-core/pull/484)
  contient deja un debut de support `INSERT DATA` / `DELETE DATA`.

Description courte :
Consolider le support minimal d'updates sur le nouveau storage.

Sous-issues :

- IRI absolus.
- prefixed names.
- `BASE` et IRI relatifs.
- literals types.
- literals avec langue.
- `GRAPH` blocks.
- tests de mutation storage.

#### E2. `[NEXT][UPDATE] Supporter CLEAR / CREATE / DROP / LOAD`

References :

- Issue existante liee a `DROP` :
  [#373](https://github.com/corese-stack/corese-core/issues/373)

Description courte :
Ajouter les operations de gestion de graphes.

Sous-issues :

- `CREATE GRAPH`.
- `DROP GRAPH`.
- `CLEAR GRAPH`.
- `CLEAR DEFAULT`.
- `CLEAR ALL`.
- `LOAD`.

#### E3. `[NEXT][UPDATE] Supporter COPY / MOVE / ADD`

References :

- Issue existante liee a `COPY` :
  [#374](https://github.com/corese-stack/corese-core/issues/374)
- Issue existante liee a `MOVE` :
  [#375](https://github.com/corese-stack/corese-core/issues/375)
- Issue existante liee a `ADD` :
  [#376](https://github.com/corese-stack/corese-core/issues/376)

Description courte :
Ajouter les operations entre graphes.

Sous-issues :

- source/destination graph.
- default graph.
- graph absent.
- operations no-op.

#### E4. `[NEXT][UPDATE] Supporter DELETE/INSERT WHERE`

Description courte :
Executer les updates qui combinent une evaluation WHERE et une mutation.

Sous-issues :

- `DELETE WHERE`.
- `DELETE { ... } INSERT { ... } WHERE { ... }`.
- blank nodes dans templates.
- atomicite.
- interaction avec transactions futures.

### Epic F - API publique et orchestration

#### F1. `[NEXT][SPARQL API] Definir l'API haut niveau`

Description courte :
Definir l'entree utilisateur stable au-dessus du parser, du bridge, de KGRAM et
du storage.

Sous-issues :

- `prepareTupleQuery`.
- `prepareBooleanQuery`.
- `prepareGraphQuery`.
- `prepareUpdate`.
- initial bindings.
- options de requete.
- modele d'erreur.
- lifecycle des resultats.

Pourquoi :
La pipeline interne avance, mais il faut une facade stable pour l'utiliser sans
connaitre les details KGRAM.

#### F2. `[NEXT][RUNTIME] Decouper l'executor temporaire`

References :

- PR existante : [#484](https://github.com/corese-stack/corese-core/pull/484)
  introduit `NextSparqlPipelineExecutor` comme integration executor.

Description courte :
Eviter qu'un futur `NextSparqlPipelineExecutor` concentre toutes les
responsabilites.

Sous-issues :

- `QueryEvaluationService`.
- `GraphResultMaterializer`.
- `DescribeEvaluator`.
- `UpdateExecutionService`.
- `TermValueMapper`.
- `NextRuntimeFactory`.

#### F3. `[NEXT][RESULTS] Stabiliser les resultats Tuple/Boolean/Graph`

Description courte :
Garantir que les resultats exposes par l'API next ne dependent pas des classes
legacy.

Sous-issues :

- tuple bindings.
- graph statements.
- close/lifecycle.
- streaming vs materialisation.
- tests API.

### Epic G - Storage et transactions

#### G1. `[NEXT][STORAGE] Clarifier les capacites de MemoryStorageManager`

Description courte :
Rendre explicites les limites du storage memoire actuel.

Sous-issues :

- transactions non supportees.
- bulk operations non supportees.
- ordre non garanti.
- contexts / named graphs.
- metadata operations.

#### G2. `[NEXT][STORAGE] Ajouter une vraie strategie de transactions`

Description courte :
Preparer les updates atomiques et les futures operations complexes.

Sous-issues :

- interface transaction stable.
- implementation memory.
- comportement rollback.
- interaction avec updates.
- tests d'erreur.

#### G3. `[NEXT][STORAGE] Isoler GraphStorageManager comme backend legacy`

Description courte :
Faire de `GraphStorageManager` une compatibilite explicite, pas le coeur de la
pipeline next.

Sous-issues :

- documenter le role compatibility backend.
- limiter les fuites de types legacy.
- tests comparatifs avec MemoryStorageManager.

### Epic H - Reduction de la dette legacy

#### H1. `[NEXT][DATA] Remplacer les RDF values legacy-backed`

Description courte :
Creer des valeurs RDF next-native au lieu d'adapter constamment les types
legacy.

Sous-issues :

- `NextIRI`.
- `NextBNode`.
- `NextLiteral`.
- `NextStatement`.
- `NextValueFactory`.
- compatibilite temporaire avec `CoreseAdaptedValueFactory`.

#### H2. `[NEXT][KGRAM] Reduire la dependance a IDatatype`

Description courte :
Preparer KGRAM next a ne plus transporter directement `IDatatype` partout.

Sous-issues :

- identifier les points d'entree obligatoires.
- isoler les conversions dans des helpers.
- remplacer progressivement dans `NodeImpl`.
- garder les tests de compatibilite.

#### H3. `[NEXT][KGRAM] Nettoyer les implementations placeholder`

Description courte :
Remplacer les `return null`, `TODO Auto-generated method stub` et
`UnsupportedOperationException("Not supported yet")` generiques par des
comportements documentes.

Sous-issues :

- `ProducerDefault`.
- `EnvironmentImpl`.
- `NodeImpl`.
- classes path/approx/search si encore utiles.

#### H4. `[NEXT][PARSER] Refactorer les grosses methodes d'expressions`

Description courte :
Reduire la complexite dans le parser/bridge d'expressions.

Sous-issues :

- `createConstraint`.
- conversion des builtins.
- termes additifs/multiplicatifs.
- erreurs de type.
- tests de non-regression.

### Epic I - Qualite, tests et validation croisee

#### I1. `[NEXT][TESTS] Creer une suite end-to-end SPARQL next`

Description courte :
Centraliser les tests de la pipeline complete.

Sous-issues :

- helper de dataset.
- helper de requete.
- assertions order-independent.
- SELECT/ASK/CONSTRUCT/DESCRIBE.
- tests de features unsupported.

#### I2. `[NEXT][TESTS] Comparer next avec legacy sur un sous-ensemble`

Description courte :
Pour chaque feature stabilisee, comparer le resultat next avec l'ancien moteur
sur des cas simples.

Sous-issues :

- memes donnees.
- meme requete.
- normalisation des resultats.
- differences documentees.

#### I3. `[NEXT][CI] Corriger l'erreur JaCoCo / Java major version 70`

Description courte :
L'erreur JaCoCo n'empeche pas les tests de passer, mais elle pollue les logs et
doit etre corrigee proprement.

Sous-issues :

- confirmer la version Java utilisee en CI.
- confirmer la version JaCoCo compatible.
- desactiver l'instrumentation des classes JDK si necessaire.
- documenter le comportement attendu.

## 5. Decoupage conseille

### Prochaine vague de PR

1. Finaliser et merger #482.
2. Rebaser #484 sur `feature/corese-next`.
3. Traiter #483.
4. Traiter #463.
5. Creer `A2 - Clarifier les hooks Producer`.
6. Creer `A3 - Supporter GRAPH ?g`.
7. Creer `F1 - API haut niveau`.

### Vague suivante

1. `B1 - SELECT end to end` si #484 est splittee.
2. `B2 - ASK end to end` si #484 est splittee.
3. `C1 - FILTER`.
4. `C2 - OPTIONAL`.
5. `C3 - UNION`.
6. `D2 - ORDER BY / LIMIT / OFFSET`.
7. `B3 - CONSTRUCT`.

### Plus tard

1. `C8 - Property paths`.
2. `D3 - Aggregates`.
3. `E* - Updates`.
4. `H* - Dette legacy`.
