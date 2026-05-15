package com.skillexchange.data.remote

import com.google.firebase.functions.FirebaseFunctions
import com.skillexchange.core.AppResult
import com.skillexchange.core.readableMessage
import com.skillexchange.domain.model.MatchRecommendation
import com.skillexchange.domain.repository.GenAiRepository
import kotlinx.coroutines.tasks.await

class FirebaseGenAiRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : GenAiRepository {
    override suspend fun recommendations(
        uid: String,
        skill: String?,
        village: String?
    ): AppResult<List<MatchRecommendation>> = try {
        val result = functions
            .getHttpsCallable("recommendMatches")
            .call(mapOf("uid" to uid, "skill" to skill, "village" to village))
            .await()

        @Suppress("UNCHECKED_CAST")
        val raw = result.data as? List<Map<String, Any?>> ?: emptyList()
        AppResult.Success(raw.map {
            MatchRecommendation(
                postId = it["postId"] as? String ?: "",
                title = it["title"] as? String ?: "",
                score = (it["score"] as? Number)?.toDouble() ?: 0.0,
                reason = it["reason"] as? String ?: ""
            )
        })
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }

    override suspend fun fraudCheck(text: String, uid: String): AppResult<Boolean> = try {
        val result = functions
            .getHttpsCallable("detectFraud")
            .call(mapOf("uid" to uid, "text" to text))
            .await()
        val flagged = (result.data as? Map<*, *>)?.get("flagged") as? Boolean ?: false
        AppResult.Success(flagged)
    } catch (t: Throwable) {
        AppResult.Error(t.readableMessage(), t)
    }
}
