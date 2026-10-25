# Validation de la bêta Parole

Version : `1.0.0-beta` · application `app.parole.debug` · Android 8.0 minimum.

## Résultats constatés

- 21 tests serveur réussis : authentification, isolation des utilisateurs, import PDF réel, quotas, doublons, concurrence, reprise après interruption, suppression du compte, erreurs Groq et signatures AdMob SSV.
- 5 tests unitaires Android réussis : métriques du bilan.
- 12 tests Android sur émulateur API 35 réussis, sans test ignoré : clair/sombre, grandes polices, navigation, saisie, consentement et démarrage de la vraie Activity sans cloud configuré. Huit captures exportées et contrôlées visuellement.
- Analyse Android Lint : aucune erreur bloquante. Des avertissements de versions de bibliothèques et de style Kotlin restent présents.
- Firebase Auth et Firestore testés avec les émulateurs officiels : création d’utilisateur, jeton d’émulateur, persistance, transactions, règles d’accès et suppression de l’identité.
- Appels réels Groq réussis : génération d’une question puis analyse d’une réponse française avec `openai/gpt-oss-120b`. La clé reste hors du dépôt et de l’APK.
- Test HTTP local de l’API : `/health` répond 200 en signalant la configuration absente ; `/ready` répond 503 dans cet état.
- APK construit par GitHub Actions ; signature de développement vérifiée avec `apksigner`.

APK SHA-256 :

```text
53da9a97c8451950eab04dc6d3405571bb4c93e8602333959dd6defa4e4c0776
```

Build du code `9339951774d2b13d1061ccb9dd87465e4ee6aca3` :
https://github.com/rito27959-arch/Parole-/actions/runs/40167450647

## Activation restante

Le backend a été déployé sur Railway le 25 octobre 2026 : https://parole-api-production.up.railway.app. Le déploiement `5f033602-2d46-4d19-bc01-4d0379217cdb` est réussi et suit la branche `main`. La clé Groq est enregistrée comme variable serveur. Vérification HTTPS réelle : `/health` répond 200 avec `configured: false`, `/ready` répond 503, `/config` renvoie `firebase: null` et `/privacy` répond 200.

Le projet Firebase de production et le compte AdMob ne sont pas encore créés : leurs consoles demandent l’acceptation initiale des conditions. Le pays de paiement AdMob doit être confirmé avant inscription. Les séances IA attendent cette activation et AdMob utilise toujours les identifiants Google de test. Aucun secret Groq ou compte de service n’est inclus dans le dépôt ou l’APK.

Les prochains APK de la CI utiliseront l’adresse Railway par défaut, avec surcharge possible par `PAROLE_API_URL`. L’APK du build référencé plus haut demande encore de saisir cette adresse dans Profil > Connexion.

Les émulateurs ne valident pas les permissions du futur compte Firebase de production. Le microphone, la transcription de voix humaines, TalkBack, les appareils physiques et l’affichage réel des annonces UMP/AdMob restent à vérifier avant diffusion publique. Le test de signature SSV utilise une paire de clés de test, pas une publicité facturée.

Le build est signé pour le développement. La publication Google Play exige le workflow de release et un keystore privé ; elle n’a pas été effectuée.
