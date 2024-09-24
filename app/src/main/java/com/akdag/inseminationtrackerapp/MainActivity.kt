package com.akdag.inseminationtrackerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        setContent {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf("") }
            var successMessage by remember { mutableStateOf("") }

            // UI yapısı
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // E-posta alanı
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-posta") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Şifre alanı
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Şifre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = {
                            passwordVisible = !passwordVisible
                        }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Giriş yap butonu
                Button(
                    onClick = {
                        loginUser(email, password) { success, error ->
                            if (success) {
                                // Başarılı giriş, verileri yönetme ekranına geçiş
                                val intent = Intent(this@MainActivity, CowDataActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                errorMessage = error ?: "Giriş başarısız!"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Giriş Yap", color = MaterialTheme.colorScheme.onPrimary)
                }

                // Kayıt ol butonu
                Button(
                    onClick = {
                        registerUser(email, password) { success, error ->
                            if (success) {
                                successMessage = "Kayıt başarılı! Şimdi giriş yapabilirsiniz."
                                errorMessage = ""
                            } else {
                                errorMessage = error ?: "Kayıt başarısız!"
                                successMessage = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Kayıt Ol", color = MaterialTheme.colorScheme.onPrimary)
                }

                // Hata mesajı gösterimi
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }

                // Başarı mesajı gösterimi
                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = successMessage, color = MaterialTheme.colorScheme.primary)
                }
            }

            // FCM Token'ı almak
            getFCMToken()
        }
    }

    // Giriş işlemi
    private fun loginUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Login", "Giriş başarılı")
                    callback(true, null)
                } else {
                    Log.w("Login", "Giriş başarısız", task.exception)
                    callback(false, task.exception?.message)
                }
            }
    }

    // Kayıt işlemi
    private fun registerUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Register", "Kayıt başarılı")
                    callback(true, null)
                } else {
                    Log.w("Register", "Kayıt başarısız", task.exception)
                    callback(false, task.exception?.message)
                }
            }
    }

    // FCM Token alma işlemi
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "FCM token alınamadı", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "FCM Token: $token")
        }
    }
}
