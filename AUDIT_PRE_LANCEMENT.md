# AUDIT PRÉ-LANCEMENT — Aura

> Généré le 2026-11-23 | Auditeur : Claude Sonnet 4.6  
> Stack : Kotlin Multiplatform (KMP) + Compose Multiplatform, Android + iOS  
> Backend : Firebase Auth + Firestore + Cloud Functions (Node 22)  
> Contexte : app sociale de scoring d'aura, freemium/IAP, marché France, lancement public avec acquisition

---

## 1. Verdict

### 🔴 NO-GO — Blocages légaux et de sécurité à corriger avant publication

L'app est techniquement aboutie et l'expérience principale (scoring, battles, groupes) est fonctionnelle. Mais **trois familles de problèmes bloquent la soumission aux stores** :

1. **Conformité stores** : la suppression du compte depuis l'app est **absente** — Apple et Google la rendent obligatoire pour toute app avec création de compte ; le refus est quasi-certain.
2. **Légal RGPD** : aucune politique de confidentialité, aucune CGU, aucune mention légale accessible dans l'app ; le freemium/IAP annoncé n'est pas implémenté.
3. **Sécurité** : les fichiers de configuration Firebase (`google-services.json`, `GoogleService-Info.plist`) sont commités en clair dans git depuis le premier commit — toutes les clés sont exposées dans l'historique public.

Les domaines UX, performance et code sont de bonne qualité pour un MVP.

---

## 2. Tableau de synthèse

| # | Domaine | Note /10 | Justification |
|---|---------|----------|---------------|
| 1 | Réponse au besoin | 7/10 | Parcours principaux fonctionnels (scoring, battle, groupes, amis) ; IAP freemium annoncé absent |
| 2 | UX / UI | 7/10 | Design soigné et cohérent ; manque états d'erreur réseau explicites, reset mot de passe absent |
| 3 | Accessibilité | 4/10 | Aucun contentDescription sur icônes/boutons ; cibles tactiles non vérifiables sans appareil |
| 4 | Performance | 6/10 | `allUsersRanked()` charge toute la collection users à chaque ouverture ; battles en temps réel bien faites |
| 5 | Sécurité | 4/10 | Clés Firebase dans l'historique git ; toutes les limites anti-abus côté client uniquement ; `allowBackup=true` |
| 6 | Fiabilité | 5/10 | Pas de crash reporting ; pas de gestion du mode hors-ligne ; erreurs silencieuses dans les batch |
| 7 | Légal / RGPD | 1/10 | Aucune politique de confidentialité, CGU, suppression de compte, ni inventaire de données |
| 8 | Monétisation | 0/10 | IAP annoncé, aucune implémentation présente |
| 9 | Préparation stores | 3/10 | Suppression compte absente ; Info.plist iOS quasi-vide ; `isMinifyEnabled=false` en release |
| 10 | Qualité code | 7/10 | Architecture propre MVVM, règles Firestore sérieuses ; zéro test ; pas de CI |

---

## 3. Liste des problèmes

### 🔴 Bloquants

---

**[B-01] Suppression du compte absente**  
Domaine : Légal/Stores | Effort : M

- **Emplacement** : `UserViewModel.kt` — aucune méthode `deleteAccount()` ; `UserRepository.kt` — aucun `auth.deleteUser()` ni suppression des données Firestore.
- **Preuve** : `grep -r "deleteAccount\|deleteUser" composeApp/src` → 0 résultat.
- **Impact** : rejet automatique par Apple (App Store Review Guideline 5.1.1) et Google Play (Data deletion policy). Toute app avec inscription doit proposer la suppression depuis l'app.
- **Correction** : ajouter `auth.currentUser?.delete()` + suppression des documents Firestore (user, friends, events, groupMember, groupInvites) dans une Cloud Function pour éviter les droits insuffisants côté client. Ajouter un bouton dans `EditProfileSheetContent` ou les paramètres avec confirmation.

