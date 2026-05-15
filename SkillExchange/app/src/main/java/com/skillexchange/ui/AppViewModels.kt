package com.skillexchange.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skillexchange.core.AppResult
import com.skillexchange.di.AppContainer
import com.skillexchange.domain.model.MatchRecommendation
import com.skillexchange.domain.model.ChatMessage
import com.skillexchange.domain.model.NeedPost
import com.skillexchange.domain.model.SwapOffer
import com.skillexchange.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val uid: String? = null,
    val verificationId: String = "",
    val phone: String = "",
    val otp: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    private val localState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = combine(
        localState,
        container.authRepository.observeAuthState()
    ) { local, uid -> local.copy(uid = uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState())

    fun setPhone(value: String) = localState.update { it.copy(phone = value) }
    fun setOtp(value: String) = localState.update { it.copy(otp = value) }

    fun sendOtp(activity: Activity) = viewModelScope.launch {
        localState.update { it.copy(loading = true, error = null) }
        when (val result = container.authRepository.sendOtp(activity, state.value.phone)) {
            is AppResult.Success -> localState.update { it.copy(verificationId = result.data, loading = false) }
            is AppResult.Error -> localState.update { it.copy(error = result.message, loading = false) }
            AppResult.Loading -> Unit
        }
    }

    fun verifyOtp() = viewModelScope.launch {
        localState.update { it.copy(loading = true, error = null) }
        when (val result = container.authRepository.verifyOtp(state.value.verificationId, state.value.otp)) {
            is AppResult.Success -> localState.update { it.copy(loading = false) }
            is AppResult.Error -> localState.update { it.copy(error = result.message, loading = false) }
            AppResult.Loading -> Unit
        }
    }
}

data class HomeUiState(
    val selectedSkill: String? = null,
    val profile: UserProfile? = null,
    val posts: List<NeedPost> = emptyList(),
    val recommendations: List<MatchRecommendation> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedSkill = MutableStateFlow<String?>(null)
    private val recommendations = MutableStateFlow<List<MatchRecommendation>>(emptyList())
    private val uid = container.authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.authRepository.currentUserId)

    val state: StateFlow<HomeUiState> = combine(
        selectedSkill,
        selectedSkill.flatMapLatest { container.postRepository.observeOpenPosts(it) },
        uid.flatMapLatest { id -> if (id == null) flowOf(null) else container.userRepository.observeProfile(id) },
        recommendations
    ) { skill, posts, profile, recs ->
        HomeUiState(selectedSkill = skill, posts = posts, profile = profile, recommendations = recs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectSkill(skill: String?) = selectedSkill.update { skill }

    fun saveProfile(name: String, village: String, skills: List<String>, language: String) = viewModelScope.launch {
        val currentUid = uid.value ?: return@launch
        val phone = state.value.profile?.phoneNumber.orEmpty()
        container.userRepository.upsertProfile(
            UserProfile(
                uid = currentUid,
                phoneNumber = phone,
                displayName = name,
                village = village,
                skills = skills,
                languageCode = language
            )
        )
    }

    fun createPost(title: String, description: String, required: String, offered: String, hours: Int, village: String) =
        viewModelScope.launch {
            val currentUid = uid.value ?: return@launch
            val flagged = container.genAiRepository.fraudCheck("$title $description", currentUid)
            if (flagged is AppResult.Success && flagged.data) return@launch
            container.postRepository.createPost(
                NeedPost(
                    ownerId = currentUid,
                    title = title,
                    description = description,
                    requiredSkill = required,
                    offeredSkill = offered,
                    estimatedHours = hours,
                    village = village.ifBlank { state.value.profile?.village.orEmpty() },
                    languageCode = state.value.profile?.languageCode ?: "en"
                )
            )
        }

    fun loadRecommendations() = viewModelScope.launch {
        val currentUid = uid.value ?: return@launch
        val profile = state.value.profile
        when (val result = container.genAiRepository.recommendations(currentUid, selectedSkill.value, profile?.village)) {
            is AppResult.Success -> recommendations.value = result.data
            else -> Unit
        }
    }
}

data class SwapUiState(
    val offers: List<SwapOffer> = emptyList(),
    val error: String? = null
)

class SwapViewModel(private val container: AppContainer) : ViewModel() {
    private val uid = container.authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.authRepository.currentUserId)

    val state: StateFlow<SwapUiState> = uid.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.swapRepository.observeOffersForUser(id)
    }.combine(flowOf(null as String?)) { offers, error -> SwapUiState(offers, error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwapUiState())

    fun offer(post: NeedPost, message: String) = viewModelScope.launch {
        val currentUid = uid.value ?: return@launch
        container.swapRepository.createOffer(
            SwapOffer(
                postId = post.id,
                fromUserId = currentUid,
                toUserId = post.ownerId,
                offeredSkill = post.offeredSkill,
                requestedSkill = post.requiredSkill,
                estimatedHours = post.estimatedHours,
                message = message
            )
        )
    }

    fun accept(offerId: String) = viewModelScope.launch { container.swapRepository.acceptOffer(offerId) }
    fun confirm(offerId: String) = viewModelScope.launch { uid.value?.let { container.swapRepository.confirmComplete(offerId, it) } }
}

data class ChatUiState(
    val threadId: String = "",
    val messages: List<ChatMessage> = emptyList()
)

class ChatViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedThreadId = MutableStateFlow("")
    private val uid = container.authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.authRepository.currentUserId)

    val state: StateFlow<ChatUiState> = selectedThreadId.flatMapLatest { threadId ->
        if (threadId.isBlank()) flowOf(ChatUiState())
        else container.chatRepository.observeMessages(threadId).combine(flowOf(threadId)) { messages, id ->
            ChatUiState(threadId = id, messages = messages)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun open(threadId: String) = selectedThreadId.update { threadId }

    fun send(text: String) = viewModelScope.launch {
        val senderId = uid.value ?: return@launch
        val threadId = state.value.threadId
        if (threadId.isBlank() || text.isBlank()) return@launch
        container.chatRepository.sendMessage(
            threadId,
            ChatMessage(threadId = threadId, senderId = senderId, text = text)
        )
    }
}

class SkillExchangeViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AuthViewModel::class.java -> AuthViewModel(container)
        HomeViewModel::class.java -> HomeViewModel(container)
        SwapViewModel::class.java -> SwapViewModel(container)
        ChatViewModel::class.java -> ChatViewModel(container)
        else -> error("Unknown ViewModel ${modelClass.simpleName}")
    } as T
}
