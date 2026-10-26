# Validation de la bêta Parole

Version : `1.1.0-beta` · code version `2` · application `app.parole.debug` · Android 8.0 minimum.

## Splash et onboarding

- Splash Android natif avec symbole vocal animé, fond adapté au thème système et sortie sans attente artificielle. Le démarrage de la vraie Activity est couvert par le test d’intégration Android ; la fluidité du splash sur plusieurs téléphones reste à vérifier.
- Introduction en trois étapes, accessible hors ligne : identité visuelle vocale, choix BTS/Licence/Master et jury bienveillant/exigeant, explication du parcours et des permissions différées.
- Préférences conservées après redémarrage, bouton Passer, retour à l’étape précédente, présentation relançable depuis le profil.
- Thèmes clair/sombre, contenus défilants en grandes polices, boutons de navigation fixes, animation suspendue en arrière-plan et réduction des animations prise en compte.
- L’initialisation publicitaire attend la fin de l’introduction. L’onboarding n’accorde pas le consentement à l’envoi des contenus et ne demande pas le microphone.

## Résultats constatés

- 21 tests serveur réussis : authentification, isolation des utilisateurs, import PDF réel, quotas, doublons, concurrence, reprise après interruption, suppression du compte, erreurs Groq et signatures AdMob SSV.
- 5 tests unitaires Android réussis : métriques du bilan.
- 17 tests Android sur émulateur API 35 réussis, sans test ignoré : clair/sombre, grandes polices, navigation, saisie, consentement, parcours d’introduction, choix conservés, redémarrage et relecture de l’onboarding. Quinze captures exportées et contrôlées visuellement ; le visuel du choix du jury a été réduit après contrôle pour laisser les deux options visibles.
- Analyse Android Lint : aucune erreur bloquante. Des avertissements de versions de bibliothèques et de style Kotlin restent présents.
- 5 tests Firebase Auth et Firestore avec les émulateurs officiels : création d’utilisateur, jeton d’émulateur, persistance, transactions, règles d’accès et suppression de l’identité.
- Firebase Auth de production : inscription d’un utilisateur synthétique, connexion et suppression réussies. Le compte de test a été supprimé. Cela ne valide pas encore Firestore ni la connexion Android via le serveur.
- Appels réels Groq réussis lors de la validation initiale : génération d’une question puis analyse d’une réponse française avec `openai/gpt-oss-120b`. La clé reste hors du dépôt et de l’APK.
- Vérification HTTPS du backend le 26 octobre 2026 : `/health` répond 200 avec `configured: false`, `/ready` répond 503, `/config` renvoie `firebase: null`.
- APK construit par GitHub Actions ; signature de développement vérifiée avec `apksigner`.

APK SHA-256 :

```text
1959ce41e732fd763f3aa41305cc4044ea7cc50831ffb8c42b0f0e83c7d0baf3
```

Empreinte SHA-256 du certificat Android de développement :

```text
602346d12224dac974ae2debc2d987543209fd6014cc5651ff15f5899bd65a84
```

Build du code `bef0c84ada55b0c3f5c5161650cc43b5dc88d4f6` :
https://github.com/rito27959-arch/Parole-/actions/runs/40269863334

## Installation de la bêta

Cet APK pointe par défaut vers https://parole-api-production.up.railway.app. L’adresse peut être remplacée dans Profil > Connexion.

La signature de développement est régénérée dans les builds CI actuels. Une mise à jour d’une ancienne bêta peut donc être refusée par Android. Si nécessaire, exporter auparavant les bilans et sauvegarder les éléments locaux, puis désinstaller l’ancienne bêta avant d’installer la nouvelle ; la désinstallation efface ses données locales. La publication et les mises à jour publiques doivent utiliser une clé de signature stable et privée.

## État réel du cloud

Le backend `Parole/parole-api` est déployé sur Railway et suit la branche `main`. La clé Groq est enregistrée comme variable serveur.

Le projet Firebase `parole-6a0bf` est créé, Authentication e-mail/mot de passe est activé et les deux applications Android `app.parole` / `app.parole.debug` sont enregistrées. Firestore n’est pas encore créé : le choix proposé `europe-west4` (Pays-Bas) attend une autorisation explicite. Aucune clé privée Firebase Admin n’a été générée pour cette intégration ; sa génération et son stockage comme secret Railway restent aussi à autoriser. Les rôles IAM de production n’ont pas été audités et les règles Firestore du dépôt ne sont pas encore publiées en production.

**Les séances IA en production et l’historique cloud restent indisponibles.** L’interface indique le service à activer ; les tests sur émulateurs ne remplacent pas cette activation.

Le pays demandé pour AdMob est le Cameroun. L’inscription attend un titulaire éligible et un compte validé. Aucun app ID ni bloc publicitaire de production n’a été créé ; la bêta utilise uniquement les identifiants Google de test et ne génère pas de revenus.

## Limites de la validation

Le microphone, la transcription de voix humaines, TalkBack, les appareils physiques et l’affichage réel des annonces UMP/AdMob restent à vérifier avant diffusion publique. Le test de signature SSV utilise une paire de clés de test, pas une publicité facturée. Les captures de projets et de bilans utilisent des données fictives réservées aux tests ; l’application démarre sans historique prérempli.

Le build est signé pour le développement. La publication Google Play exige le workflow de release et un keystore privé ; elle n’a pas été effectuée.
