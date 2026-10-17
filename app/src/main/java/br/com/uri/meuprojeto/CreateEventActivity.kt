package br.com.uri.meuprojeto

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateEventActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnBack: ImageButton
    private lateinit var etEventTitle: EditText
    private lateinit var etEventDescription: EditText
    private lateinit var etEventDate: EditText
    private lateinit var etEventLocation: EditText
    private lateinit var spinnerEventCategory: Spinner
    private lateinit var etMaxParticipants: EditText

    private lateinit var cardCoverPicker: MaterialCardView
    private lateinit var ivCoverPreview: ImageView
    private lateinit var btnSelectCoverImage: MaterialButton
    private lateinit var tvCoverStatus: TextView

    private lateinit var btnSaveEvent: MaterialButton
    private lateinit var pbSaveEvent: ProgressBar

    private val calendar = Calendar.getInstance()
    private var selectedLocalImagePath: String = ""

    private val categories = listOf(
        "Tecnologia",
        "Inteligência Artificial",
        "Design",
        "Hackathon",
        "Backend & Cloud",
        "Outro"
    )

    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveImageLocally(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_create_event)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnBack = findViewById(R.id.btnBack)
        etEventTitle = findViewById(R.id.etEventTitle)
        etEventDescription = findViewById(R.id.etEventDescription)
        etEventDate = findViewById(R.id.etEventDate)
        etEventLocation = findViewById(R.id.etEventLocation)
        spinnerEventCategory = findViewById(R.id.spinnerEventCategory)
        etMaxParticipants = findViewById(R.id.etMaxParticipants)

        cardCoverPicker = findViewById(R.id.cardCoverPicker)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        btnSelectCoverImage = findViewById(R.id.btnSelectCoverImage)
        tvCoverStatus = findViewById(R.id.tvCoverStatus)

        btnSaveEvent = findViewById(R.id.btnSaveEvent)
        pbSaveEvent = findViewById(R.id.pbSaveEvent)

        btnBack.setOnClickListener { finish() }

        setupCategorySpinner()
        setupNativeDateTimePicker()

        val openGalleryListener = View.OnClickListener {
            pickGalleryLauncher.launch("image/*")
        }
        cardCoverPicker.setOnClickListener(openGalleryListener)
        btnSelectCoverImage.setOnClickListener(openGalleryListener)

        btnSaveEvent.setOnClickListener {
            saveEvent()
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            categories
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        spinnerEventCategory.adapter = adapter
    }

    private fun setupNativeDateTimePicker() {
        // Pré-define com a data e horário atual
        updateDateInEditText()

        etEventDate.setOnClickListener {
            showNativeDatePicker()
        }
    }

    private fun showNativeDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay)

            // Após selecionar a data, abre o seletor nativo de horário
            showNativeTimePicker()
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun showNativeTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            updateDateInEditText()
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun updateDateInEditText() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        etEventDate.setText(dateFormat.format(calendar.time))
    }

    private fun saveImageLocally(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val coversDir = File(filesDir, "event_covers").apply { if (!exists()) mkdirs() }
                val targetFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")

                FileOutputStream(targetFile).use { output ->
                    inputStream.copyTo(output)
                }

                selectedLocalImagePath = targetFile.absolutePath

                ivCoverPreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(targetFile)
                    .into(ivCoverPreview)

                tvCoverStatus.text = "Foto de capa selecionada e salva localmente!"
                tvCoverStatus.setTextColor(getColor(R.color.primary_dark))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao carregar imagem da galeria: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveEvent() {
        val title = etEventTitle.text.toString().trim()
        val description = etEventDescription.text.toString().trim()
        val date = etEventDate.text.toString().trim()
        val location = etEventLocation.text.toString().trim()
        val category = spinnerEventCategory.selectedItem?.toString() ?: "Tecnologia"
        val maxParticipantsStr = etMaxParticipants.text.toString().trim()

        if (title.isEmpty()) {
            etEventTitle.error = "Informe o título do evento"
            etEventTitle.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etEventDescription.error = "Informe a descrição"
            etEventDescription.requestFocus()
            return
        }

        if (date.isEmpty()) {
            etEventDate.error = "Informe a data e horário"
            etEventDate.requestFocus()
            return
        }

        if (location.isEmpty()) {
            etEventLocation.error = "Informe a localização"
            etEventLocation.requestFocus()
            return
        }

        if (maxParticipantsStr.isEmpty()) {
            etMaxParticipants.error = "Informe o limite de vagas"
            etMaxParticipants.requestFocus()
            return
        }

        val maxParticipants = maxParticipantsStr.toIntOrNull()
        if (maxParticipants == null || maxParticipants <= 0) {
            etMaxParticipants.error = "Informe um número válido maior que 0"
            etMaxParticipants.requestFocus()
            return
        }

        val user = auth.currentUser ?: return
        val creatorId = user.uid
        val creatorName = if (!user.displayName.isNullOrEmpty()) {
            user.displayName!!
        } else {
            user.email?.substringBefore("@") ?: "Usuário"
        }

        val defaultImage = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97"
        val finalImageUrl = if (selectedLocalImagePath.isNotEmpty()) selectedLocalImagePath else defaultImage

        btnSaveEvent.isEnabled = false
        pbSaveEvent.visibility = View.VISIBLE

        val userEventsRef = db.collection("events").document(creatorId).collection("user_events")
        val eventId = userEventsRef.document().id

        val newEvent = Event(
            id = eventId,
            creatorId = creatorId,
            creatorName = creatorName,
            title = title,
            description = description,
            date = date,
            location = location,
            category = category,
            maxParticipants = maxParticipants,
            subscriberCount = 0,
            favoriteCount = 0,
            commentCount = 0,
            status = "OPEN",
            averageRating = 0.0,
            ratingCount = 0,
            imageUrl = finalImageUrl,
            createdAt = System.currentTimeMillis()
        )

        userEventsRef.document(eventId).set(newEvent)
            .addOnSuccessListener {
                pbSaveEvent.visibility = View.GONE
                Toast.makeText(this, "Evento publicado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnSaveEvent.isEnabled = true
                pbSaveEvent.visibility = View.GONE
                Toast.makeText(this, "Erro ao cadastrar evento: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
