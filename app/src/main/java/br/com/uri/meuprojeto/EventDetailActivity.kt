package br.com.uri.meuprojeto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var eventId: String = ""
    private var currentEvent: Event? = null
    private var eventListener: ListenerRegistration? = null
    private var audiosListener: ListenerRegistration? = null

    // Main Header Views
    private lateinit var btnBack: ImageButton
    private lateinit var ivEventImage: ImageView
    private lateinit var btnFavorite: ImageButton
    private lateinit var tvCategory: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvRatingAverage: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCapacity: TextView

    private lateinit var btnSubscribe: MaterialButton
    private lateinit var btnRate: MaterialButton
    private lateinit var layoutComments: LinearLayout
    private lateinit var tvCommentCount: TextView

    // Tab Layout & Containers
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutTabLocation: MaterialCardView
    private lateinit var layoutTabAudios: LinearLayout

    // Tab 1: Local
    private lateinit var tvLocation: TextView
    private lateinit var btnOpenMaps: MaterialButton
    private lateinit var tvDescription: TextView

    // Tab 2: Áudios
    private lateinit var cardRecordAudio: MaterialCardView
    private lateinit var btnRecordAudio: MaterialButton
    private lateinit var tvRecordingStatus: TextView
    private lateinit var pbAudios: ProgressBar
    private lateinit var tvEmptyAudios: TextView
    private lateinit var rvAudios: RecyclerView
    private lateinit var audioAdapter: EventAudioAdapter

    // Audio Recorder & Player State
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecordingAudio: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingAudioId: String? = null

    private val requestAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            toggleAudioRecording()
        } else {
            Toast.makeText(this, "Permissão de gravação de áudio é necessária", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_event_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        eventId = intent.getStringExtra("EXTRA_EVENT_ID") ?: ""
        if (eventId.isEmpty()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadEventDetailsRealtime()
        loadEventAudiosRealtime()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivEventImage = findViewById(R.id.ivEventImage)
        btnFavorite = findViewById(R.id.btnFavorite)
        tvCategory = findViewById(R.id.tvCategory)
        tvStatus = findViewById(R.id.tvStatus)
        tvRatingAverage = findViewById(R.id.tvRatingAverage)
        tvTitle = findViewById(R.id.tvTitle)
        tvDate = findViewById(R.id.tvDate)
        tvCapacity = findViewById(R.id.tvCapacity)

        btnSubscribe = findViewById(R.id.btnSubscribe)
        btnRate = findViewById(R.id.btnRate)
        layoutComments = findViewById(R.id.layoutComments)
        tvCommentCount = findViewById(R.id.tvCommentCount)

        tabLayout = findViewById(R.id.tabLayout)
        layoutTabLocation = findViewById(R.id.layoutTabLocation)
        layoutTabAudios = findViewById(R.id.layoutTabAudios)

        // Tab 1: Local
        tvLocation = findViewById(R.id.tvLocation)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        tvDescription = findViewById(R.id.tvDescription)

        // Tab 2: Áudios
        cardRecordAudio = findViewById(R.id.cardRecordAudio)
        btnRecordAudio = findViewById(R.id.btnRecordAudio)
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus)
        pbAudios = findViewById(R.id.pbAudios)
        tvEmptyAudios = findViewById(R.id.tvEmptyAudios)
        rvAudios = findViewById(R.id.rvAudios)

        val currentUserId = auth.currentUser?.uid ?: ""
        rvAudios.layoutManager = LinearLayoutManager(this)
        audioAdapter = EventAudioAdapter(
            audios = emptyList(),
            currentUserId = currentUserId,
            onPlayClick = { audio -> handleAudioPlayPause(audio) },
            onDeleteClick = { audio -> confirmDeleteAudio(audio) }
        )
        rvAudios.adapter = audioAdapter

        if (auth.currentUser == null) {
            cardRecordAudio.visibility = View.GONE
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutTabLocation.visibility = View.VISIBLE
                        layoutTabAudios.visibility = View.GONE
                    }
                    1 -> {
                        layoutTabLocation.visibility = View.GONE
                        layoutTabAudios.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnOpenMaps.setOnClickListener {
            val location = currentEvent?.location ?: ""
            if (location.isNotEmpty()) {
                val uri = "geo:0,0?q=${Uri.encode(location)}".toUri()
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(mapIntent)
                } catch (_: Exception) {
                    Toast.makeText(this, "Nenhum aplicativo de mapas disponível", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Endereço não informado para este evento", Toast.LENGTH_SHORT).show()
            }
        }

        btnRecordAudio.setOnClickListener {
            checkAudioPermissionAndToggle()
        }

        layoutComments.setOnClickListener {
            CommentsBottomSheetFragment.newInstance(eventId).show(supportFragmentManager, "CommentsBottomSheetFragment")
        }

        btnFavorite.setOnClickListener { currentEvent?.let { toggleFavorite(it) } }
        btnSubscribe.setOnClickListener { currentEvent?.let { toggleSubscription(it) } }
        btnRate.setOnClickListener { currentEvent?.let { showRatingDialog(it) } }
    }

    // ==================== LÓGICA DE ÁUDIO ====================

    private fun checkAudioPermissionAndToggle() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            toggleAudioRecording()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun toggleAudioRecording() {
        if (!isRecordingAudio) {
            startAudioRecording()
        } else {
            stopAudioRecordingAndUpload()
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
    }

    private fun startAudioRecording() {
        try {
            val audiosDir = File(filesDir, "event_audios").apply { if (!exists()) mkdirs() }
            recordingFile = File(audiosDir, "audio_${System.currentTimeMillis()}.m4a")

            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
            }

            isRecordingAudio = true
            btnRecordAudio.text = "Parar Gravação"
            btnRecordAudio.setBackgroundColor("#FFD32F2F".toColorInt())
            tvRecordingStatus.text = "Gravando áudio..."
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao iniciar gravação: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAudioRecordingAndUpload() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecordingAudio = false

            btnRecordAudio.text = "Gravar Áudio"
            btnRecordAudio.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_blue))
            tvRecordingStatus.text = "Clique para iniciar a gravação"

            if (recordingFile != null && recordingFile!!.exists()) {
                val user = auth.currentUser ?: return
                val audioId = db.collection("events").document(eventId).collection("audios").document().id
                val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"

                val audio = EventAudio(
                    id = audioId,
                    eventId = eventId,
                    userId = user.uid,
                    userName = userName,
                    audioPath = recordingFile!!.absolutePath,
                    createdAt = System.currentTimeMillis()
                )

                db.collection("events").document(eventId).collection("audios").document(audioId).set(audio)
                    .addOnSuccessListener { Toast.makeText(this, "Áudio publicado!", Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao finalizar gravação: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleAudioPlayPause(audio: EventAudio) {
        if (currentlyPlayingAudioId == audio.id) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlayingAudioId = null
            audioAdapter.setCurrentlyPlayingId(null)
            return
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audio.audioPath)
                prepare()
                start()
                setOnCompletionListener {
                    currentlyPlayingAudioId = null
                    audioAdapter.setCurrentlyPlayingId(null)
                }
            }

            currentlyPlayingAudioId = audio.id
            audioAdapter.setCurrentlyPlayingId(audio.id)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao reproduzir áudio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteAudio(audio: EventAudio) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Áudio")
            .setMessage("Deseja realmente excluir este áudio?")
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteAudio(audio)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteAudio(audio: EventAudio) {
        if (currentlyPlayingAudioId == audio.id) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlayingAudioId = null
            audioAdapter.setCurrentlyPlayingId(null)
        }

        // Apaga do Firestore
        db.collection("events").document(eventId).collection("audios").document(audio.id)
            .delete()
            .addOnSuccessListener {
                // Apaga o arquivo local
                try {
                    val file = File(audio.audioPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}

                Toast.makeText(this, "Áudio excluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir áudio: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadEventAudiosRealtime() {
        pbAudios.visibility = View.VISIBLE
        audiosListener = db.collection("events").document(eventId).collection("audios")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                pbAudios.visibility = View.GONE
                if (snapshot != null) {
                    val audios = snapshot.documents.mapNotNull { doc -> doc.toObject(EventAudio::class.java) }
                    tvEmptyAudios.visibility = if (audios.isEmpty()) View.VISIBLE else View.GONE
                    rvAudios.visibility = if (audios.isEmpty()) View.GONE else View.VISIBLE
                    audioAdapter.updateAudios(audios)
                }
            }
    }

    // ==================== OUTROS DADOS DO EVENTO ====================

    private fun loadEventDetailsRealtime() {
        eventListener = db.collection("events").document(eventId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val event = snapshot.toObject(Event::class.java)
                    if (event != null) {
                        currentEvent = event
                        bindEventData(event)
                    }
                }
            }
    }

    private fun bindEventData(event: Event) {
        val currentUserId = auth.currentUser?.uid ?: ""

        tvCategory.text = event.category
        tvTitle.text = event.title
        tvDate.text = event.date
        tvLocation.text = if (event.location.isNotEmpty()) event.location else "Endereço não informado"
        tvDescription.text = if (event.description.isNotEmpty()) event.description else "Sem descrição informada."

        val subscriberCount = event.subscribers.size
        tvCapacity.text = "$subscriberCount / ${event.maxParticipants} participantes inscritos"
        tvCommentCount.text = event.commentCount.toString()

        val isSubscribed = event.subscribers.contains(currentUserId)

        if (event.isEnded) {
            tvStatus.text = "Encerrado"
            tvStatus.setBackgroundColor("#FFD32F2F".toColorInt())
            val labelReviews = if (event.ratingCount == 1) "avaliação" else "avaliações"
            tvRatingAverage.text = String.format(Locale.getDefault(), "★ %.1f (%d %s)", event.averageRating, event.ratingCount, labelReviews)
            tvRatingAverage.visibility = View.VISIBLE
            btnSubscribe.visibility = View.GONE

            if (isSubscribed) {
                btnRate.visibility = View.VISIBLE
                val userRating = event.ratings[currentUserId]
                btnRate.text = if (userRating != null) String.format(Locale.getDefault(), "Sua Nota: %.1f ★", userRating) else "Avaliar Evento"
            } else {
                btnRate.visibility = View.GONE
            }
        } else {
            tvStatus.text = "Aberto"
            tvStatus.setBackgroundColor("#FF388E3C".toColorInt())
            tvRatingAverage.visibility = View.GONE
            btnRate.visibility = View.GONE
            btnSubscribe.visibility = View.VISIBLE
            btnSubscribe.text = if (isSubscribed) "Inscrito" else "Inscrever-se"
        }

        val isFavorite = event.favorites.contains(currentUserId)
        btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)

        if (event.imageUrl.isNotEmpty()) {
            Glide.with(this).load(event.imageUrl).placeholder(R.drawable.ic_launcher_background).error(R.drawable.ic_launcher_background).into(ivEventImage)
        } else {
            ivEventImage.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    private fun toggleSubscription(event: Event) {
        val userId = auth.currentUser?.uid ?: return
        val eventRef = db.collection("events").document(event.id)
        val isSubscribed = event.subscribers.contains(userId)

        if (isSubscribed) {
            eventRef.update("subscribers", FieldValue.arrayRemove(userId))
                .addOnSuccessListener { Toast.makeText(this, "Inscrição cancelada!", Toast.LENGTH_SHORT).show() }
        } else {
            if (event.subscribers.size >= event.maxParticipants && event.maxParticipants > 0) {
                Toast.makeText(this, "Evento com vagas esgotadas!", Toast.LENGTH_SHORT).show()
                return
            }
            eventRef.update("subscribers", FieldValue.arrayUnion(userId))
                .addOnSuccessListener { Toast.makeText(this, "Inscrição realizada!", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun toggleFavorite(event: Event) {
        val userId = auth.currentUser?.uid ?: return
        val eventRef = db.collection("events").document(event.id)
        val isFavorite = event.favorites.contains(userId)

        if (isFavorite) {
            eventRef.update("favorites", FieldValue.arrayRemove(userId))
                .addOnSuccessListener { Toast.makeText(this, "Removido dos favoritos", Toast.LENGTH_SHORT).show() }
        } else {
            eventRef.update("favorites", FieldValue.arrayUnion(userId))
                .addOnSuccessListener { Toast.makeText(this, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showRatingDialog(event: Event) {
        val userId = auth.currentUser?.uid ?: return

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Avaliar Evento")
        builder.setMessage("Atribua uma nota de 1 a 5 estrelas para '${event.title}':")

        val ratingBar = RatingBar(this).apply {
            numStars = 5
            stepSize = 1.0f
            val previousRating = event.ratings[userId] ?: 5.0
            rating = previousRating.toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 16)
            addView(ratingBar)
        }

        builder.setView(container)
        builder.setPositiveButton("Salvar Avaliação") { dialog, _ ->
            val score = ratingBar.rating.toDouble()
            if (score <= 0.0) {
                Toast.makeText(this, "Selecione pelo menos 1 estrela", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            db.collection("events").document(event.id).update("ratings.$userId", score)
                .addOnSuccessListener { Toast.makeText(this, "Avaliação salva com sucesso!", Toast.LENGTH_SHORT).show() }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.remove()
        audiosListener?.remove()

        mediaPlayer?.release()
        mediaRecorder?.release()
    }
}
