package com.akdag.inseminationtrackerapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.platform.LocalContext

class CowDataActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    // İzin istemek için metot
    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // İzin verilmemişse, izin isteyin
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestNotificationPermission()

        auth = FirebaseAuth.getInstance()

        setContent {
            CowDataScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CowDataScreen() {
        val context = LocalContext.current // Composable içinde context almak için

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tohumlama İşlemleri") },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Anasayfaya yönlendirme işlemi
                            val intent = Intent(context, HomeActivity::class.java)
                            context.startActivity(intent)  // Intent ile activity başlatma
                        }) {
                            Icon(Icons.Filled.Home, contentDescription = "Anasayfa")
                        }
                    }
                )
            },
            content = { paddingValues ->
                CowDataForm(paddingValues)
            }
        )
    }

    @Composable
    fun CowDataForm(paddingValues: PaddingValues) {
        var earTag by remember { mutableStateOf("") }
        var inseminationDate by remember { mutableStateOf("") }
        var cowsList by remember { mutableStateOf(listOf<Triple<String, List<InseminationRecord>, Boolean>>()) }

        // Arama bölmesi için durum
        var searchQuery by remember { mutableStateOf("") }
        var filteredCowsList by remember { mutableStateOf(listOf<Triple<String, List<InseminationRecord>, Boolean>>()) }
        val context = LocalContext.current

        // Veri yenileme fonksiyonu
        fun refreshCowData() {
            fetchCowData { data ->
                cowsList = data
                filteredCowsList = data
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Scaffold'dan gelen paddingValues burada kullanılıyor
                .padding(16.dp)
        ) {
            // Küpe numarası girişi
            TextField(
                value = earTag,
                onValueChange = { earTag = it },
                label = { Text("Küpe Numarası") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Takvim seçimi için DatePickerButton
            DatePickerButton(
                label = "Tohumlama Tarihi (gg.aa.yyyy)",
                selectedDate = inseminationDate,
                onDateSelected = { inseminationDate = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Veri ekleme butonu
            Button(
                onClick = {
                    if (isValidDate(inseminationDate)) {
                        addCowData(earTag, inseminationDate) {
                            refreshCowData()
                        }
                    } else {

                        Toast.makeText(context, "Geçerli bir tarih girin (gg.aa.yyyy)", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Veriyi Kaydet")
            }

            Spacer(modifier = Modifier.height(16.dp))



            // Arama bölmesi
            TextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    filteredCowsList = if (query.isNotEmpty()) {
                        cowsList.filter { it.first.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) }
                    } else {
                        cowsList
                    }
                },
                label = { Text("Küpe Numarasına Göre Ara") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Verileri göster butonu
            Button(
                onClick = { refreshCowData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verileri Göster")
            }

            // Verilerin gösterildiği alan
            CowDataDisplay(
                cowsList = filteredCowsList,
                onDelete = { earTag ->
                    deleteCowData(earTag) { refreshCowData() }
                },
                onMarkFailed = { earTag, record ->
                    markInseminationFailed(earTag, record) { refreshCowData() }
                },
                onMarkSuccessful = { earTag, record ->
                    markInseminationSuccessful(earTag, record) { refreshCowData() }
                }
            )
        }
    }

    // Takvim Seçimi İçin Buton
    @Composable
    fun DatePickerButton(
        label: String,
        selectedDate: String,
        onDateSelected: (String) -> Unit
    ) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        var showDatePicker by remember { mutableStateOf(false) }



        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            label = { Text("Seçilen Tarih") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { showDatePicker = true }) {
            Text("Tarih Seç")
        }

        if (showDatePicker) {
            DatePickerDialog(
                context,
                { _, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                    val formattedDate = String.format("%02d.%02d.%04d", selectedDayOfMonth, selectedMonth + 1, selectedYear)
                    onDateSelected(formattedDate)
                    showDatePicker = false
                },
                year,
                month,
                day
            ).show()
        }
    }

    // InseminationRecord veri modeli
    data class InseminationRecord(val date: Date, val status: String)

    // Tarih doğrulama fonksiyonu
    private fun isValidDate(dateStr: String): Boolean {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        dateFormat.isLenient = false
        return try {
            val date = dateFormat.parse(dateStr)
            date != null && date.before(Date())
        } catch (e: ParseException) {
            false
        }
    }

    // Veri ekleme işlemi
    private fun addCowData(earTag: String, inseminationDateStr: String, onComplete: () -> Unit) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            db.collection("Cows")
                .whereEqualTo("user_id", currentUser.uid)
                .whereEqualTo("ear_tag", earTag)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val cowDocument = documents.first()
                        val cowData = cowDocument.data
                        val inseminationRecords = cowData["insemination_records"] as? List<Map<String, Any>>

                        val lastRecord = inseminationRecords?.lastOrNull()
                        val lastStatus = lastRecord?.get("status") as? String

                        if (lastStatus == "Başarısız") {
                            saveNewInseminationRecord(cowDocument.id, earTag, inseminationDateStr) {
                                onComplete()
                            }
                        } else {
                            Toast.makeText(this, "Bu küpe numarasıyla daha önce başarılı bir tohumlama yapılmış!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
                        val inseminationDate: Date? = dateFormat.parse(inseminationDateStr)

                        if (inseminationDate != null) {
                            val cow = hashMapOf(
                                "user_id" to currentUser.uid,
                                "ear_tag" to earTag,
                                "insemination_records" to listOf(
                                    mapOf(
                                        "date" to Timestamp(inseminationDate),
                                        "status" to "Tohumlama Yapıldı"
                                    )
                                ),
                                "is_pregnant" to false
                            )

                            db.collection("Cows")
                                .add(cow)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Veri başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                                    onComplete()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Veri kaydedilemedi", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
        }
    }

    // Verileri gösterme işlemi
    private fun fetchCowData(callback: (List<Triple<String, List<InseminationRecord>, Boolean>>) -> Unit) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            db.collection("Cows")
                .whereEqualTo("user_id", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val cows = result.map { document ->
                        val earTag = document["ear_tag"].toString()
                        val records = document["insemination_records"] as List<Map<String, Any>>
                        val isPregnant = document["is_pregnant"] as Boolean

                        val inseminationRecords = records.map {
                            InseminationRecord(
                                (it["date"] as Timestamp).toDate(),
                                it["status"] as String
                            )
                        }

                        Triple(earTag, inseminationRecords, isPregnant)
                    }
                    callback(cows)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veri okunamadı", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Yeni bir tohumlama kaydını eklemek için kullanılan fonksiyon
    private fun saveNewInseminationRecord(
        documentId: String,
        earTag: String,
        inseminationDateStr: String,
        onComplete: () -> Unit
    ) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val inseminationDate: Date? = dateFormat.parse(inseminationDateStr)

        if (inseminationDate != null) {
            val newRecord = mapOf(
                "date" to Timestamp(inseminationDate),
                "status" to "Tohumlama Yapıldı"
            )

            db.collection("Cows").document(documentId)
                .update("insemination_records", FieldValue.arrayUnion(newRecord))
                .addOnSuccessListener {
                    Toast.makeText(this, "Yeni tohumlama kaydedildi", Toast.LENGTH_SHORT).show()
                    onComplete()  // Veri güncellendikten sonra listeyi yeniliyoruz
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Tohumlama kaydedilemedi", Toast.LENGTH_SHORT).show()
                }
        }
    }


    // Tohumlama başarısız işaretleme
    private fun markInseminationFailed(earTag: String, record: InseminationRecord, onComplete: () -> Unit) {
        updateInseminationStatus(earTag, record, "Başarısız", false, null, onComplete)
    }

    // Tohumlama başarılı işaretleme ve 195 gün sonrası kuruya çıkarma tarihi ekleme
    private fun markInseminationSuccessful(earTag: String, record: InseminationRecord, onComplete: () -> Unit) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            db.collection("Cows")
                .whereEqualTo("user_id", currentUser.uid)
                .whereEqualTo("ear_tag", earTag)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val calendar = Calendar.getInstance()
                        calendar.time = record.date
                        calendar.add(Calendar.DAY_OF_YEAR, 195) // 195 gün sonrası hesaplanıyor
                        val dryingOffDate = calendar.time

                        db.collection("Cows").document(document.id)
                            .update(
                                mapOf(
                                    "insemination_records" to FieldValue.arrayUnion(
                                        hashMapOf("date" to record.date, "status" to "Başarılı")
                                    ),
                                    "is_pregnant" to true,
                                    "drying_off_date" to Timestamp(dryingOffDate) // Kuruya çıkarma tarihi ekleniyor
                                )
                            )
                            .addOnSuccessListener {
                                // Schedule the notification after 195 days
                                val delayInMillis = dryingOffDate.time - System.currentTimeMillis()
                                // val delayInMillis = TimeUnit.MINUTES.toMillis(1)  // Test için 1 dakika gecikme
                                // val delayInMillis = dryingOffDate.time - System.currentTimeMillis()  // orjinal 195
                                enqueueCowBirthReminder(earTag, delayInMillis)

                                Toast.makeText(this, "Tohumlama başarılı olarak işaretlendi", Toast.LENGTH_SHORT).show()
                                onComplete()  // Başarılı işaretlendikten sonra listeyi yeniliyoruz
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Tohumlama başarılı olarak işaretlenemedi", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veri okunamadı", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Bildirim için iş planlama
    private fun enqueueCowBirthReminder(earTag: String, delayInMillis: Long) {
        val workRequest = OneTimeWorkRequestBuilder<CowBirthReminderWorker>()
            .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)  // 195 gün sonrasına ayarlanmış gecikme
            .setInputData(
                workDataOf("earTag" to earTag)  // Bildirim için küpe numarasını geçiyoruz
            )
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun updateInseminationStatus(
        earTag: String,
        record: InseminationRecord,
        newStatus: String,
        isPregnant: Boolean,
        expectedDeliveryDate: Date? = null,
        onComplete: () -> Unit,
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            db.collection("Cows")
                .whereEqualTo("user_id", currentUser.uid)
                .whereEqualTo("ear_tag", earTag)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val updatedRecord = mapOf(
                            "date" to Timestamp(record.date),
                            "status" to newStatus
                        )
                        val updates = hashMapOf(
                            "insemination_records" to FieldValue.arrayRemove(record),
                            "insemination_records" to FieldValue.arrayUnion(updatedRecord),
                            "is_pregnant" to isPregnant
                        )
                        if (isPregnant) {
                            updates["expected_delivery_date"] = Timestamp(expectedDeliveryDate!!)
                        }
                        db.collection("Cows").document(document.id)
                            .update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Tohumlama durumu güncellendi", Toast.LENGTH_SHORT).show()
                                onComplete()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Güncelleme başarısız", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veri okunamadı", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @Composable
    fun CowDataDisplay(
        cowsList: List<Triple<String, List<InseminationRecord>, Boolean>>,
        onDelete: (String) -> Unit,
        onMarkSuccessful: (String, InseminationRecord) -> Unit,
        onMarkFailed: (String, InseminationRecord) -> Unit
    ) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(cowsList) { (earTag, records, isPregnant) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = "Küpe Numarası: $earTag", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                        // Tohumlama kayıtlarını listeleme
                        records.forEach { record ->
                            Text(text = "Tohumlama Tarihi: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(record.date)} - ${record.status}")
                        }

                        // Gebelik durumu
                        Text(text = if (isPregnant) "Gebe" else "Gebe değil", style = MaterialTheme.typography.bodyLarge)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Eğer ineğin gebe olduğu tespit edildiyse, kuruya çıkarma tarihi gösteriliyor
                        if (isPregnant) {
                            val lastRecord = records.lastOrNull()
                            if (lastRecord != null) {
                                val calendar = Calendar.getInstance()
                                calendar.time = lastRecord.date
                                calendar.add(Calendar.DAY_OF_YEAR, 195)
                                val dryingOffDate = calendar.time
                                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                Text(text = "Kuruya Çıkarma Tarihi: ${dateFormat.format(dryingOffDate)}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sil ve Başarılı/Başarısız butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp) // Butonlar arası boşluk eklendi
                        ) {
                            Button(
                                onClick = { onDelete(earTag) },
                                modifier = Modifier.weight(1f), // Butonları eşit genişlikte yapıyoruz
                                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                            ) {
                                Text(text = "Sil")
                            }

                            Button(
                                onClick = { onMarkSuccessful(earTag, records.last()) },
                                modifier = Modifier.weight(1.5f), // Butonları eşit genişlikte yapıyoruz
                                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                            ) {
                                Text(text = "Başarılı")
                            }

                            Button(
                                onClick = { onMarkFailed(earTag, records.last()) },
                                modifier = Modifier.weight(1.7f), // Butonları eşit genişlikte yapıyoruz
                                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(text = "Başarısız")
                            }
                        }
                    }
                }
            }
        }
    }


    private fun deleteCowData(earTag: String, onComplete: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("Cows")
                .whereEqualTo("user_id", currentUser.uid)
                .whereEqualTo("ear_tag", earTag)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        db.collection("Cows").document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Veri başarıyla silindi", Toast.LENGTH_SHORT).show()
                                onComplete()  // Silme işleminden sonra listeyi yeniliyoruz
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Veri silinemedi", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }
    }
}