package com.skillexchange.di

import com.skillexchange.data.remote.FirebaseAuthRepository
import com.skillexchange.data.remote.FirebaseChatRepository
import com.skillexchange.data.remote.FirebaseGenAiRepository
import com.skillexchange.data.remote.FirebasePostRepository
import com.skillexchange.data.remote.FirebaseSwapRepository
import com.skillexchange.data.remote.FirebaseUserRepository
import com.skillexchange.domain.repository.AuthRepository
import com.skillexchange.domain.repository.ChatRepository
import com.skillexchange.domain.repository.GenAiRepository
import com.skillexchange.domain.repository.PostRepository
import com.skillexchange.domain.repository.SwapRepository
import com.skillexchange.domain.repository.UserRepository

class AppContainer {
    val authRepository: AuthRepository = FirebaseAuthRepository()
    val userRepository: UserRepository = FirebaseUserRepository()
    val postRepository: PostRepository = FirebasePostRepository()
    val swapRepository: SwapRepository = FirebaseSwapRepository()
    val chatRepository: ChatRepository = FirebaseChatRepository()
    val genAiRepository: GenAiRepository = FirebaseGenAiRepository()
}
