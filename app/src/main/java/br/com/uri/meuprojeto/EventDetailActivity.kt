package br.com.uri.meuprojeto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.FileOutputStream
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var eventId: String = ""
    private var currentEvent: Event? = null
    private var eventListener: ListenerRegistration? = null
    private var audiosListener: ListenerRegistration? = null
    private var videosListener: ListenerRegistration? = null

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
    private lateinit var layoutTabVideos: LinearLayout

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

    // Tab 3: Vídeos
    private lateinit var cardVideoActions: MaterialCardView
    private lateinit var btnCaptureVideo: MaterialButton
    private lateinit var btnPickVideo: MaterialButton
    private lateinit var pbVideos: ProgressBar
    private lateinit var tvEmptyVideos: TextView
    private lateinit var rvVideos: RecyclerView
    private lateinit var videoAdapter: EventVideoAdapter

    // Audio Recorder & Player State
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecordingAudio: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingAudioId: String? = null

    // Video Temp File & Uri
    private var tempVideoFile: File? = null
    private var tempVideoUri: Uri? = null

    private val requestAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            toggleAudioRecording()
        } else {
            Toast.makeText(this, "Permissão de gravação de áudio é necessária", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchVideoCapture()
        } else {
            Toast.makeText(this, "Permissão de câmera é necessária para gravar vídeos", Toast.LENGTH_SHORT).show()
        }
    }

    private val captureVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && tempVideoFile != null && tempVideoFile!!.exists()) {
            saveAndUploadVideo(tempVideoFile!!)
        }
    }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveUriLocallyAndUploadVideo(uri)
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
        loadEventVideosRealtime()
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
        layoutTabVideos = findViewById(R.id.layoutTabVideos)

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

        // Tab 3: Vídeos
        cardVideoActions = findViewById(R.id.cardVideoActions)
        btnCaptureVideo = findViewById(R.id.btnCaptureVideo)
        btnPickVideo = findViewById(R.id.btnPickVideo)
        pbVideos = findViewById(R.id.pbVideos)
        tvEmptyVideos = findViewById(R.id.tvEmptyVideos)
        rvVideos = findViewById(R.id.rvVideos)

        val currentUserId = auth.currentUser?.uid ?: ""

        rvAudios.layoutManager = LinearLayoutManager(this)
        audioAdapter = EventAudioAdapter(
            audios = emptyList(),
            currentUserId = currentUserId,
            onPlayClick = { audio -> handleAudioPlayPause(audio) },
            onDeleteClick = { audio -> confirmDeleteAudio(audio) }
        )
        rvAudios.adapter = audioAdapter

        rvVideos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        videoAdapter = EventVideoAdapter(
            videos = emptyList(),
            currentUserId = currentUserId,
            onVideoClick = { video -> showVideoDialog(video) },
            onDeleteClick = { video -> confirmDeleteVideo(video) }
        )
        rvVideos.adapter = videoAdapter

        if (auth.currentUser == null) {
            cardRecordAudio.visibility = View.GONE
            cardVideoActions.visibility = View.GONE
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutTabLocation.visibility = View.VISIBLE
                        layoutTabAudios.visibility = View.GONE
                        layoutTabVideos.visibility = View.GONE
                    }
                    1 -> {
                        layoutTabLocation.visibility = View.GONE
                        layoutTabAudios.visibility = View.VISIBLE
                        layoutTabVideos.visibility = View.GONE
                    }
                    2 -> {
                        layoutTabLocation.visibility = View.GONE
                        layoutTabAudios.visibility = View.GONE
                        layoutTabVideos.visibility = View.VISIBLE
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

        btnCaptureVideo.setOnClickListener {
            checkCameraPermissionAndLaunchVideo()
        }

        btnPickVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        layoutComments.setOnClickListener {
            CommentsBottomSheetFragment.newInstance(eventId).show(supportFragmentManager, "CommentsBottomSheetFragment")
        }

        btnFavorite.setOnClickListener { currentEvent?.let { toggleFavorite(it) } }
        btnSubscribe.setOnClickListener { currentEvent?.let { toggleSubscription(it) } }
        btnRate.setOnClickListener { currentEvent?.let { showRatingDialog(it) } }
    }

    private fun checkCameraPermissionAndLaunchVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchVideoCapture()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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

        db.collection("events").document(eventId).collection("audios").document(audio.id)
            .delete()
            .addOnSuccessListener {
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

    // ==================== LÓGICA DE VÍDEO ====================

    private fun launchVideoCapture() {
        try {
            val videosDir = File(filesDir, "event_videos").apply { if (!exists()) mkdirs() }
            tempVideoFile = File(videosDir, "temp_video_${System.currentTimeMillis()}.mp4")
            tempVideoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", tempVideoFile!!)

            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, tempVideoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            val resInfoList = packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, tempVideoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            captureVideoLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir gravador de vídeo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUriLocallyAndUploadVideo(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val videosDir = File(filesDir, "event_videos").apply { if (!exists()) mkdirs() }
                val targetFile = File(videosDir, "video_${System.currentTimeMillis()}.mp4")
                FileOutputStream(targetFile).use { output -> inputStream.copyTo(output) }
                saveAndUploadVideo(targetFile)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar vídeo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveAndUploadVideo(file: File) {
        val user = auth.currentUser ?: return
        val videoId = db.collection("events").document(eventId).collection("videos").document().id
        val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"

        val video = EventVideo(
            id = videoId,
            eventId = eventId,
            userId = user.uid,
            userName = userName,
            videoPath = file.absolutePath,
            createdAt = System.currentTimeMillis()
        )

        db.collection("events").document(eventId).collection("videos").document(videoId).set(video)
            .addOnSuccessListener { Toast.makeText(this, "Vídeo publicado!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Erro ao publicar vídeo: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showVideoDialog(video: EventVideo) {
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .create()

        val frameLayout = android.widget.FrameLayout(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        val videoView = EventVideoView(this).apply {
            setVideoPath(video.videoPath)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            setOnPreparedListener { mp ->
                mp.isLooping = true
            }
        }

        val btnClose = ImageButton(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setPadding(32, 32, 32, 32)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(24, 48, 24, 24)
            }
            setOnClickListener {
                videoView.stopPlayback()
                dialog.dismiss()
            }
        }

        frameLayout.addView(videoView)
        frameLayout.addView(btnClose)

        dialog.setView(frameLayout)
        dialog.show()

        videoView.start()
    }

    private fun confirmDeleteVideo(video: EventVideo) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Vídeo")
            .setMessage("Deseja realmente excluir este vídeo?")
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteVideo(video)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteVideo(video: EventVideo) {
        db.collection("events").document(eventId).collection("videos").document(video.id)
            .delete()
            .addOnSuccessListener {
                try {
                    val file = File(video.videoPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}

                Toast.makeText(this, "Vídeo excluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir vídeo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadEventVideosRealtime() {
        pbVideos.visibility = View.VISIBLE
        videosListener = db.collection("events").document(eventId).collection("videos")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                pbVideos.visibility = View.GONE
                if (snapshot != null) {
                    val videos = snapshot.documents.mapNotNull { doc -> doc.toObject(EventVideo::class.java) }
                    tvEmptyVideos.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
                    rvVideos.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
                    videoAdapter.updateVideos(videos)
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
        videosListener?.remove()

        mediaPlayer?.release()
        mediaRecorder?.release()
    }
}
