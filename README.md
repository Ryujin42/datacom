# DataCom

Sam LECLERCQ - Hector ROUSSEL

Réécriture sécurisée de l'application DataCom : enregistrement des fiches produits,
contrôle de conformité, puis autorisation de mise en circulation sur le marché.

Le code legacy audité et les spécifications complètes de cette refonte vivent dans le
dépôt du séminaire, dossier `Datacom/` (`AUDIT.md`, `SPECIFICATIONS.md`).

## Démarrer

```bash
docker compose up --build
```

L'application est servie sur <http://localhost:8080>.

## État actuel du projet

Ce dépôt est construit **lot par lot** (voir `SPECIFICATIONS.md §11`). L'authentification,
le workflow métier et les écrans arrivent progressivement ; l'état d'avancement :

- [x] **L0** — Socle technique : squelette Spring Boot, migrations Flyway, conteneurisation,
      intégration continue, pages d'erreur, journalisation, harnais de tests
- [ ] L1 — Authentification & autorisation
- [ ] L2 — Domaine & workflow
- [ ] L3 — Saisie des fiches
- [ ] L4 — Contrôle de conformité
- [ ] L5 — Consultation & recherche
- [ ] L6 — Écoconception & finalisation

## Développement local

- **Java 25**, Maven, Docker sont requis pour un développement hors conteneur.
- Sans JDK 25 en local, utiliser le conteneur comme boucle de vérification :
  `docker compose up --build` reconstruit et relance l'image à chaque changement.
- Tests : `mvn verify` (unitaires + intégration Testcontainers + couverture JaCoCo +
  analyse statique Checkstyle). Nécessite Docker pour les tests d'intégration.
- Point de santé : <http://localhost:8080/actuator/health>

## Stack technique

Java 25 (LTS) · Spring Boot 4.1 · PostgreSQL 17 · Thymeleaf · Flyway · Testcontainers ·
GitHub Actions.
