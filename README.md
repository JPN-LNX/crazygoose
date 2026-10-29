# Crazy Goose

Jeu de l'oie sportif pour Android : toute la famille joue sur un seul téléphone, et chaque case fait bouger les joueurs avec des exercices adaptés au niveau de chacun.

« Crazy Goose » est un nom de travail, en attente de la recherche de marque.

## Organisation du code

| Dossier | Contenu |
| --- | --- |
| `game/` | Moteur du jeu en Kotlin pur : plateau, dé, cases spéciales, niveaux, bibliothèque d'exercices. Testé sans Android. |
| `app/` | Application Android (Kotlin, Jetpack Compose) : écrans, sauvegarde locale, synthèse vocale. |

## Compiler et tester

Il faut un JDK 17 et le SDK Android (installé avec Android Studio).

```bash
./gradlew :game:test          # tests des règles du jeu
./gradlew :app:assembleDebug  # APK de test dans app/build/outputs/apk/debug/
```

Chaque push et chaque pull request lancent ces deux commandes sur GitHub Actions. L'APK de test se télécharge depuis l'onglet **Actions**, dans les artefacts du dernier passage (`crazy-goose-debug-apk`).

## Installer l'APK de test sur un téléphone

1. Téléchargez l'artefact `crazy-goose-debug-apk` depuis l'onglet Actions et décompressez-le.
2. Copiez `app-debug.apk` sur le téléphone (câble, Google Drive, e-mail…).
3. Ouvrez le fichier sur le téléphone et autorisez l'installation depuis cette source quand Android le demande.

## Données

Toutes les données (profils, partie en cours, historique) restent sur le téléphone. L'application n'a pas d'accès Internet.