---

**[B-02] Politique de confidentialité, CGU et mentions légales absentes**  
Domaine : Légal/RGPD | Effort : M

- **Emplacement** : aucun fichier dans `ui/` ne référence de lien vers politique de confidentialité, CGU, ou mentions légales.
- **Preuve** : `grep -r "privacy\|confidentialite\|cgu\|mentions" composeApp/src --include="*.kt"` → 0 résultat.
- **Impact** : (1) refus App Store (Guideline 5.1.1 — Privacy Policy required) ; (2) Google Play Data Safety section requiert un lien vers PP valide ; (3) obligation légale RGPD (Art. 13) et loi française (LCEN).
- **Correction** : héberger une politique de confidentialité (peut être sur `aura-7d0fb.web.app/privacy`), ajouter un lien cliquable sur l'écran d'inscription (`RegisterScreen`) et dans les paramètres du profil. Minimum : politique de confidentialité + inventaire des données collectées (email, nom, photo, score, FCM token).

---

**[B-03] Fichiers Firebase commités dans git (historique exposé)**  
Domaine : Sécurité | Effort : S

- **Emplacement** : `composeApp/google-services.json` (commit `50d30cb`), `iosApp/iosApp/GoogleService-Info.plist` (commit `2069bae`).
- **Preuve** :
  ```
  git log --oneline -- composeApp/google-services.json
  → 8b82107, 2069bae, 50d30cb
  
  current_key = AIzaSyD5tC_F0zcR_juRXmoDQKFZrfd5-DiXaRg
  ```
- **Impact** : la clé API Firebase Android est exposée dans l'historique git public. Elle permet des lectures Firestore non authentifiées si les règles le tolèrent, et peut générer des coûts Firebase si abusée.
- **Correction** : (1) régénérer la clé dans la console Firebase/GCP immédiatement ; (2) ajouter `google-services.json` et `GoogleService-Info.plist` au `.gitignore` ; (3) ne pas réécrire l'historique git si le repo est public — la clé circule déjà ; régénérer suffit. Note : ces fichiers *peuvent* rester dans git privé selon la politique Google (la clé est côté client), mais les règles Firestore doivent être solides — elles le sont.

---

**[B-04] IAP freemium annoncé, zéro implémentation**  
Domaine : Monétisation | Effort : L

- **Emplacement** : `composeApp/build.gradle.kts` — aucune dépendance `billing` ou `play-billing` ou `StoreKit`.
- **Preuve** : `grep -r "BillingClient\|StoreKit\|purchase\|subscription" composeApp/src` → 0 résultat.
- **Impact** : si l'app est soumise avec un modèle freemium sans aucun IAP implémenté et sans paywall, Apple/Google peuvent rejeter pour "fonctionnalités trop minimales". Plus important : toute transaction monétaire future hors IAP natif (ex : lien Stripe web) est interdite par les règles Apple (Guideline 3.1.1) et peut entraîner une suppression de l'app.
- **Correction** : soit (a) lancer en mode 100% gratuit sans aucune mention de freemium (supprimer la référence dans les métadonnées store), soit (b) implémenter les IAP via Google Play Billing + StoreKit 2 avant soumission. Option (a) est la voie rapide pour un MVP.

---

**[B-05] Toutes les limites anti-abus sont côté client uniquement**  
Domaine : Sécurité | Effort : M

- **Emplacement** : `EventRepository.kt:72-100` — vérification de `MAX_POSITIVES_PER_DAY`, `MAX_NEGATIVES_PER_DAY`, cooldowns rareté entièrement en Kotlin côté client.
- **Preuve** : les règles Firestore (`firestore.rules`) permettent à tout utilisateur authentifié d'écrire un événement et d'incrémenter `auraScore` **sans vérifier les limites** :
  ```
  match /users/{userId}/events/{eventId} {
    allow create: if request.auth != null
                  && request.resource.data.authorUid == request.auth.uid
                  && request.auth.uid != userId;
  ```
  Aucun check sur les compteurs quotidiens, aucune validation du `delta`.
