# Activation de Parole

## Firebase

Le projet dédié **Parole**, identifiant `parole-6a0bf`, est créé. Authentication > E-mail/Mot de passe est activé, et les applications Android `app.parole` (release) et `app.parole.debug` (bêta) sont enregistrées. Une création de compte synthétique, une connexion et sa suppression ont été vérifiées contre Firebase Auth en production.

Firestore n’est pas encore créé. L’emplacement proposé est `europe-west4` (Pays-Bas), proche du serveur Railway ; ce choix irréversible attend une validation explicite. Le Cameroun correspond au pays de paiement AdMob demandé, pas au lieu de stockage choisi pour les données. Ajouter les empreintes de signature Android quand nécessaire.

Après création de Firestore, publier les règles : `firebase deploy --only firestore:rules --project parole-6a0bf`. Aucun bucket Storage n’est nécessaire pour cette version. Si un bucket est créé ultérieurement, publier aussi les règles Storage fermées du dépôt.

Les écritures Firestore passent exclusivement par le serveur Firebase Admin. Les clients authentifiés ne lisent que leurs propres données. Le bucket Storage reste fermé : cette version traite PDF/audio en mémoire et conserve les fichiers audio localement.

Le compte Firebase Admin SDK du projet existe déjà ; aucune clé privée n’a été générée pour cette intégration. Sa génération puis son enregistrement dans `Parole/parole-api` sur Railway attendent une autorisation explicite. Vérifier ses droits Firebase Authentication et Firestore et les limiter aux besoins du serveur. Enregistrer le JSON **comme secret serveur**, jamais dans le dépôt ou l’APK. Les rôles IAM de production n’ont pas encore été audités.

Configurer ensemble les quatre variables Firebase ci-dessous : le backend ne doit pas distribuer une configuration Android incomplète.

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

Le service `Parole/parole-api` est déployé à l’adresse https://parole-api-production.up.railway.app et suit déjà `main`. Les variables Groq sont configurées ; la connexion du serveur au projet Firebase reste à activer. Un healthcheck réussi ne prouve pas que Firebase fonctionne : contrôler `/ready`, puis une véritable inscription et une séance.

La CI bêta utilise cette adresse par défaut. Elle peut être remplacée via `PAROLE_API_URL` au build ou dans Profil > Connexion. Les builds locaux sans cette variable restent sans serveur par défaut, notamment pour les tests isolés.

## GitHub CI

Le workflow beta fonctionne sans secret et construit un APK installable pour vérifier l’interface. Le service IA devient utilisable quand le serveur et Firebase sont activés.

Variable de dépôt : `PAROLE_API_URL`.
Secret facultatif : `GOOGLE_SERVICES_JSON` (JSON intégral, non encodé en base64).

Pour une release signée : `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GOOGLE_SERVICES_JSON` ; variables `PAROLE_API_URL`, `ADMOB_APP_ID`, `ADMOB_REWARDED_ID`. Le workflow refuse une release sans configuration.

## AdMob

Le pays de paiement retenu est le **Cameroun**. L’inscription réelle attend un titulaire éligible et un compte validé ; aucun identifiant publicitaire de production n’a été créé.

Les builds beta utilisent les identifiants Google de test ; ils ne rapportent pas d’argent. Ne jamais cliquer sur ses propres annonces réelles.

Configurer le consentement dans AdMob > Confidentialité et messages. Ajouter l’URL SSV `https://VOTRE_SERVEUR/admob/ssv` au bloc publicitaire et configurer `ADMOB_REWARDED_AD_UNIT` sur le serveur. Les champs utilisateur et ticket sont renseignés avant affichage.

Le serveur vérifie la signature ECDSA Google, le bloc publicitaire, l’horodatage, le ticket, l’utilisateur et l’unicité de la transaction. Deux crédits de questions sont attribués une seule fois. Une séance récompensée consomme deux crédits. Le callback mobile seul ne crée aucun crédit.

Publier `app-ads.txt`, faire valider l’app par AdMob, déclarer la collecte de données et publier une politique de confidentialité avec les coordonnées réelles de l’éditeur.

## Vérification avant diffusion publique

Exécuter les tests, tester le micro sur un téléphone réel, valider les accents français avec de vrais enregistrements, tester perte réseau/reprise, contrôler les coûts Groq et les quotas. Vérifier les choix UMP et la SSV sur un environnement de test. Prévoir rétention des données et suppression des tickets publicitaires expirés. Les tests de rendu ne remplacent pas TalkBack ni les essais sur plusieurs téléphones.
