package com.skillexchange.domain.model

data class UserProfile(
    val uid: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val village: String = "",
    val languageCode: String = "en",
    val skills: List<String> = emptyList(),
    val trustScore: Double = 50.0,
    val completedSwaps: Int = 0,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class NeedPost(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val requiredSkill: String = "",
    val offeredSkill: String = "",
    val estimatedHours: Int = 1,
    val village: String = "",
    val languageCode: String = "en",
    val status: PostStatus = PostStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
) {
    val skillPoints: Int get() = estimatedHours
}

enum class PostStatus { OPEN, MATCHED, COMPLETED, CANCELLED }

data class SwapOffer(
    val id: String = "",
    val postId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val offeredSkill: String = "",
    val requestedSkill: String = "",
    val estimatedHours: Int = 1,
    val message: String = "",
    val status: OfferStatus = OfferStatus.PENDING,
    val fromConfirmedComplete: Boolean = false,
    val toConfirmedComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val skillPoints: Int get() = estimatedHours
}

enum class OfferStatus { PENDING, ACCEPTED, REJECTED, COMPLETED, DISPUTED }

data class ChatThread(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val postId: String = "",
    val offerId: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Rating(
    val id: String = "",
    val swapOfferId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val stars: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class MatchRecommendation(
    val postId: String = "",
    val title: String = "",
    val score: Double = 0.0,
    val reason: String = ""
)

data class FraudSignal(
    val id: String = "",
    val userId: String = "",
    val severity: String = "low",
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