- **Impact** : un utilisateur peut bypasser les limites (3 positifs/jour, 1 négatif/jour) en modifiant le client ou en appelant Firestore directement. Il peut spam-incrémenter n'importe quel score. L'intégrité du classement est compromise.
- **Correction** : déplacer les vérifications dans une **Cloud Function** `onCall` ou `onDocumentCreated`. Alternativement, ajouter des règles Firestore qui vérifient les compteurs existants (lecture cross-document via `get()`). La Cloud Function est plus robuste et permet une validation atomique.

---

### 🟠 Majeurs

---

**[M-01] `allUsersRanked()` charge toute la collection `users` à chaque refresh**  
Domaine : Performance | Effort : M

- **Emplacement** : `UserRepository.kt:72` — `firestore.collection("users").get()`.
- **Preuve** : appelé dans `MainViewModel.load()` à chaque ouverture de l'app + pull-to-refresh éventuel.
- **Impact** : à 1 000 utilisateurs = 1 000 lectures Firestore par ouverture d'app par utilisateur. À 10 000 utilisateurs actifs/jour × 3 ouvertures = 300 M lectures/mois → dépassement du quota gratuit Firestore (50 000/jour). Temps de chargement proportionnel au nombre d'utilisateurs.
- **Correction** : (1) limiter le classement aux amis uniquement (déjà partiellement fait avec `_leaderboard`) ; (2) utiliser une collection dénormalisée `leaderboard` mise à jour par Cloud Function ; (3) pagination avec `limit()` + `startAfter()`.

---

**[M-02] Pas de réinitialisation du mot de passe**  
Domaine : UX/Auth | Effort : S

- **Emplacement** : `LoginScreen.kt` — aucun lien "Mot de passe oublié".
- **Preuve** : `grep -r "resetPassword\|sendPasswordReset\|mot.*passe.*oubli" composeApp/src` → 0 résultat.
- **Impact** : un utilisateur qui oublie son mot de passe ne peut pas récupérer son compte. Perte d'utilisateurs garantie, et risque de mauvaises notes sur les stores.
- **Correction** : ajouter un lien `TextButton` sur `LoginScreen` → `auth.sendPasswordResetEmail(email)`.

---

**[M-03] `android:allowBackup="true"` expose les tokens Firebase**  
Domaine : Sécurité | Effort : S

- **Emplacement** : `composeApp/src/androidMain/AndroidManifest.xml:11`.
- **Preuve** : `android:allowBackup="true"` sans `android:fullBackupContent` restrictif.
- **Impact** : les sauvegardes Android (ADB ou cloud Google) peuvent inclure les tokens Firebase Auth persistés par le SDK. Un accès physique à l'appareil permet de restaurer une session sur un autre appareil.
- **Correction** : passer à `android:allowBackup="false"` ou configurer `android:fullBackupContent` avec une règle qui exclut les données Firebase (`SharedPreferences` Firebase).

---

**[M-04] Info.plist iOS quasi-vide — Privacy Manifest manquant**  
Domaine : Stores iOS | Effort : M

- **Emplacement** : `iosApp/iosApp/Info.plist` — 2 clés seulement, aucune `NS...UsageDescription`.
- **Preuve** : contenu complet du fichier : `CADisableMinimumFrameDurationOnPhone` + `ITSAppUsesNonExemptEncryption`.
- **Impact** : (1) Apple exige depuis mai 2024 un **Privacy Manifest** (`PrivacyInfo.xcprivacy`) listant les APIs utilisées (FileKit utilise des APIs à déclarer) ; (2) FCM/notifications push nécessitent une justification ; (3) accès galerie photo (FileKit) requiert `NSPhotoLibraryUsageDescription` ; (4) rejet App Store garanti.
- **Correction** : ajouter dans `Info.plist` : `NSPhotoLibraryUsageDescription` (sélection de photo de profil), et créer un fichier `PrivacyInfo.xcprivacy` avec les APIs requises par les SDKs tiers (Firebase, FileKit). Vérifier le Privacy Manifest d'Apple à jour.

