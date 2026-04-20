package com.ordemservico.app.data.repository

import com.ordemservico.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo

class AuthRepository {

    private val auth = SupabaseClient.client.auth

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    fun currentUser(): UserInfo? = auth.currentUserOrNull()

    fun currentUserId(): String? = auth.currentUserOrNull()?.id
}
