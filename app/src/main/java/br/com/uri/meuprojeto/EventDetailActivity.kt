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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var eventId: String = ""
    private var creatorId: String = ""
    private var currentEvent: Event? = null

    private var eventListener: ListenerRegistration? = null
    private var photosListener: ListenerRegistration? = null
    private var audiosListener: ListenerRegistration? = null
    private var videosListener: ListenerRegistration? = null
    private var subscriberListener: ListenerRegistration? = null
    private var ratingListener: ListenerRegistration? = null
    private var favoriteListener: ListenerRegistration? = null

    private var isCurrentUserSubscribed: Boolean = false
    private var isCurrentUserFavorite: Boolean = false
    private var currentUserRatingScore: Double? = null

    // Main Header Views
    private lateinit var btnBack: ImageButton
    private lateinit var ivEventImage: ImageView
    private lateinit var btnFavorite: ImageButton
    private lateinit var tvCategory: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvRatingAverage: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvCreatorName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCapacity: TextView

    private lateinit var btnSubscribe: MaterialButton
    private lateinit var btnRate: MaterialButton
    private lateinit var layoutComments: LinearLayout
    private lateinit var tvCommentCount: TextView

    // Creator Management Card
    private lateinit var cardCreatorActions: MaterialCardView
    private lateinit var btnToggleStatus: MaterialButton
    private lateinit var btnDeleteEvent: MaterialButton

    // Tab Layout & Containers
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutTabLocation: MaterialCardView
    private lateinit var layoutTabPhotos: LinearLayout
    private lateinit var layoutTabAudios: LinearLayout
    private lateinit var layoutTabVideos: LinearLayout

    // Tab 1: Local
    private lateinit var tvLocation: TextView
    private lateinit var btnOpenMaps: MaterialButton
    private lateinit var tvDescription: TextView

    // Tab 2: Fotos
    private lateinit var cardPhotoActions: MaterialCardView
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnPickGalleryPhoto: MaterialButton
    private lateinit var pbPhotos: ProgressBar
    private lateinit var tvEmptyPhotos: TextView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var photoAdapter: EventPhotoAdapter

    // Tab 3: Áudios
    private lateinit var cardRecordAudio: MaterialCardView
    private lateinit var btnRecordAudio: MaterialButton
    private lateinit var tvRecordingStatus: TextView
    private lateinit var pbAudios: ProgressBar
    private lateinit var tvEmptyAudios: TextView
    private lateinit var rvAudios: RecyclerView
    private lateinit var audioAdapter: EventAudioAdapter

    // Tab 4: Vídeos
    private lateinit var cardVideoActions: MaterialCardView
    private lateinit var btnCaptureVideo: MaterialButton
    private lateinit var btnPickVideo: MaterialButton
    private lateinit var pbVideos: ProgressBar
    private lateinit var tvEmptyVideos: TextView
    private lateinit var rvVideos: RecyclerView
    private lateinit var videoAdapter: EventVideoAdapter

    // Temp File & Uri for Photo
    private var tempPhotoFile: File? = null
    private var tempPhotoImageUri: Uri? = null

    // Audio Recorder & Player State
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecordingAudio: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingAudioId: String? = null

    // Video Temp File & Uri
    private var tempVideoFile: File? = null
    private var tempVideoUri: Uri? = null

    private val requestPhotoPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchPhotoCapture()
        } else {
            Toast.makeText(this, "Permissão de câmera é necessária para tirar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && tempPhotoFile != null && tempPhotoFile!!.exists()) {
            saveAndUploadPhoto(tempPhotoFile!!)
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveUriLocallyAndUploadPhoto(uri)
        }
    }

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
        creatorId = intent.getStringExtra("EXTRA_CREATOR_ID") ?: ""

        if (eventId.isEmpty()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadEventDetailsRealtime()
    }

    private fun getEventRef(): DocumentReference {
        val cid = if (creatorId.isNotEmpty()) creatorId else currentEvent?.creatorId ?: ""
        return if (cid.isNotEmpty()) {
            db.collection("events").document(cid).collection("user_events").document(eventId)
        } else {
            db.collection("events").document(eventId)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivEventImage = findViewById(R.id.ivEventImage)
        btnFavorite = findViewById(R.id.btnFavorite)
        tvCategory = findViewById(R.id.tvCategory)
        tvStatus = findViewById(R.id.tvStatus)
        tvRatingAverage = findViewById(R.id.tvRatingAverage)
        tvTitle = findViewById(R.id.tvTitle)
        tvCreatorName = findViewById(R.id.tvCreatorName)
        tvDate = findViewById(R.id.tvDate)
        tvCapacity = findViewById(R.id.tvCapacity)

        btnSubscribe = findViewById(R.id.btnSubscribe)
        btnRate = findViewById(R.id.btnRate)
        layoutComments = findViewById(R.id.layoutComments)
        tvCommentCount = findViewById(R.id.tvCommentCount)

        cardCreatorActions = findViewById(R.id.cardCreatorActions)
        btnToggleStatus = findViewById(R.id.btnToggleStatus)
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent)

        tabLayout = findViewById(R.id.tabLayout)
        layoutTabLocation = findViewById(R.id.layoutTabLocation)
        layoutTabPhotos = findViewById(R.id.layoutTabPhotos)
        layoutTabAudios = findViewById(R.id.layoutTabAudios)
        layoutTabVideos = findViewById(R.id.layoutTabVideos)

        // Tab 1: Local
        tvLocation = findViewById(R.id.tvLocation)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        tvDescription = findViewById(R.id.tvDescription)

        // Tab 2: Fotos
        cardPhotoActions = findViewById(R.id.cardPhotoActions)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnPickGalleryPhoto = findViewById(R.id.btnPickGalleryPhoto)
        pbPhotos = findViewById(R.id.pbPhotos)
        tvEmptyPhotos = findViewById(R.id.tvEmptyPhotos)
        rvPhotos = findViewById(R.id.rvPhotos)

        // Tab 3: Áudios
        cardRecordAudio = findViewById(R.id.cardRecordAudio)
        btnRecordAudio = findViewById(R.id.btnRecordAudio)
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus)
        pbAudios = findViewById(R.id.pbAudios)
        tvEmptyAudios = findViewById(R.id.tvEmptyAudios)
        rvAudios = findViewById(R.id.rvAudios)

        // Tab 4: Vídeos
        cardVideoActions = findViewById(R.id.cardVideoActions)
        btnCaptureVideo = findViewById(R.id.btnCaptureVideo)
        btnPickVideo = findViewById(R.id.btnPickVideo)
        pbVideos = findViewById(R.id.pbVideos)
        tvEmptyVideos = findViewById(R.id.tvEmptyVideos)
        rvVideos = findViewById(R.id.rvVideos)

        val currentUserId = auth.currentUser?.uid ?: ""

        rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photoAdapter = EventPhotoAdapter(
            photos = emptyList(),
            currentUserId = currentUserId,
            onPhotoClick = { photo -> showPhotoDialog(photo) },
            onDeleteClick = { photo -> confirmDeletePhoto(photo) }
        )
        rvPhotos.adapter = photoAdapter

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
            cardPhotoActions.visibility = View.GONE
            cardRecordAudio.visibility = View.GONE
            cardVideoActions.visibility = View.GONE
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutTabLocation.visibility = View.VISIBLE
                        layoutTabPhotos.visibility = View.GONE
                        layoutTabAudios.visibility = View.GONE
                        layoutTabVideos.visibility = View.GONE
                    }
                    1 -> {
                        layoutTabLocation.visibility = View.GONE
                        layoutTabPhotos.visibility = View.VISIBLE
                        layoutTabAudios.visibility = View.GONE
                        layoutTabVideos.visibility = View.GONE
                    }
                    2 -> {
                        layoutTabLocation.visibility = View.GONE
                        layoutTabPhotos.visibility = View.GONE
                        layoutTabAudios.visibility = View.VISIBLE
                        layoutTabVideos.visibility = View.GONE
                    }
                    3 -> {
                        layoutTabLocation.visibility = View.GONE
                        layoutTabPhotos.visibility = View.GONE
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

        btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunchPhoto()
        }

        btnPickGalleryPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
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
            val cid = if (creatorId.isNotEmpty()) creatorId else currentEvent?.creatorId ?: ""
            CommentsBottomSheetFragment.newInstance(eventId, cid).show(supportFragmentManager, "CommentsBottomSheetFragment")
        }

        btnFavorite.setOnClickListener { currentEvent?.let { toggleFavorite(it) } }
        btnSubscribe.setOnClickListener { currentEvent?.let { toggleSubscription(it) } }
        btnRate.setOnClickListener { currentEvent?.let { showRatingDialog(it) } }

        btnToggleStatus.setOnClickListener {
            currentEvent?.let { toggleEventStatus(it) }
        }

        btnDeleteEvent.setOnClickListener {
            currentEvent?.let { confirmDeleteEvent(it) }
        }
    }

    private fun toggleEventStatus(event: Event) {
        val newStatus = if (event.isEnded) "OPEN" else "ENDED"
        val statusMessage = if (newStatus == "ENDED") "Evento encerrado com sucesso!" else "Evento reaberto com sucesso!"

        getEventRef()
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao alterar status: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmDeleteEvent(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Evento")
            .setMessage("Tem certeza de que deseja excluir o evento '${event.title}'? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteEvent(event)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteEvent(event: Event) {
        getEventRef()
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Evento excluído com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir evento: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ==================== LÓGICA DE FOTO ====================

    private fun checkCameraPermissionAndLaunchPhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchPhotoCapture()
        } else {
            requestPhotoPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkCameraPermissionAndLaunchVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchVideoCapture()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchPhotoCapture() {
        try {
            val photosDir = File(filesDir, "event_photos").apply { if (!exists()) mkdirs() }
            tempPhotoFile = File(photosDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            tempPhotoImageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", tempPhotoFile!!)

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoImageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            val resInfoList = packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, tempPhotoImageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            takePhotoLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir câmera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUriLocallyAndUploadPhoto(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val photosDir = File(filesDir, "event_photos").apply { if (!exists()) mkdirs() }
                val targetFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(targetFile).use { output -> inputStream.copyTo(output) }
                saveAndUploadPhoto(targetFile)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveAndUploadPhoto(file: File) {
        val user = auth.currentUser ?: return
        val photosRef = getEventRef().collection("photos")
        val photoId = photosRef.document().id
        val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"

        val photo = EventPhoto(
            id = photoId,
            eventId = eventId,
            userId = user.uid,
            userName = userName,
            photoPath = file.absolutePath,
            createdAt = System.currentTimeMillis()
        )

        photosRef.document(photoId).set(photo)
            .addOnSuccessListener { Toast.makeText(this, "Foto publicada!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Erro ao publicar foto: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showPhotoDialog(photo: EventPhoto) {
        val dialogView = layoutInflater.inflate(R.layout.item_event_photo, null)
        val ivPhoto = dialogView.findViewById<ImageView>(R.id.ivPhoto)
        val tvAuthor = dialogView.findViewById<TextView>(R.id.tvAuthor)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDate)

        ivPhoto.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 800)
        val authorName = photo.userName.ifEmpty { "Usuário" }
        tvAuthor.text = "Publicado por: $authorName"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        tvDate.text = dateFormat.format(Date(photo.createdAt))

        val photoSource: Any = if (photo.photoPath.startsWith("content://") || photo.photoPath.startsWith("file://")) {
            photo.photoPath
        } else {
            File(photo.photoPath)
        }

        Glide.with(this).load(photoSource).placeholder(R.drawable.ic_launcher_background).into(ivPhoto)

        AlertDialog.Builder(this)
            .setTitle("Foto do Evento")
            .setView(dialogView)
            .setPositiveButton("Fechar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun confirmDeletePhoto(photo: EventPhoto) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Foto")
            .setMessage("Deseja realmente excluir esta foto?")
            .setPositiveButton("Excluir") { dialog, _ ->
                deletePhoto(photo)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deletePhoto(photo: EventPhoto) {
        getEventRef().collection("photos").document(photo.id)
            .delete()
            .addOnSuccessListener {
                try {
                    val file = File(photo.photoPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}

                Toast.makeText(this, "Foto excluída com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadEventPhotosRealtime() {
        pbPhotos.visibility = View.VISIBLE
        photosListener = getEventRef().collection("photos")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                pbPhotos.visibility = View.GONE
                if (snapshot != null) {
                    val photos = snapshot.documents.mapNotNull { doc -> doc.toObject(EventPhoto::class.java) }
                    tvEmptyPhotos.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
                    rvPhotos.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
                    photoAdapter.updatePhotos(photos)
                }
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
                val audiosRef = getEventRef().collection("audios")
                val audioId = audiosRef.document().id
                val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"

                val audio = EventAudio(
                    id = audioId,
                    eventId = eventId,
                    userId = user.uid,
                    userName = userName,
                    audioPath = recordingFile!!.absolutePath,
                    createdAt = System.currentTimeMillis()
                )

                audiosRef.document(audioId).set(audio)
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

        getEventRef().collection("audios").document(audio.id)
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
        audiosListener = getEventRef().collection("audios")
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
        val videosRef = getEventRef().collection("videos")
        val videoId = videosRef.document().id
        val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"

        val video = EventVideo(
            id = videoId,
            eventId = eventId,
            userId = user.uid,
            userName = userName,
            videoPath = file.absolutePath,
            createdAt = System.currentTimeMillis()
        )

        videosRef.document(videoId).set(video)
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
        getEventRef().collection("videos").document(video.id)
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
        videosListener = getEventRef().collection("videos")
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
        if (creatorId.isNotEmpty()) {
            eventListener = getEventRef().addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val event = snapshot.toObject(Event::class.java)
                    if (event != null) {
                        currentEvent = event
                        bindEventData(event)
                        loadSubcollections()
                    }
                }
            }
        } else {
            eventListener = db.collectionGroup("user_events")
                .whereEqualTo("id", eventId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        val event = doc.toObject(Event::class.java)
                        if (event != null) {
                            currentEvent = event
                            creatorId = event.creatorId
                            bindEventData(event)
                            loadSubcollections()
                        }
                    }
                }
        }
    }

    private fun loadSubcollections() {
        val currentUserId = auth.currentUser?.uid ?: ""

        if (subscriberListener == null && currentUserId.isNotEmpty()) {
            subscriberListener = getEventRef().collection("subscribers").document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    isCurrentUserSubscribed = (snapshot != null && snapshot.exists())
                    currentEvent?.let { bindEventData(it) }
                }
        }

        if (favoriteListener == null && currentUserId.isNotEmpty()) {
            favoriteListener = getEventRef().collection("favorites").document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    isCurrentUserFavorite = (snapshot != null && snapshot.exists())
                    currentEvent?.let { bindEventData(it) }
                }
        }

        if (ratingListener == null && currentUserId.isNotEmpty()) {
            ratingListener = getEventRef().collection("ratings").document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    currentUserRatingScore = snapshot?.getDouble("rating")
                    currentEvent?.let { bindEventData(it) }
                }
        }

        if (photosListener == null) loadEventPhotosRealtime()
        if (audiosListener == null) loadEventAudiosRealtime()
        if (videosListener == null) loadEventVideosRealtime()
    }

    private fun bindEventData(event: Event) {
        val currentUserId = auth.currentUser?.uid ?: ""

        tvCategory.text = event.category
        tvTitle.text = event.title

        val creator = if (event.creatorName.isNotEmpty()) event.creatorName else "URI Eventos"
        tvCreatorName.text = "Criado por: $creator"

        tvDate.text = event.date
        tvLocation.text = if (event.location.isNotEmpty()) event.location else "Endereço não informado"
        tvDescription.text = if (event.description.isNotEmpty()) event.description else "Sem descrição informada."

        tvCapacity.text = "${event.subscriberCount} / ${event.maxParticipants} participantes inscritos"
        tvCommentCount.text = event.commentCount.toString()

        val isSubscribed = isCurrentUserSubscribed

        val isOwner = currentUserId.isNotEmpty() && currentUserId == event.creatorId

        if (event.isEnded) {
            tvStatus.text = "Encerrado"
            tvStatus.setBackgroundColor("#FFD32F2F".toColorInt())
            val labelReviews = if (event.ratingCount == 1) "avaliação" else "avaliações"
            tvRatingAverage.text = String.format(Locale.getDefault(), "★ %.1f (%d %s)", event.averageRating, event.ratingCount, labelReviews)
            tvRatingAverage.visibility = View.VISIBLE
            btnSubscribe.visibility = View.GONE

            if (isSubscribed && !isOwner) {
                btnRate.visibility = View.VISIBLE
                val userRating = currentUserRatingScore
                btnRate.text = if (userRating != null) String.format(Locale.getDefault(), "Sua Nota: %.1f ★", userRating) else "Avaliar Evento"
            } else {
                btnRate.visibility = View.GONE
            }
        } else {
            tvStatus.text = "Aberto"
            tvStatus.setBackgroundColor("#FF388E3C".toColorInt())
            tvRatingAverage.visibility = View.GONE
            btnRate.visibility = View.GONE

            if (isOwner) {
                btnSubscribe.visibility = View.GONE
            } else {
                btnSubscribe.visibility = View.VISIBLE
                btnSubscribe.text = if (isSubscribed) "Inscrito" else "Inscrever-se"
            }
        }

        // Ações do Criador (Se o usuário for o criador do evento)
        if (isOwner) {
            cardCreatorActions.visibility = View.VISIBLE
            if (event.isEnded) {
                btnToggleStatus.text = "Reabrir Evento"
            } else {
                btnToggleStatus.text = "Encerrar Evento"
            }
        } else {
            cardCreatorActions.visibility = View.GONE
        }

        val isFavorite = isCurrentUserFavorite
        btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)

        if (event.imageUrl.isNotEmpty()) {
            Glide.with(this).load(event.imageUrl).placeholder(R.drawable.ic_launcher_background).error(R.drawable.ic_launcher_background).into(ivEventImage)
        } else {
            ivEventImage.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    private fun toggleSubscription(event: Event) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        if (userId == event.creatorId) {
            Toast.makeText(this, "Você é o organizador deste evento!", Toast.LENGTH_SHORT).show()
            return
        }

        val eventRef = getEventRef()

        val userSubRef = db.collection("users").document(userId).collection("subscriptions").document(event.id)
        val eventSubRef = eventRef.collection("subscribers").document(userId)

        if (isCurrentUserSubscribed) {
            userSubRef.delete()
            eventSubRef.delete().addOnSuccessListener {
                syncSubscriberCount(eventRef)
                Toast.makeText(this, "Inscrição cancelada!", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (event.subscriberCount >= event.maxParticipants && event.maxParticipants > 0) {
                Toast.makeText(this, "Evento com vagas esgotadas!", Toast.LENGTH_SHORT).show()
                return
            }
            val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"
            val subscriber = Subscriber(
                id = userId,
                userId = userId,
                userName = userName,
                eventId = event.id,
                creatorId = event.creatorId,
                title = event.title,
                date = event.date,
                location = event.location,
                imageUrl = event.imageUrl
            )

            userSubRef.set(subscriber)
            eventSubRef.set(subscriber).addOnSuccessListener {
                syncSubscriberCount(eventRef)
                Toast.makeText(this, "Inscrição realizada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFavorite(event: Event) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val eventRef = getEventRef()

        val userFavRef = db.collection("users").document(userId).collection("favorites").document(event.id)
        val eventFavRef = eventRef.collection("favorites").document(userId)

        if (isCurrentUserFavorite) {
            userFavRef.delete()
            eventFavRef.delete().addOnSuccessListener {
                syncFavoriteCount(eventRef)
                Toast.makeText(this, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
            }
        } else {
            val favoriteObj = Favorite(
                id = userId,
                userId = userId,
                eventId = event.id,
                creatorId = event.creatorId,
                title = event.title,
                date = event.date,
                location = event.location,
                imageUrl = event.imageUrl
            )

            userFavRef.set(favoriteObj)
            eventFavRef.set(favoriteObj).addOnSuccessListener {
                syncFavoriteCount(eventRef)
                Toast.makeText(this, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncSubscriberCount(eventRef: DocumentReference) {
        eventRef.collection("subscribers").get().addOnSuccessListener { snapshot ->
            val count = snapshot?.size() ?: 0
            eventRef.update("subscriberCount", count)
        }
    }

    private fun syncFavoriteCount(eventRef: DocumentReference) {
        eventRef.collection("favorites").get().addOnSuccessListener { snapshot ->
            val count = snapshot?.size() ?: 0
            eventRef.update("favoriteCount", count)
        }
    }

    private fun showRatingDialog(event: Event) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Avaliar Evento")
        builder.setMessage("Atribua uma nota de 1 a 5 estrelas para '${event.title}':")

        val ratingBar = RatingBar(this).apply {
            numStars = 5
            stepSize = 1.0f
            val previousRating = currentUserRatingScore ?: 5.0
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

            val eventRef = getEventRef()
            val ratingRef = eventRef.collection("ratings").document(userId)

            val ratingObj = Rating(
                id = userId,
                userId = userId,
                rating = score,
                eventId = event.id,
                creatorId = event.creatorId
            )

            ratingRef.set(ratingObj).addOnSuccessListener {
                eventRef.collection("ratings").get().addOnSuccessListener { ratingsSnapshot ->
                    if (ratingsSnapshot != null && !ratingsSnapshot.isEmpty) {
                        val scores = ratingsSnapshot.documents.mapNotNull { it.getDouble("rating") }
                        val totalCount = scores.size
                        val avg = if (totalCount > 0) scores.sum() / totalCount else 0.0

                        eventRef.update(
                            mapOf(
                                "averageRating" to avg,
                                "ratingCount" to totalCount
                            )
                        )
                    }
                }
                Toast.makeText(this, "Avaliação salva com sucesso!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.remove()
        photosListener?.remove()
        audiosListener?.remove()
        videosListener?.remove()
        subscriberListener?.remove()
        ratingListener?.remove()
        favoriteListener?.remove()

        mediaPlayer?.release()
        mediaRecorder?.release()
    }
}