---

**[M-05] Pas de crash reporting ni de monitoring**  
Domaine : Fiabilité | Effort : S

- **Emplacement** : `composeApp/build.gradle.kts` — aucune dépendance Crashlytics, Sentry ou équivalent.
- **Preuve** : `grep -r "crashlytics\|sentry\|firebase.*crash" composeApp/src` → 0 résultat.
- **Impact** : en production, les crashs sont invisibles. Impossible de diagnostiquer les problèmes des utilisateurs, pas d'alertes.
- **Correction** : ajouter Firebase Crashlytics (déjà dans le projet Firebase, coût zéro) + `firebase-crashlytics` dans `build.gradle.kts`.

---

**[M-06] Pas de validation des entrées côté UI (inscription)**  
Domaine : UX | Effort : S

- **Emplacement** : `RegisterScreen.kt:51-62` — aucune validation email, aucune contrainte sur le mot de passe (longueur, complexité), aucun feedback en ligne.
- **Preuve** : les champs appellent directement `viewModel.register(email, password, name)` sans aucun check local.
- **Impact** : l'utilisateur reçoit l'erreur Firebase (en anglais, non localisée) après un appel réseau ; mot de passe "a" accepté côté client. Expérience utilisateur médiocre, mauvaises notes prévisibles.
- **Correction** : validation locale avant appel réseau : email regex, mot de passe ≥ 8 caractères, nom non vide. Traduire les messages d'erreur Firebase en français.

---

**[M-07] Leaderboard global expose tous les utilisateurs à la recherche**  
Domaine : Sécurité/RGPD | Effort : M

- **Emplacement** : `FriendRepository.kt:searchUsers()` + règles Firestore `users/{userId} allow read: if request.auth != null`.
- **Preuve** : `searchUsers()` fait une requête Firestore `where "email" equalTo q` — n'importe quel utilisateur authentifié peut chercher l'email exact d'un autre et récupérer son profil complet (nom, email, score, photoUrl, bio).
- **Impact** : exposition de l'email de tous les utilisateurs à la recherche par email exact. Problème RGPD (données personnelles exposées sans consentement explicite à ce niveau de granularité).
- **Correction** : (1) ne pas stocker l'email dans le document `users` public ou le remplacer par un hash pour la recherche ; (2) limiter la recherche au nom d'affichage uniquement ; (3) supprimer le champ `email` du `UserProfile` Firestore ou le déplacer dans une sous-collection privée.

---

### 🟡 Mineurs

---

**[Mi-01] `isMinifyEnabled = false` en release Android**  
Domaine : Stores/Sécurité | Effort : S  
`composeApp/build.gradle.kts:100` — le build release n'est pas minifié ni obfusqué. L'APK/AAB contient les noms de classes en clair. Activer `isMinifyEnabled = true` + ProGuard/R8 réduit la taille et complique le reverse engineering.

**[Mi-02] Token d'invitation généré côté client avec `kotlin.random`**  
Domaine : Sécurité | Effort : S  
`GroupRepository.kt:generateInviteLink()` — le token de 20 caractères est généré avec `kotlin.random.Random` (non cryptographiquement sûr). Utiliser `java.security.SecureRandom` ou laisser Firebase générer l'ID (`firestore.collection("groupLinks").document` génère un UUID v4 natif qui suffit comme token opaque).

**[Mi-03] `versionCode = 1` codé en dur**  
Domaine : Stores | Effort : S  
`composeApp/build.gradle.kts:93` — `versionCode = 1` et `versionName = "1.0"` en dur. À automatiser dans la CI avant chaque release.

