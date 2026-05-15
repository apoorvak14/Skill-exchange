# SkillExchange

SkillExchange is a Kotlin + Jetpack Compose Android app for rural service barter. Technicians can post needs, offer their own skills, chat in real time, complete swaps, and build trust around a simple exchange rule:

**1 hour = 1 skill point**

## What is included

- Kotlin Android app with Jetpack Compose UI
- MVVM + Repository structure with clean domain/data boundaries
- Firebase Phone OTP authentication
- Firestore realtime streams for profiles, needs, offers, chats, ratings, and trust data
- FCM token registration service
- Firebase callable Functions contract for GenAI recommendations and fraud checks
- Firestore security rules and indexes
- Trust score flow where client users can confirm completion, but score updates happen only through backend logic after both participants confirm
- Multilingual-friendly UI fields using language codes and short, plain labels

## Project structure

```text
SkillExchange/
  app/
    src/main/java/com/skillexchange/
      core/                 Result and Firestore constants
      data/remote/          Firebase Auth, Firestore, FCM, GenAI repositories
      di/                   Lightweight app container
      domain/model/         App models
      domain/repository/    Repository contracts
      ui/                   Compose screens and ViewModels
  functions/                Firebase Functions TypeScript backend
  firestore.rules           Production-oriented Firestore rules
  firestore.indexes.json    Required composite indexes
  firebase.json             Firebase deploy config
```

## Setup

1. Open this folder in Android Studio.
2. Create a Firebase project.
3. Add an Android app with package name `com.skillexchange`.
4. Download `google-services.json` and place it at:

```text
app/google-services.json
```

5. Enable Firebase Authentication > Phone provider.
6. Enable Firestore in production mode.
7. Enable Firebase Cloud Messaging.
8. Deploy Firestore rules and indexes:

```bash
firebase deploy --only firestore
```

9. Install and deploy Functions:

```bash
cd functions
npm install
npm run deploy
```

10. Build and run the Android app from Android Studio.

## GenAI integration

The Android app calls Firebase Functions instead of shipping GenAI API keys in the APK.

- `recommendMatches`: reads open need posts and returns ranked barter recommendations.
- `detectFraud`: checks user generated post text for fraud, spam, coercion, and unsafe barter patterns.
- `updateTrustAfterBothConfirm`: updates trust scores only after both swap participants confirm completion.

The included Functions sample uses Vertex AI Gemini. In production, deploy Functions in the same Google Cloud/Firebase project and ensure Vertex AI is enabled.

## Firestore collections

- `users/{uid}`: profile, village, language, skills, FCM token, trust score
- `needPosts/{postId}`: requested skill, offered skill, hours, village, status
- `swapOffers/{offerId}`: participants, skill point amount, status, completion confirmations
- `chats/{chatId}/messages/{messageId}`: realtime chat messages
- `ratings/{ratingId}`: post-completion ratings
- `fraudSignals/{signalId}`: server-created fraud audit records

## Production notes

- Add App Check before public launch.
- Restrict Firebase API keys by Android package and SHA certificates.
- Add SMS region allowlists and abuse quotas for Phone Auth.
- Add moderation review tooling for `fraudSignals`.
- Move trust/rating calculations into Cloud Functions only; rules already prevent direct client edits to trust counters.
- Add translated string resources for target villages/languages when copy is finalized.
