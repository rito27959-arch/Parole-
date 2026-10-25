# Activation de Parole

## Firebase

Créer un projet dédié Parole. Activer Authentication > E-mail/Mot de passe et Cloud Firestore. Créer les applications Android `app.parole` (release) et `app.parole.debug` (bêta). Ajouter leurs empreintes de signature quand nécessaire.

Déployer les règles : `firebase deploy --only firestore:rules,storage --project VOTRE_PROJET`.

Les écritures Firestore passent exclusivement par le serveur Firebase Admin. Les clients authentifiés ne lisent que leurs propres données. Le bucket Storage reste fermé : cette version traite PDF/audio en mémoire et conserve les fichiers audio localement.

Créer un compte de service dédié pour le backend avec les droits nécessaires à Firestore et Firebase Authentication, sans rôle propriétaire. Enregistrer le JSON **comme secret serveur**, jamais dans le dépôt.

## Variables serveur

- `GROQ_API_KEY` : clé privée Groq ; remplacer la clé initialement partagée en conversation avant production.
- `FIREBASE_PROJECT_ID` : identifiant du projet.
- `FIREBASE_SERVICE_ACCOUNT_JSON` : compte de service JSON, variable secrète.
- `FIREBASE_WEB_API_KEY` et `FIREBASE_ANDROID_APP_ID` : configuration publique de l’application Android bêta, pour `/config`.
- `GROQ_MODEL` : `openai/gpt-oss-120b` par défaut.
- `GROQ_TRANSCRIPTION_MODEL` : `whisper-large-v3-turbo`.
- `FREE_DAILY_SESSIONS` : 3 par défaut.
- `ADMOB_REWARDED_AD_UNIT` : vrai identifiant du bloc récompensé, vide tant que non activé.

`/health` indique que le processus tourne. `/ready` répond 503 tant que les variables minimales sont absentes. Vérifier ensuite une vraie connexion Firebase et une séance Groq : la présence des variables seule ne valide pas leurs droits ou leur validité.

## Railway

Le `Dockerfile` racine construit le backend. Lier le dépôt, branche `main`, sans sous-répertoire. Configurer `/health` comme healthcheck, ajouter les variables puis générer un domaine HTTPS. Les données persistantes sont dans Firestore, pas sur le disque Railway.

Dans Android, l’adresse peut être fournie via `PAROLE_API_URL` au build ou dans Profil > Connexion.

## GitHub CI

Le workflow beta fonctionne sans secret et construit un APK installable pour vérifier l’interface. Le service IA devient utilisable quand le serveur et Firebase sont activés.

Variable de dépôt : `PAROLE_API_URL`.
Secret facultatif : `GOOGLE_SERVICES_JSON` (JSON intégral, non encodé en base64).

Pour une release signée : `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GOOGLE_SERVICES_JSON` ; variables `PAROLE_API_URL`, `ADMOB_APP_ID`, `ADMOB_REWARDED_ID`. Le workflow refuse une release sans configuration.

## AdMob

Les builds beta utilisent les identifiants Google de test ; ils ne rapportent pas d’argent. Ne jamais cliquer sur ses propres annonces réelles.

Configurer le consentement dans AdMob > Confidentialité et messages. Ajouter l’URL SSV `https://VOTRE_SERVEUR/admob/ssv` au bloc publicitaire et configurer `ADMOB_REWARDED_AD_UNIT` sur le serveur. Les champs utilisateur et ticket sont renseignés avant affichage.

Le serveur vérifie la signature ECDSA Google, le bloc publicitaire, l’horodatage, le ticket, l’utilisateur et l’unicité de la transaction. Deux crédits de questions sont attribués une seule fois. Une séance récompensée consomme deux crédits. Le callback mobile seul ne crée aucun crédit.

Publier `app-ads.txt`, faire valider l’app par AdMob, déclarer la collecte de données et publier une politique de confidentialité avec les coordonnées réelles de l’éditeur.

## Vérification avant diffusion publique

Exécuter les tests, tester le micro sur un téléphone réel, valider les accents français avec de vrais enregistrements, tester perte réseau/reprise, contrôler les coûts Groq et les quotas. Vérifier les choix UMP et la SSV sur un environnement de test. Prévoir rétention des données et suppression des tickets publicitaires expirés. Les tests de rendu ne remplacent pas TalkBack ni les essais sur plusieurs téléphones.
