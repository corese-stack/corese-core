# Finition et validation du stockage mémoire — 14 octobre 2026

Les points signalés dans les deux dernières reviews sont corrigés. Aucun commit
ni push n'a été effectué. Les modifications préexistantes du nettoyage sont
conservées.

## Timeout et transactions

`SparqlQueryExecutor` exécute toujours l'évaluation sur le thread qui possède
la transaction. Le watchdog utilise désormais un seul `AtomicInteger` : le premier
CAS départage fin d'évaluation et expiration, sans intervalle entre la revendication
d'un timeout et sa publication. Un résultat ne peut pas être accepté après que le
watchdog a gagné cette transition.

L'échéance utilise un temps monotone mesuré à l'entrée, incluant le délai de
démarrage du watchdog. La fin d'évaluation vérifie aussi le budget, même si le
watchdog n'a pas encore été planifié. Après expiration, l'appelant attend la fin du
watchdog et de sa fermeture des ressources avant de consommer l'interruption du
timeout ; celui-ci ne peut donc plus interrompre une requête suivante.

Deux tests couvrent l'expiration dans une transaction, sa réutilisation et son
commit, ainsi que la conservation d'une interruption externe déjà présente.
Le test existant couvre le succès de SELECT et ASK avec timeout dans une transaction.

Les suppressions Sonar sont localisées et commentées : `S2142` pour consommer le
signal du watchdog après son arrêt, et `S9391` pour conserver la liste mutable du
dataset sans ajouter de pipeline stream à sa préparation.

## Sondes et documentation

Le runner actif `corese-storage-benchmarks/benchmarks/profile-memory.py` compile
les deux sondes depuis leur nouveau dépôt, avec le classpath du checkout Corese
sélectionné. Il ne cherche plus ces sources dans `corese-core/src/test`.
Il enregistre les commandes et hashes des sources des deux dépôts et utilise Java 21,
G1 et un heap de 2 Go pour le protocole de référence. Le backend actif est `memory` ;
les anciennes campagnes et leurs runtimes restent des archives.

La documentation corrige les liens et packages déplacés, le moniteur partagé de
`Database`, le registre `State.graphs`, le seuil de huit entrées de `CompactTermMap`
et les limites de l'isolation des valeurs temporelles. Les mesures et résultats
de qualité historiques sont distingués de la validation du checkout actuel.
Le README W3C indique que les sélections autres que `memory` sont rejetées.

## Résultats

| Vérification | Résultat |
| --- | --- |
| Corese, `test storageBenchmarkClasspath --offline`, Java 21 | **3 155 tests**, zéro échec, erreur ou test ignoré |
| W3C, backend `memory` | **2 915 cas**, aucun changement de statut ni d'outcome |
| JUnit du harness W3C | 3 053 tests, les cinq échecs préexistants et treize tests ignorés attendus, zéro erreur |
| `validateEarlReport` | Parsing de 37 551 statements, quinze contrôles SPARQL et cohérence JSON/EARL validés |
| `enforceW3cRegressions` | Zéro régression contre la baseline officielle |
| Sonar way local, exécuteur et tests modifiés | 597 règles, deux fichiers, zéro anomalie et zéro échec d'analyse |
| Compilation stricte de l'exécuteur | `-Xlint:all -Werror`, zéro avertissement |
| Mémoire accessible inclusive, 600k, unitaire | **90 827 088 octets** |
| Mémoire accessible inclusive, 600k, batch | **90 827 088 octets** |
| Contrôle du mode heap libéré | Exécution validée sur 120 statements et trois snapshots ; ce contrôle n'est pas une mesure de performance |

La valeur mémoire est exactement celle de la review précédente, après renommage
du backend en `memory`. Elle est inférieure de huit octets à l'ancienne référence
`indexed-memory`. Aucun fichier de production du stockage natif n'est modifié par
cette passe ; ses chemins d'ingestion et de `find()` conservent leurs sources.
Il n'a pas été lancé de nouvelle campagne chronométrée d'allocations ou de latence.

Les résultats bruts, commandes, rapports Sonar et audits sont conservés dans
`build/cleanup-final/`. La dernière modification de JavaDoc et d'annotation de
l'exécuteur a été compilée strictement et comparée aux instructions testées :
elles sont identiques. L'analyse Sonar locale ne préjuge pas d'un profil upstream
personnalisé.
