package com.akdag.inseminationtrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class VaccineActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            VaccineScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VaccineScreen() {
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Aşı İşlemleri") },
                    navigationIcon = {
                        IconButton(onClick = {
                            val intent = Intent(context, HomeActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Filled.Home, contentDescription = "Anasayfa")
                        }
                    }
                )
            },
            content = { paddingValues ->
                VaccineForm(paddingValues)
            }
        )
    }

    @Composable
    fun VaccineForm(paddingValues: PaddingValues) {
        var earTag by remember { mutableStateOf("") }
        var vaccineName by remember { mutableStateOf("") }
        var vaccinationDate by remember { mutableStateOf("") }
        var cowVaccineList by remember { mutableStateOf(listOf<Triple<String, List<VaccineRecord>, Boolean>>()) }
        var filteredVaccineList by remember { mutableStateOf(listOf<Triple<String, List<VaccineRecord>, Boolean>>()) }
        var context = LocalContext.current

        var isExpanded by remember { mutableStateOf(false) } // Bu durum yukarı kaydırıldığında formu gizleyecek

        // Veri yenileme fonksiyonu
        fun refreshVaccineData() {
            fetchVaccineData { data ->
                cowVaccineList = data
                filteredVaccineList = data  // Başlangıçta tüm verileri göster
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!isExpanded) {
                // Yukarı kaydırılmadıysa formu göster
                TextField(
                    value = earTag,
                    onValueChange = { earTag = it },
                    label = { Text("Küpe Numarası") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = vaccineName,
                    onValueChange = { vaccineName = it },
                    label = { Text("Yapılan Aşı") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                DatePickerButton(
                    label = "Aşının Yapıldığı Tarih (gg.aa.yyyy)",
                    selectedDate = vaccinationDate,
                    onDateSelected = { vaccinationDate = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isValidDate(vaccinationDate)) {
                            addVaccineData(earTag, vaccineName, vaccinationDate) {
                                refreshVaccineData()
                            }
                        } else {
                            Toast.makeText(context, "Geçerli bir tarih girin (gg.aa.yyyy)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aşıyı Kaydet")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { refreshVaccineData()
                    isExpanded = !isExpanded },  // Butona basıldığında genişleme durumunu tersine çevir
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isExpanded) {
                    Text("Formu Göster")  // Eğer genişleme durumu aktifse, formu tekrar göstermek için buton
                } else {
                    Text("Verileri Göster ve Yukarı Kaydır")  // Aksi halde verileri göster ve yukarı kaydır
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isExpanded) {
                // Eğer genişleme durumu aktifse, veriler gösterilsin
                TextField(
                    value = earTag,
                    onValueChange = { query ->
                        earTag = query
                        filteredVaccineList = if (query.isNotEmpty()) {
                            // Büyük/küçük harf duyarlılığı ile küpe numarasına göre filtrele
                            cowVaccineList.filter {
                                it.first.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
                            }
                        } else {
                            cowVaccineList  // Eğer boş ise tüm verileri göster
                        }
                    },
                    label = { Text("Küpe Numarasına Göre Ara") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                VaccineDataDisplay(filteredVaccineList)  // Filtrelenmiş listeyi göster
            }
        }
    }



    // Aşı Bilgilerini Firestore'a Ekleme Fonksiyonu
    private fun addVaccineData(earTag: String, vaccineName: String, vaccinationDateStr: String, onComplete: () -> Unit) {
        val currentUser = auth.currentUser
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val vaccinationDate: Date? = dateFormat.parse(vaccinationDateStr)

        if (currentUser != null && vaccinationDate != null) {
            val vaccineRecord = mapOf(
                "vaccine_name" to vaccineName,
                "date" to Timestamp(vaccinationDate)
            )

            // Vaccinations koleksiyonuna ekleme veya güncelleme
            db.collection("Vaccinations")
                .whereEqualTo("user_id", currentUser.uid)
                .whereEqualTo("ear_tag", earTag)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // İnek zaten varsa, vaccination_records alanına yeni aşı ekle
                        val document = documents.first()
                        db.collection("Vaccinations").document(document.id)
                            .update("vaccination_records", FieldValue.arrayUnion(vaccineRecord))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Aşı kaydedildi", Toast.LENGTH_SHORT).show()
                                onComplete()
                            }
                    } else {
                        // İnek yoksa yeni bir kayıt oluştur
                        val cowData = hashMapOf(
                            "user_id" to currentUser.uid,
                            "ear_tag" to earTag,
                            "vaccination_records" to listOf(vaccineRecord)
                        )

                        db.collection("Vaccinations")
                            .add(cowData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "İnek ve aşı kaydedildi", Toast.LENGTH_SHORT).show()
                                onComplete()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veri kaydedilemedi", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Aşı Bilgilerini Veritabanından Alma Fonksiyonu
    private fun fetchVaccineData(callback: (List<Triple<String, List<VaccineRecord>, Boolean>>) -> Unit) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            db.collection("Vaccinations")
                .whereEqualTo("user_id", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val cows = result.map { document ->
                        val earTag = document["ear_tag"].toString()
                        val records = document["vaccination_records"] as? List<Map<String, Any>> ?: emptyList()

                        // Aşıları tarih sırasına göre sıralıyoruz (en yeni en altta görünecek şekilde)
                        val sortedVaccineRecords = records.map {
                            VaccineRecord(
                                vaccineName = it["vaccine_name"].toString(),
                                date = (it["date"] as Timestamp).toDate()
                            )
                        }.sortedBy { it.date }  // Tarihe göre sıralama yapıyoruz

                        Triple(earTag, sortedVaccineRecords, false)
                    }
                    callback(cows)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veri okunamadı", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // DatePicker Button (Tarih Seçimi)
    @OptIn(ExperimentalMaterial3Api::class)
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

        var showDatePicker by remember { mutableStateOf(false) } // Takvim durumunu yöneten state
        var lastSelectedDate by remember { mutableStateOf(selectedDate) } // İptal sonrası yeniden açma kontrolü

        OutlinedTextField(
            value = lastSelectedDate,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { showDatePicker = true }) {
            Text("Tarih Seç")
        }

        if (showDatePicker) {
            android.app.DatePickerDialog(
                context,
                { _, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                    val formattedDate = String.format(
                        "%02d.%02d.%04d",
                        selectedDayOfMonth,
                        selectedMonth + 1,
                        selectedYear
                    )
                    onDateSelected(formattedDate)
                    lastSelectedDate = formattedDate
                    showDatePicker = false
                },
                year,
                month,
                day
            ).apply {
                setOnCancelListener {
                    // İptal durumunda takvimin tekrar açılabilmesi için reset
                    showDatePicker = false
                }
                show()
            }
        }
    }

    // Aşı Kayıtları Veri Modeli
    data class VaccineRecord(val vaccineName: String, val date: Date)

    // Aşı Verilerini Gösterme Alanı
    @Composable
    fun VaccineDataDisplay(cowsList: List<Triple<String, List<VaccineRecord>, Boolean>>) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(cowsList) { (earTag, records, _) ->
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
                        Text(
                            text = "Küpe Numarası: $earTag",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        records.forEach { record ->
                            Text(
                                text = "Aşı Adı: ${record.vaccineName}, Aşı Tarihi: ${
                                    SimpleDateFormat(
                                        "dd.MM.yyyy",
                                        Locale.getDefault()
                                    ).format(record.date)
                                }"
                            )
                        }
                    }
                }
            }
        }
    }

    // Tarih Doğrulama Fonksiyonu
    private fun isValidDate(dateStr: String): Boolean {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        return try {
            dateFormat.isLenient = false
            dateFormat.parse(dateStr) != null
        } catch (e: Exception) {
            false
        }
    }
}