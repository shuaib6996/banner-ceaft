package com.example.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AuthState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val isLoggedIn: Boolean get() = user != null && !user.isAnonymous
    val isGuest: Boolean get() = user != null && user.isAnonymous
    val displayName: String
        get() = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: if (isGuest) "Guest Creator" else "Creator"
    val email: String get() = user?.email.orEmpty()
    val photoUrl: String? get() = user?.photoUrl?.toString()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _state = MutableStateFlow(AuthState(user = FirebaseAuth.getInstance().currentUser))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _state.update { it.copy(user = firebaseAuth.currentUser) }
    }

    init {
        try {
            auth.addAuthStateListener(authListener)
        } catch (_: Exception) {
            // Ignore if Firebase not yet initialized
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            auth.removeAuthStateListener(authListener)
        } catch (_: Exception) {}
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit = {}) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                }
                _state.update { it.copy(isLoading = false, successMessage = "Welcome back, ${state.value.displayName}!") }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit = {}) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }
        if (cleanPass.length < 6) {
            _state.update { it.copy(errorMessage = "Password must be at least 6 characters long.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                }

                if (cleanName.isNotBlank() && result.user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()
                    withContext(Dispatchers.IO) {
                        result.user!!.updateProfile(profileUpdates).await()
                    }
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        user = auth.currentUser,
                        successMessage = "Account created successfully! Welcome, $cleanName!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun signInAsGuest(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    auth.signInAnonymously().await()
                }
                _state.update { it.copy(isLoading = false, successMessage = "Continuing as Guest") }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter your email to reset password.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    auth.sendPasswordResetEmail(cleanEmail).await()
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Password reset link sent to $cleanEmail."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        try {
            auth.signOut()
            _state.update { AuthState(user = null, successMessage = "Signed out successfully.") }
            onSignedOut()
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = "Sign out failed: ${e.message}") }
        }
    }

    private fun formatAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL", "invalid-email" -> "Invalid email address format."
                    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "invalid-credential" -> "Incorrect email or password. Please verify your details."
                    "ERROR_EMAIL_ALREADY_IN_USE", "email-already-in-use" -> "An account with this email already exists."
                    "ERROR_WEAK_PASSWORD", "weak-password" -> "Password is too weak. Please use at least 6 characters."
                    "ERROR_USER_DISABLED", "user-disabled" -> "This account has been disabled."
                    "ERROR_TOO_MANY_REQUESTS", "too-many-requests" -> "Too many attempts. Please try again later."
                    else -> e.localizedMessage ?: "Authentication failed (${e.errorCode})"
                }
            }
            else -> e.localizedMessage ?: "Something went wrong. Please check your internet connection."
        }
    }
}