**[Mi-04] Aucun état d'erreur réseau explicite sur les écrans principaux**  
Domaine : UX | Effort : M  
`MainViewModel.load()` — les `Result.failure` des repositories sont ignorés silencieusement (ex: `groupRepo.myGroups().onSuccess { ... }` sans `.onFailure`). L'utilisateur voit un écran vide sans savoir pourquoi. Ajouter un état `_error` global avec une SnackBar ou un message inline.

**[Mi-05] Emails en clair dans `FriendRequest`**  
Domaine : RGPD | Effort : S  
`FriendRepository.kt:sendFriendRequest()` — les emails de l'expéditeur et du destinataire (`fromEmail`, `toEmail`) sont stockés dans la collection `friendRequests` lisible par les deux parties. Le champ email n'est pas nécessaire une fois la demande acceptée. Supprimer ou ne pas stocker les emails dans ce document.

**[Mi-06] Aucun test unitaire ou d'intégration**  
Domaine : Qualité code | Effort : L  
`commonTest/` est vide. Pas de CI détectée. Les logiques critiques (calcul de rareté, formules de scoring battle, limites quotidiennes) ne sont pas couvertes.

**[Mi-07] Photo de profil stockée en base64 dans Firestore**  
Domaine : Performance/Coûts | Effort : M  
`EditProfileSheetContent.kt` → `encodeImageBase64()` — l'image est compressée puis stockée en base64 directement dans le document `users/{uid}`. Un document Firestore a une limite de 1 Mo. À grande échelle : documents lourds, coût de lecture élevé, lenteur d'affichage. Migrer vers Firebase Storage.

**[Mi-08] `BackHandler` iOS non testé**  
Domaine : UX | Effort : À vérifier manuellement  
`MainScreen.kt` utilise `BackHandler` (Compose Multiplatform). Sur iOS le comportement du geste "retour" natif (swipe from left edge) doit être vérifié — en particulier pour fermer la BattleScreen ou le profil ami.

---

## 4. Checklist de tests manuels

À réaliser sur : un iPhone 15 récent (iOS 17+), un Android milieu/entrée de gamme (Android 12+), et si possible une tablette.

### Auth & Compte
- [ ] Inscription email/mot de passe → vérifier l'onboarding groupe
- [ ] Connexion Google → vérifier que le profil est bien récupéré
- [ ] Déconnexion → vérifier que le retour à Home efface bien la session
- [ ] Mauvais identifiants → message d'erreur affiché en français (ou pas ?)
- [ ] Mot de passe oublié → (attendu : absent — confirmer le bug M-02)
- [ ] Session expirée en background → comportement à l'ouverture

### Parcours critiques
- [ ] Signaler un événement positif sur un ami → score mis à jour immédiatement ?
- [ ] Signaler 4 événements positifs → le 4e est bloqué (limite 3/j) ?
- [ ] Signaler un événement légendaire → cooldown 7 jours respecté ?
- [ ] Lancer une battle → compte à rebours visible, supporters qui tapent en temps réel ?
- [ ] Clôturer une battle → aura créditée immédiatement ?
- [ ] Créer un groupe → rejoindre via lien d'invitation sur un autre appareil
- [ ] Rejoindre un groupe via deep link `aura://join/<token>` depuis une notification

### Edge cases
- [ ] Mode avion pendant une battle → que se passe-t-il ?
- [ ] Ouvrir l'app sans connexion réseau → écran vide ou message ?
- [ ] Profil vide (nouveau compte) → affichage correct de l'AuraGauge ?
- [ ] Nom très long (> 30 caractères) → overflow dans les listes/podiums ?
- [ ] 0 amis → onglet Amis et Classement → états vides clairs ?
- [ ] Photo de profil volumineuse → compression effective, pas de crash ?

