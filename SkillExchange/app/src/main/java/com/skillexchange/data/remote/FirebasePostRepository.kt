package com.skillexchange.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skillexchange.core.AppResult
import com.skillexchange.core.FirestoreCollections
import com.skillexchange.core.readableMessage
import com.skillexchange.domain.model.NeedPost
import com.skillexchange.domain.model.PostStatus
import com.skillexchange.domain.repository.PostRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PostRepository {
    override fun observeOpenPosts(skill: String?): Flow<List<NeedPost>> = callbackFlow {
        var query: Query = db.collection(FirestoreCollections.POSTS)
            .whereEqualTo("status", PostStatus.OPEN.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        if (!skill.isNullOrBlank()) query = query.whereEqualTo("requiredSkill", skill)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot?.documents?.mapNotNull { it.toModelWithId<NeedPost>() }.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    override fun observeMyPosts(uid: String): Flow<List<NeedPost>> = callbackFlow {
        val registration = db.collection(FirestoreCollections.POSTS)
            .whereEqualTo("ownerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.documents?.mapNotNull { it.toModelWithId<NeedPost>() }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createPost(post: NeedPost): AppResult<String> = try {
        val ref = db.collection(FirestoreCollections.POSTS).document()
        ref.set(post.copy(id = ref.id, status = PostStatus.OPEN)).await()
        AppResult.Success(ref.id)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }
}
