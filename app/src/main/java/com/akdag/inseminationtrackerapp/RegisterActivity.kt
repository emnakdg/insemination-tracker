package com.akdag.inseminationtrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            RegisterScreen(
                onRegisterSuccess = {
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                },
                onBackPressed = {
                    finish()  // Geri tuşuna basınca giriş ekranına dön
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit, // Kayıt başarılı olduğunda çağrılacak callback
    onBackPressed: () -> Unit      // Geri tuşuna basıldığında yapılacak işlem
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Geri tuşu simgesi
        IconButton(onClick = onBackPressed) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Geri Dön")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ad Soyad alanı
        TextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Ad Soyad") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage.contains("Adınızda numara olamaz")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // E-posta alanı
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = errorMessage.contains("Geçerli bir e-posta girin")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Şifre alanı
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorMessage.contains("Şifre en az 6 karakter olmalı")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Kayıt Ol Butonu
        Button(
            onClick = {
                errorMessage = ""

                // Ad Soyad doğrulama
                if (fullName.isBlank() || fullName.any { it.isDigit() }) {
                    errorMessage = "Adınızda numara olamaz. Geçerli bir ad girin."
                }
                // Email doğrulama
                else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Geçerli bir e-posta girin."
                }
                // Şifre doğrulama
                else if (password.length < 6) {
                    errorMessage = "Şifre en az 6 karakter olmalı."
                }
                // Tüm doğrulamalar başarılıysa kullanıcıyı kaydet
                else {
                    registerUser(fullName, email, password) { success, error ->
                        if (success) {
                            Toast.makeText(context, "Kayıt başarılı! Şimdi giriş yapabilirsiniz.", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            errorMessage = error ?: "Kayıt işlemi başarısız!"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kayıt Ol")
        }

        // Hata mesajı
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun registerUser(fullName: String, email: String, password: String, callback: (Boolean, String?) -> Unit) {
    val auth = FirebaseAuth.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, null)
            } else {
                callback(false, task.exception?.message)
            }
        }
}
