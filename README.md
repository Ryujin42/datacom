# DataCom

Sam Leclercq — Hector Roussel

Réécriture sécurisée de l'application DataCom : enregistrement des fiches produits, contrôle
de conformité, puis autorisation de mise en circulation sur le marché.

## Sommaire

- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Lancer l'application](#lancer-lapplication)
- [Lancer les tests](#lancer-les-tests)
- [Contribution](#contribution)
- [Structure du projet](#structure-du-projet)
- [État d'avancement](#état-davancement)
- [Liens utiles](#liens-utiles)

## Stack technique

Java 25 (LTS) · Spring Boot 4.1 (Web, Thymeleaf, Security, Validation, Data JPA) · PostgreSQL 17
· Flyway · Log4j2 · Lombok · JUnit 5 / Testcontainers · ArchUnit · Checkstyle / Spotless / JaCoCo
· Husky / commitlint · Docker · GitHub Actions.

## Prérequis

- Docker et Docker Compose (suffisent pour lancer l'application).
- Pour le développement hors conteneur : JDK 25, Maven, Node.js.

## Installation

```bash
git clone https://github.com/Ryujin42/datacom.git
cd datacom
npm install
```

`npm install` n'installe aucune dépendance applicative ; elle active les hooks Git (Husky,
commitlint) définis dans `.husky/`.

## Lancer l'application

```bash
docker compose up --build
```

- Application disponible sur <http://localhost:8080>.
- Point de santé : <http://localhost:8080/actuator/health>.
- La base PostgreSQL n'expose aucun port sur l'hôte ; elle n'est accessible que depuis le
réseau Docker interne.

Variables d'environnement (optionnelles, valeurs par défaut entre parenthèses) :
`SPRING_PROFILES_ACTIVE` (`dev`), `DB_NAME` (`datacom`), `DB_USER` (`datacom_app`),
`DB_PASSWORD` (`datacom_app`), `APP_PORT` (`8080`).

Comptes de démonstration (profil `dev` uniquement) :

| Rôle | Identifiant | Mot de passe |
| --- | --- | --- |
| Opérateur de saisie | `operator1` | `OperatorPass123!` |
| Opérateur de saisie (2ᵉ compte) | `operator2` | `OperatorPass123!` |
| Responsable conformité | `validator1` | `ValidatorPass123!` |

### Sans Docker

Nécessite un JDK 25, Maven, et une instance PostgreSQL 17 accessible (par exemple
`docker compose up postgres`). Sans JDK 25 en local, `docker compose up --build` reste la
méthode la plus fiable pour tester les changements ; `scripts/run-maven.sh` bascule
automatiquement sur un conteneur Maven si aucun JDK 25 n'est présent sur le `PATH`.

## Lancer les tests

```bash
mvn verify
```

Exécute l'ensemble de la chaîne de vérification : tests unitaires, tests d'intégration
(Testcontainers, nécessite Docker), Checkstyle, Spotless, couverture JaCoCo et tests
d'architecture ArchUnit.

```bash
mvn test
```

N'exécute que les tests unitaires (pas de Docker requis) — utile pour une boucle rapide en
développement.

Suite actuelle : 55 tests unitaires, 72 tests d'intégration, tous verts, seuil de couverture
JaCoCo atteint.

## Contribution

Le workflow Git (branches par lot, Conventional Commits, hooks Husky, checklist avant Pull
Request) est détaillé dans [`CONTRIBUTING.md`](./CONTRIBUTING.md).

## Structure du projet

Architecture en couches, imposée par des tests ArchUnit :

```
com.datacom.<module>.domain            règles métier
com.datacom.<module>.application       cas d'usage
com.datacom.<module>.infrastructure    persistance, adaptateurs
com.datacom.<module>.web               contrôleurs, vues
```

## État d'avancement

Les 7 lots (`SPECIFICATIONS.md §11`) sont terminés et mergés dans `main`, chacun via une Pull
Request :

- [x] L0 — Socle technique
- [x] L1 — Authentification & autorisation
- [x] L2 — Domaine & workflow
- [x] L3 — Saisie des fiches
- [x] L4 — Contrôle de conformité
- [x] L5 — Consultation & recherche
- [x] L6 — Écoconception & finalisation

## Liens utiles

- [`CONTRIBUTING.md`](./CONTRIBUTING.md) — workflow de contribution
- [`docs/ECO-17-mesures.md`](./docs/ECO-17-mesures.md) — mesures écoconception avant/après
- Dépôt du séminaire, dossier `Datacom/` — `AUDIT.md` et `SPECIFICATIONS.md`

## Licence

MIT — voir [`LICENSE`](./LICENSE).
