package com.skillexchange.data.remote

import com.google.firebase.firestore.DocumentSnapshot

inline fun <reified T> DocumentSnapshot.toModelWithId(): T? {
    val model = toObject(T::class.java) ?: return null
    return when (model) {
        is com.skillexchange.domain.model.NeedPost -> model.copy(id = id) as T
        is com.skillexchange.domain.model.SwapOffer -> model.copy(id = id) as T
        is com.skillexchange.domain.model.ChatMessage -> model.copy(id = id) as T
        is com.skillexchange.domain.model.Rating -> model.copy(id = id) as T
        is com.skillexchange.domain.model.UserProfile -> model.copy(uid = id) as T
        else -> model
    }
}
