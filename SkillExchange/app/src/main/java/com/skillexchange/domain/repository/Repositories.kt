package com.skillexchange.domain.repository

import android.app.Activity
import com.skillexchange.core.AppResult
import com.skillexchange.domain.model.ChatMessage
import com.skillexchange.domain.model.MatchRecommendation
import com.skillexchange.domain.model.NeedPost
import com.skillexchange.domain.model.Rating
import com.skillexchange.domain.model.SwapOffer
import com.skillexchange.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    fun observeAuthState(): Flow<String?>
    suspend fun sendOtp(activity: Activity, phoneNumber: String): AppResult<String>
    suspend fun verifyOtp(verificationId: String, otp: String): AppResult<String>
    suspend fun signOut()
}

interface UserRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun upsertProfile(profile: UserProfile): AppResult<Unit>
    suspend fun saveFcmToken(uid: String, token: String): AppResult<Unit>
}

interface PostRepository {
    fun observeOpenPosts(skill: String? = null): Flow<List<NeedPost>>
    fun observeMyPosts(uid: String): Flow<List<NeedPost>>
    suspend fun createPost(post: NeedPost): AppResult<String>
}

interface SwapRepository {
    fun observeOffersForUser(uid: String): Flow<List<SwapOffer>>
    suspend fun createOffer(offer: SwapOffer): AppResult<String>
    suspend fun acceptOffer(offerId: String): AppResult<Unit>
    suspend fun confirmComplete(offerId: String, uid: String): AppResult<Unit>
    suspend fun rateSwap(rating: Rating): AppResult<Unit>
}

interface ChatRepository {
    fun observeMessages(threadId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(threadId: String, message: ChatMessage): AppResult<Unit>
}

interface GenAiRepository {
    suspend fun recommendations(uid: String, skill: String?, village: String?): AppResult<List<MatchRecommendation>>
    suspend fun fraudCheck(text: String, uid: String): AppResult<Boolean>
}
