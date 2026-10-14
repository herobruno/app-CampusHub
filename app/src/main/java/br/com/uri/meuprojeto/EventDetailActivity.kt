package br.com.uri.meuprojeto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var eventId: String = ""
    private var currentEvent: Event? = null
    private var eventListener: ListenerRegistration? = null

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

    // Tab Layout & Location
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutTabLocation: MaterialCardView
    private lateinit var tvLocation: TextView
    private lateinit var btnOpenMaps: MaterialButton
    private lateinit var tvDescription: TextView

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
        tvLocation = findViewById(R.id.tvLocation)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        tvDescription = findViewById(R.id.tvDescription)

        // Configuração inicial das Abas
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> layoutTabLocation.visibility = View.VISIBLE
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

        layoutComments.setOnClickListener {
            CommentsBottomSheetFragment.newInstance(eventId).show(supportFragmentManager, "CommentsBottomSheetFragment")
        }

        btnFavorite.setOnClickListener { currentEvent?.let { toggleFavorite(it) } }
        btnSubscribe.setOnClickListener { currentEvent?.let { toggleSubscription(it) } }
        btnRate.setOnClickListener { currentEvent?.let { showRatingDialog(it) } }
    }

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
    }
}