### Accessibilité
- [ ] VoiceOver (iOS) sur ProfileTab, BattleTab, GroupsTab → tous les boutons annoncés ?
- [ ] TalkBack (Android) → idem
- [ ] Taille de police maximale du système → aucun texte tronqué/débordant ?
- [ ] Mode sombre → lisibilité de tous les écrans ?

### iOS spécifique
- [ ] Sélecteur de photo de profil → demande de permission galerie ?
- [ ] Notifications push → apparaissent bien en mode background et foreground ?
- [ ] Swipe retour natif iOS depuis BattleScreen et profil ami

### Android spécifique
- [ ] Bouton retour système depuis chaque écran
- [ ] Clavier qui monte → champs d'inscription/connexion accessibles ?
- [ ] `imePadding()` sur EditProfileSheet → vérifié ?

---

## 5. Plan d'action

### Sprint 1 — Blocages (avant toute soumission)

| ID | Action | Effort |
|----|--------|--------|
| B-01 | Implémenter suppression de compte (Auth + Firestore) | M |
| B-02 | Rédiger et héberger politique de confidentialité + CGU ; ajouter liens dans l'app | M |
| B-03 | Régénérer les clés Firebase ; ajouter les fichiers config au `.gitignore` | S |
| B-04 | Choisir : lancer 100% gratuit (supprimer référence IAP) OU implémenter IAP | M-L |
| B-05 | Déplacer les limites anti-abus dans une Cloud Function | M |
| M-04 | Créer `PrivacyInfo.xcprivacy` iOS + `NSPhotoLibraryUsageDescription` | M |

### Sprint 2 — Quick wins (avant lancement public)

| ID | Action | Effort |
|----|--------|--------|
| M-02 | Ajouter "Mot de passe oublié" sur LoginScreen | S |
| M-03 | Passer `allowBackup=false` dans AndroidManifest | S |
| M-05 | Ajouter Firebase Crashlytics | S |
| M-06 | Validation locale des formulaires inscription + messages d'erreur en français | S |
| Mi-01 | Activer `isMinifyEnabled = true` en release | S |
| Mi-02 | Remplacer `kotlin.random` par `SecureRandom` pour le token d'invitation | S |

### Sprint 3 — Après lancement

| ID | Action | Effort |
|----|--------|--------|
| M-01 | Réécrire `allUsersRanked()` avec pagination / dénormalisation | M |
| M-07 | Supprimer l'email du `UserProfile` public (ou hash) ; refactor recherche | M |
| Mi-07 | Migrer les photos de profil vers Firebase Storage | M |
| Mi-04 | Ajouter gestion d'erreur réseau globale dans MainViewModel | M |
| Mi-06 | Ajouter tests unitaires sur logiques métier (rareté, scoring) + CI | L |

---

## 6. Points forts

- **Règles Firestore sérieuses** : les règles de sécurité sont détaillées, well-thought-out, et couvrent les principaux vecteurs d'abus (IDOR, modification par des tiers, inflation du score des battles). C'est un vrai point fort pour une app de ce type.
- **Architecture propre** : séparation claire Repository / ViewModel / UI, utilisation correcte de StateFlow, Compose Multiplatform bien structuré.
- **Anti-abus côté client** : les limites quotidiennes, les cooldowns par rareté et le plafond de perte journalière montrent une vraie réflexion sur l'équilibre du jeu — même si elles doivent migrer côté serveur.
- **Battle en temps réel** : l'utilisation de snapshots Firestore + transactions pour les battles est correcte et évite les race conditions grâce à `runTransaction`.
- **Partage natif soigné** : les cartes de partage (profil, battle, invitation groupe) sont dessinées en code natif Android et bien intégrées.
- **Deep links groupes** : le mécanisme d'invitation via lien partageable + deep link `aura://join/<token>` est fonctionnel et bien pensé.

---

*Fin de l'audit. En attente de validation avant toute correction.*
