# ECO-17 — Mesure avant / après

Livrable obligatoire du §7.2 de la spécification.

## Comment ces chiffres ont été obtenus

**Après** : mesures relevées le 29/07/2026 sur l'application réellement démarrée
(`docker compose up`, profil `dev`, PostgreSQL 17), avec une session authentifiée et un jeu de
**25 fiches** — les écrans de liste affichent donc une page pleine (20 lignes, ECO-11). Les poids
viennent de `curl` (`%{size_download}`), les temps de `%{time_total}` sur **100 requêtes par
écran**, la taille d'image de `docker image ls`.

**Avant** : chiffres du legacy tels que relevés dans l'audit technique et repris au §7.2 de la
spécification. Ils n'ont pas été re-mesurés ici : l'application legacy n'a pas été redémarrée pour
cet exercice, et la source d'origine fait foi.

Les mesures « après » sont donc de première main ; les mesures « avant » sont citées.

## Poids des pages

Poids complet d'une première visite = HTML + feuille de style + logo WebP.
En visite suivante, seules les pages repassent sur le réseau : les ressources statiques portent une
empreinte dans leur nom et un cache d'un an (ECO-08).

| Écran | HTML brut | HTML gzip | 1ʳᵉ visite (gzip, tout compris) | Visite suivante (gzip) |
|---|---:|---:|---:|---:|
| Accueil | 1 786 o | 955 o | **6,8 Ko** | **0,9 Ko** |
| Liste des fiches (20 lignes) | 13 952 o | 1 674 o | **7,5 Ko** | **1,6 Ko** |
| File de contrôle (20 lignes) | 9 321 o | 1 358 o | **7,2 Ko** | **1,3 Ko** |
| Recherche (résultats) | 7 886 o | 1 448 o | **7,3 Ko** | **1,4 Ko** |
| Journal d'audit | 2 006 o | 983 o | **6,8 Ko** | **1,0 Ko** |

Ressources statiques : `app.css` 4 087 o (1 371 o gzip), `datacom.webp` 4 636 o,
`datacom.png` 9 647 o (repli, téléchargé uniquement par un navigateur sans WebP).

| Budget | Cible | Mesuré (pire écran) | Marge |
|---|---:|---:|---|
| ECO-01 — 1ʳᵉ visite | ≤ 300 Ko | 7,5 Ko | **40× sous le budget** |
| ECO-02 — visite suivante | ≤ 60 Ko | 1,6 Ko | **37× sous le budget** |
| ECO-04 — total images | ≤ 50 Ko | 14,3 Ko (WebP + repli PNG) | **3,5× sous le budget** |

### Comparaison au legacy

| | Avant (legacy) | Après | Rapport |
|---|---:|---:|---:|
| Accueil | 8,3 Mo | 6,8 Ko | **≈ 1 250× plus léger** |
| Page interne | 2,0 Mo | 7,5 Ko | **≈ 270× plus léger** |

L'écart ne vient pas d'une optimisation fine mais de trois décisions structurelles : aucune image
décorative lourde, aucun JavaScript, et une liste bornée à 20 lignes au lieu d'un rendu complet de
la table.

## Requêtes SQL par écran

Mesuré par `SqlBudgetIT`, qui compte les instructions réellement exécutées (statistiques Hibernate)
**à deux volumes différents** et échoue si le compte dépasse 3 ou s'il varie avec le volume.

| | Avant (legacy) | Après |
|---|---|---|
| Liste des fiches | Liste non bornée, rendu de toute la table | **2** (une projection paginée + un comptage), constant |
| File de contrôle | — | **≤ 3**, constant — l'auteur est joint dans la même requête |
| Accueil | — | **≤ 3**, constant — deux `count`, jamais un chargement de liste |

Le point vérifié n'est pas seulement le nombre, mais son **indépendance au volume** : un « N+1 »
tiendrait le budget sur un jeu de données minuscule et exploserait en production.

## Temps de réponse

100 requêtes par écran, application et base sur la même machine, données chaudes.

| Écran | Médiane | **P95** | Max |
|---|---:|---:|---:|
| Accueil | 14 ms | **18 ms** | 33 ms |
| Liste des fiches | 20 ms | **28 ms** | 36 ms |
| File de contrôle | 17 ms | **22 ms** | 31 ms |
| Recherche | 16 ms | **22 ms** | 33 ms |

| Cible | Seuil | Mesuré | Verdict |
|---|---:|---:|---|
| PERF-01 — P95 consultation | < 300 ms | 28 ms | **atteint, 10× sous le seuil** |
| PERF-05 — démarrage | < 30 s | 10,2 s | **atteint** |

**Limite assumée** : ces chiffres sont relevés en local, sans concurrence et sur 25 fiches. Ils ne
valent donc pas pour PERF-03 (10 000 fiches) ni PERF-04 (50 utilisateurs simultanés), qui
demanderaient une campagne de charge dédiée. Ce qu'ils établissent, c'est l'ordre de grandeur et
l'absence de dérive manifeste.

## Connexions à la base

| | Avant (legacy) | Après |
|---|---|---|
| Gestion | Une connexion ouverte par requête | Pool HikariCP, 5 connexions en `dev` |
| Observé | — | **6 connexions** au total sur `pg_stat_activity`, stable sous charge |

Le nombre de connexions ne suit plus le trafic : c'est l'objet d'ECO-10.

## Image conteneur

| | Valeur |
|---|---:|
| Base | `eclipse-temurin:25-jre-alpine` — JRE seul, sans outillage de compilation (ECO-14) |
| Taille | **538 Mo** |

**Point d'amélioration identifié, non traité** : 538 Mo reste élevé pour une application de cette
taille. Un `jlink` produisant un runtime réduit aux modules réellement utilisés ramènerait l'image
à quelques dizaines de mégaoctets. Ce n'est pas fait ici : le gain porte sur le stockage et le
temps de déploiement, pas sur la consommation à l'exécution, et l'exigence ECO-14 (« image basée
sur un JRE, sans outillage de développement ») est déjà satisfaite. C'est une évolution candidate
plutôt qu'un manque.

## Synthèse

| Exigence | Cible | Mesuré | Statut |
|---|---|---|---|
| ECO-01 | ≤ 300 Ko | 7,5 Ko | ✅ |
| ECO-02 | ≤ 60 Ko | 1,6 Ko | ✅ |
| ECO-03 | ≤ 3 requêtes / écran | 2, constant | ✅ |
| ECO-04 | ≤ 50 Ko d'images | 14,3 Ko | ✅ |
| ECO-10 | Pool de connexions | 6 connexions stables | ✅ |
| ECO-11 | 20 par page | 20 | ✅ |
| PERF-01 | P95 < 300 ms | 28 ms | ✅ |
| PERF-05 | Démarrage < 30 s | 10,2 s | ✅ |
| PERF-03 / PERF-04 | 10 000 fiches / 50 utilisateurs | non mesuré | ⚠️ campagne de charge à mener |

Les budgets ECO-01 à ECO-04 ne sont pas seulement constatés ici : ils sont **vérifiés par des tests**
(`PageWeightIT`, `SqlBudgetIT`) qui font échouer la construction en cas de dépassement, comme le
demande le §7.2.
