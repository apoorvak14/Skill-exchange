package com.skillexchange.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skillexchange.core.AppResult
import com.skillexchange.core.FirestoreCollections
import com.skillexchange.core.readableMessage
import com.skillexchange.domain.model.OfferStatus
import com.skillexchange.domain.model.ChatThread
import com.skillexchange.domain.model.Rating
import com.skillexchange.domain.model.SwapOffer
import com.skillexchange.domain.repository.SwapRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSwapRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SwapRepository {
    override fun observeOffersForUser(uid: String): Flow<List<SwapOffer>> = callbackFlow {
        val registration = db.collection(FirestoreCollections.OFFERS)
            .whereArrayContains("participantIds", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.documents?.mapNotNull { it.toModelWithId<SwapOffer>() }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createOffer(offer: SwapOffer): AppResult<String> = try {
        val ref = db.collection(FirestoreCollections.OFFERS).document()
        val chatRef = db.collection(FirestoreCollections.CHATS).document(ref.id)
        val payload = hashMapOf(
            "id" to ref.id,
            "postId" to offer.postId,
            "fromUserId" to offer.fromUserId,
            "toUserId" to offer.toUserId,
            "participantIds" to listOf(offer.fromUserId, offer.toUserId),
            "offeredSkill" to offer.offeredSkill,
            "requestedSkill" to offer.requestedSkill,
            "estimatedHours" to offer.estimatedHours,
            "message" to offer.message,
            "status" to OfferStatus.PENDING.name,
            "fromConfirmedComplete" to false,
            "toConfirmedComplete" to false,
            "createdAt" to System.currentTimeMillis()
        )
        db.runBatch { batch ->
            batch.set(ref, payload)
            batch.set(
                chatRef,
                ChatThread(
                    id = ref.id,
                    participantIds = listOf(offer.fromUserId, offer.toUserId),
                    postId = offer.postId,
                    offerId = ref.id
                )
            )
        }.await()
        AppResult.Success(ref.id)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }

    override suspend fun acceptOffer(offerId: String): AppResult<Unit> = try {
        db.collection(FirestoreCollections.OFFERS)
            .document(offerId)
            .update("status", OfferStatus.ACCEPTED.name)
            .await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }

    override suspend fun confirmComplete(offerId: String, uid: String): AppResult<Unit> = try {
        val ref = db.collection(FirestoreCollections.OFFERS).document(offerId)
        db.runTransaction { tx ->
            val offer = tx.get(ref)
            val fromUserId = offer.getString("fromUserId").orEmpty()
            val toUserId = offer.getString("toUserId").orEmpty()
            val field = when (uid) {
                fromUserId -> "fromConfirmedComplete"
                toUserId -> "toConfirmedComplete"
                else -> error("Only participants can confirm completion.")
            }
            tx.update(ref, field, true, "updatedAt", FieldValue.serverTimestamp())
            val fromConfirmed = if (field == "fromConfirmedComplete") true else offer.getBoolean("fromConfirmedComplete") == true
            val toConfirmed = if (field == "toConfirmedComplete") true else offer.getBoolean("toConfirmedComplete") == true
            if (fromConfirmed && toConfirmed) {
                tx.update(ref, "status", OfferStatus.COMPLETED.name)
            }
        }.await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }

    override suspend fun rateSwap(rating: Rating): AppResult<Unit> = try {
        val ref = db.collection(FirestoreCollections.RATINGS).document()
        ref.set(rating.copy(id = ref.id)).await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }
}
