# Workflow de contribution

## Branches

- `main` — toujours buildable, ne reçoit que des merges de branches `lot/*` validées.
- `lot/lN-nom-court` — une branche par lot de `SPECIFICATIONS.md §11` (ex. `lot/l1-authentification-autorisation`).
  Créée depuis `main`, fusionnée dans `main` avec `git merge --no-ff` une fois le lot validé.

## Commits

Format [Conventional Commits](https://www.conventionalcommits.org/), imposé par commitlint (`commit-msg` hook) :

```
<type>(<scope>): <résumé>

<corps optionnel — le pourquoi, les exigences couvertes (RG-xx/US-xx/SEC-xx/ECO-xx)>
```

Types acceptés : `feat`, `fix`, `test`, `docs`, `build`, `ci`, `chore`, `refactor`, `perf`, `style`.
Le scope est libre ; `l0`..`l6` pour rattacher un commit à un lot est la convention utilisée jusqu'ici.

## Hooks Git (Husky)

Installés automatiquement par `npm install` (script `prepare`).

| Hook | Quand | Ce qu'il vérifie |
|---|---|---|
| `pre-commit` | à chaque commit touchant `.java`/`.xml`/`.yml`/`.sql` | Spotless (auto-format + restage), Checkstyle, tests **unitaires** (`*Test.java`, rapide) |
| `commit-msg` | à chaque commit | format Conventional Commits (commitlint) |
| `pre-push` | avant `git push` | `mvn verify` complet : tests unitaires **et** d'intégration (Testcontainers), Checkstyle, Spotless, couverture |

`scripts/run-maven.sh` utilise un JDK 25 local s'il est sur le `PATH`, sinon bascule automatiquement
sur `docker run maven:3.9-eclipse-temurin-25` — les hooks fonctionnent donc à l'identique sans JDK 25
installé, à condition que Docker tourne.

## Avant de proposer un lot terminé

1. `mvn verify` passe (le hook `pre-push` le garantit déjà).
2. Vérification manuelle en conditions réelles : `docker compose up --build`, test du parcours concerné
   dans le navigateur — un test vert ne prouve pas qu'un écran fonctionne.
3. Toute exigence `SEC-`/`ECO-` touchée par le lot a son test de non-régression nommé (`QUA-02`).
