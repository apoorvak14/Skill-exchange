package com.skillexchange.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.skillexchange.core.AppResult
import com.skillexchange.core.FirestoreCollections
import com.skillexchange.core.readableMessage
import com.skillexchange.domain.model.UserProfile
import com.skillexchange.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {
    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = db.collection(FirestoreCollections.USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.toModelWithId<UserProfile>())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun upsertProfile(profile: UserProfile): AppResult<Unit> = try {
        db.collection(FirestoreCollections.USERS)
            .document(profile.uid)
            .set(profile)
            .await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }

    override suspend fun saveFcmToken(uid: String, token: String): AppResult<Unit> = try {
        db.collection(FirestoreCollections.USERS)
            .document(uid)
            .update("fcmToken", token)
            .await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }
}
