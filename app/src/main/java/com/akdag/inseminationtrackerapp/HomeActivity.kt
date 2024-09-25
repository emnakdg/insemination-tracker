package com.akdag.inseminationtrackerapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // SharedPreferences başlatma
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        setContent {
            HomeScreen(
                onLogout = {
                    logoutUser()
                }
            )
        }
    }

    private fun logoutUser() {
        // Firebase'den çıkış yap
        auth.signOut()

        // "Beni Hatırla" özelliği varsa onu temizle
        sharedPreferences.edit().remove("email").remove("password").apply()

        // Giriş ekranına yönlendir
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Mevcut activity'yi kapat
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Tohumlama işlemleri butonu
        Button(
            onClick = {
                val intent = Intent(context, CowDataActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Tohumlama İşlemleri")
        }

        // Aşı işlemleri butonu
        Button(
            onClick = {
                // Henüz işlev eklenmedi, daha sonra eklenecek
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Aşı İşlemleri")
        }

        // Çıkış yap butonu
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Çıkış Yap")
        }
    }
}
