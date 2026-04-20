package com.ordemservico.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ordemservico.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val userId: String? = null
)

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    init {
        val user = repo.currentUser()
        if (user != null) {
            _state.value = AuthUiState(isLoggedIn = true, userId = user.id)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repo.signIn(email, password)
                .onSuccess {
                    _state.value = AuthUiState(
                        isLoggedIn = true,
                        userId = repo.currentUserId()
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message ?: "Erro ao fazer login"
                    )
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repo.signUp(email, password)
                .onSuccess {
                    _state.value = AuthUiState(
                        isLoggedIn = true,
                        userId = repo.currentUserId()
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message ?: "Erro ao criar conta"
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repo.signOut()
            _state.value = AuthUiState(isLoggedIn = false)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
