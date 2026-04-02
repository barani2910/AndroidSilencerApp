package com.example.androidsilencerapp.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthViewModel"
    
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _user.value = auth.currentUser
    }

    fun login(email: String, pass: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                _user.value = result.user
            } catch (e: Exception) {
                Log.e(TAG, "Login Error: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(name: String, email: String, pass: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                _user.value = result.user
                Log.d(TAG, "SignUp Success: ${result.user?.uid}")
            } catch (e: Exception) {
                Log.e(TAG, "SignUp Error: ${e.message}", e)
                if (e is FirebaseAuthException) {
                    _error.value = "Auth Error: ${e.errorCode} - ${e.message}"
                } else {
                    _error.value = e.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
