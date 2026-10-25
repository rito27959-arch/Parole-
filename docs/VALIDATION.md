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

Le backend n’est pas encore déployé. Le projet Firebase de production, sa configuration Android et les identifiants AdMob réels ne sont pas fournis. Dans l’APK livré, les séances IA attendent cette activation et AdMob utilise les identifiants Google de test. Aucun secret Groq ou compte de service n’est inclus.

Les émulateurs ne valident pas les permissions du futur compte Firebase de production. Le microphone, la transcription de voix humaines, TalkBack, les appareils physiques et l’affichage réel des annonces UMP/AdMob restent à vérifier avant diffusion publique. Le test de signature SSV utilise une paire de clés de test, pas une publicité facturée.

Le build est signé pour le développement. La publication Google Play exige le workflow de release et un keystore privé ; elle n’a pas été effectuée.
