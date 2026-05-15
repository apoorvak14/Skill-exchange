package com.skillexchange.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skillexchange.core.AppResult
import com.skillexchange.core.FirestoreCollections
import com.skillexchange.core.readableMessage
import com.skillexchange.domain.model.ChatMessage
import com.skillexchange.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChatRepository {
    override fun observeMessages(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection(FirestoreCollections.CHATS)
            .document(threadId)
            .collection(FirestoreCollections.MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.documents?.mapNotNull { it.toModelWithId<ChatMessage>() }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(threadId: String, message: ChatMessage): AppResult<Unit> = try {
        val messageRef = db.collection(FirestoreCollections.CHATS)
            .document(threadId)
            .collection(FirestoreCollections.MESSAGES)
            .document()
        val threadRef = db.collection(FirestoreCollections.CHATS).document(threadId)
        db.runBatch { batch ->
            batch.set(messageRef, message.copy(id = messageRef.id, threadId = threadId))
            batch.update(threadRef, mapOf("lastMessage" to message.text, "updatedAt" to System.currentTimeMillis()))
        }.await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }
}
