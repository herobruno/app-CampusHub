package br.com.uri.meuprojeto

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var rvMyEvents: RecyclerView
    private lateinit var tvNoMyEvents: TextView
    private lateinit var tabLayoutProfile: TabLayout
    private lateinit var eventAdapter: EventAdapter

    private var myEventsListener: ListenerRegistration? = null
    private var userSubscriptionsListener: ListenerRegistration? = null
    private var userSubscribedEventIds: Set<String> = emptySet()
    private var userFavoritesListener: ListenerRegistration? = null
    private var userFavoriteEventIds: Set<String> = emptySet()

    private lateinit var tvProfileHeaderName: TextView
    private lateinit var tvProfileHeaderEmail: TextView
    private lateinit var tvUserInitial: TextView
    private lateinit var cardEditProfile: MaterialCardView
    private lateinit var btnManageMyEvents: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user == null) {
            finish()
            return
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnToggleEdit = findViewById<ImageButton>(R.id.btnToggleEdit)
        val etProfileEmail = findViewById<EditText>(R.id.etProfileEmail)
        val etProfileName = findViewById<EditText>(R.id.etProfileName)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)

        tvProfileHeaderName = findViewById(R.id.tvProfileHeaderName)
        tvProfileHeaderEmail = findViewById(R.id.tvProfileHeaderEmail)
        tvUserInitial = findViewById(R.id.tvUserInitial)
        cardEditProfile = findViewById(R.id.cardEditProfile)
        btnManageMyEvents = findViewById(R.id.btnManageMyEvents)
        tabLayoutProfile = findViewById(R.id.tabLayoutProfile)
        rvMyEvents = findViewById(R.id.rvMyEvents)
        tvNoMyEvents = findViewById(R.id.tvNoMyEvents)

        etProfileEmail.setText(user.email ?: "")
        tvProfileHeaderEmail.text = user.email ?: ""

        // Configuração do RecyclerView para os eventos do usuário
        rvMyEvents.layoutManager = LinearLayoutManager(this)
        eventAdapter = EventAdapter(
            events = emptyList(),
            currentUserId = user.uid,
            subscribedEventIds = userSubscribedEventIds,
            favoriteEventIds = userFavoriteEventIds,
            onSubscribeClick = { event -> toggleSubscription(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onCommentsClick = { event -> openCommentsBottomSheet(event.id, event.creatorId) },
            onRateClick = { event -> showRatingDialog(event) },
            onEventClick = { event -> openEventDetail(event) }
        )
        rvMyEvents.adapter = eventAdapter

        // Tenta carregar o nome de exibição do Auth ou do Firestore
        if (!user.displayName.isNullOrEmpty()) {
            etProfileName.setText(user.displayName)
            updateHeaderInfo(user.displayName!!)
        } else {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        if (!name.isNullOrEmpty()) {
                            etProfileName.setText(name)
                            updateHeaderInfo(name)
                        } else {
                            updateHeaderInfo("Usuário")
                        }
                    }
                }
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnToggleEdit.setOnClickListener {
            if (cardEditProfile.visibility == View.VISIBLE) {
                cardEditProfile.visibility = View.GONE
            } else {
                cardEditProfile.visibility = View.VISIBLE
            }
        }

        btnManageMyEvents.setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }

        btnSaveProfile.setOnClickListener {
            val newName = etProfileName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Informe um nome válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveProfile.isEnabled = false

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        // Atualiza também no Firestore
                        val updates = mapOf<String, Any>(
                            "name" to newName,
                        )

                        db.collection("users").document(user.uid)
                            .update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                updateHeaderInfo(newName)
                                btnSaveProfile.isEnabled = true
                                cardEditProfile.visibility = View.GONE
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro ao atualizar no banco: ${e.message}", Toast.LENGTH_LONG).show()
                                btnSaveProfile.isEnabled = true
                            }
                    } else {
                        Toast.makeText(this, "Erro ao atualizar perfil: ${profileTask.exception?.message}", Toast.LENGTH_LONG).show()
                        btnSaveProfile.isEnabled = true
                    }
                }
        }

        // Configuração das Abas (Inscrições vs Favoritos)
        tabLayoutProfile.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                refreshActiveTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                refreshActiveTab()
            }
        })

        loadUserSubscriptionsRealtime()
        loadUserFavoritesRealtime()
        refreshActiveTab()
    }

    private fun loadUserSubscriptionsRealtime() {
        val userId = auth.currentUser?.uid ?: return
        userSubscriptionsListener = db.collection("users").document(userId).collection("subscriptions")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    userSubscribedEventIds = snapshot.documents.map { it.id }.toSet()
                    eventAdapter.updateSubscribedEvents(userSubscribedEventIds)
                }
            }
    }

    private fun loadUserFavoritesRealtime() {
        val userId = auth.currentUser?.uid ?: return
        userFavoritesListener = db.collection("users").document(userId).collection("favorites")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    userFavoriteEventIds = snapshot.documents.map { it.id }.toSet()
                    eventAdapter.updateFavoriteEvents(userFavoriteEventIds)
                }
            }
    }

    private fun refreshActiveTab() {
        val user = auth.currentUser ?: return
        when (tabLayoutProfile.selectedTabPosition) {
            0 -> loadSubscribedEvents(user.uid, "Você ainda não está inscrito em nenhum evento.")
            1 -> loadFavoriteEvents(user.uid, "Você ainda não possui eventos favoritados.")
            else -> loadSubscribedEvents(user.uid, "Você ainda não está inscrito em nenhum evento.")
        }
    }

    private fun openCommentsBottomSheet(eventId: String, creatorId: String) {
        val fragment = CommentsBottomSheetFragment.newInstance(eventId, creatorId)
        fragment.show(supportFragmentManager, "CommentsBottomSheetFragment")
    }

    private fun openEventDetail(event: Event) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra("EXTRA_EVENT_ID", event.id)
            putExtra("EXTRA_CREATOR_ID", event.creatorId)
        }
        startActivity(intent)
    }

    private fun getEventRef(event: Event) =
        db.collection("events").document(event.creatorId).collection("user_events").document(event.id)

    private fun showRatingDialog(event: Event) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Avaliar Evento")
        builder.setMessage("Atribua uma nota de 1 a 5 estrelas para '${event.title}':")

        val ratingBar = RatingBar(this).apply {
            numStars = 5
            stepSize = 1.0f
            rating = 5.0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
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

            val eventRef = getEventRef(event)
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

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun updateHeaderInfo(name: String) {
        tvProfileHeaderName.text = name
        if (name.isNotEmpty()) {
            tvUserInitial.text = name.substring(0, 1).uppercase()
        }
    }

    private fun loadSubscribedEvents(userId: String, emptyMessage: String) {
        myEventsListener?.remove()

        myEventsListener = db.collection("users").document(userId).collection("subscriptions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Erro ao carregar suas inscrições: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val subscriberDocs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Subscriber::class.java)
                    }

                    if (subscriberDocs.isEmpty()) {
                        tvNoMyEvents.text = emptyMessage
                        tvNoMyEvents.visibility = View.VISIBLE
                        rvMyEvents.visibility = View.GONE
                        eventAdapter.updateEvents(emptyList())
                    } else {
                        val initialList = subscriberDocs.map { sub ->
                            Event(
                                id = sub.eventId,
                                creatorId = sub.creatorId,
                                title = sub.title,
                                description = sub.description,
                                date = sub.date,
                                location = sub.location,
                                category = sub.category,
                                imageUrl = sub.imageUrl,
                                status = sub.status,
                                maxParticipants = sub.maxParticipants
                            )
                        }
                        tvNoMyEvents.visibility = View.GONE
                        rvMyEvents.visibility = View.VISIBLE
                        eventAdapter.updateEvents(initialList)

                        val eventsMap = initialList.associateBy { it.id }.toMutableMap()
                        for (sub in subscriberDocs) {
                            val ref = if (sub.creatorId.isNotEmpty()) {
                                db.collection("events").document(sub.creatorId).collection("user_events").document(sub.eventId)
                            } else {
                                db.collection("events").document(sub.eventId)
                            }
                            ref.get().addOnSuccessListener { eventDoc ->
                                if (eventDoc != null && eventDoc.exists()) {
                                    eventDoc.toObject(Event::class.java)?.let { freshEvent ->
                                        eventsMap[freshEvent.id] = freshEvent
                                        eventAdapter.updateEvents(eventsMap.values.toList())
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun loadFavoriteEvents(userId: String, emptyMessage: String) {
        myEventsListener?.remove()

        myEventsListener = db.collection("users").document(userId).collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Erro ao carregar favoritos: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val favoriteDocs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Favorite::class.java)
                    }

                    if (favoriteDocs.isEmpty()) {
                        tvNoMyEvents.text = emptyMessage
                        tvNoMyEvents.visibility = View.VISIBLE
                        rvMyEvents.visibility = View.GONE
                        eventAdapter.updateEvents(emptyList())
                    } else {
                        val initialList = favoriteDocs.map { fav ->
                            Event(
                                id = fav.eventId,
                                creatorId = fav.creatorId,
                                title = fav.title,
                                description = fav.description,
                                date = fav.date,
                                location = fav.location,
                                category = fav.category,
                                imageUrl = fav.imageUrl,
                                status = fav.status,
                                maxParticipants = fav.maxParticipants
                            )
                        }
                        tvNoMyEvents.visibility = View.GONE
                        rvMyEvents.visibility = View.VISIBLE
                        eventAdapter.updateEvents(initialList)

                        val eventsMap = initialList.associateBy { it.id }.toMutableMap()
                        for (fav in favoriteDocs) {
                            val ref = if (fav.creatorId.isNotEmpty()) {
                                db.collection("events").document(fav.creatorId).collection("user_events").document(fav.eventId)
                            } else {
                                db.collection("events").document(fav.eventId)
                            }
                            ref.get().addOnSuccessListener { eventDoc ->
                                if (eventDoc != null && eventDoc.exists()) {
                                    eventDoc.toObject(Event::class.java)?.let { freshEvent ->
                                        eventsMap[freshEvent.id] = freshEvent
                                        eventAdapter.updateEvents(eventsMap.values.toList())
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun toggleSubscription(event: Event) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        if (userId == event.creatorId) {
            Toast.makeText(this, "Você é o organizador deste evento!", Toast.LENGTH_SHORT).show()
            return
        }

        val eventRef = getEventRef(event)

        val userSubRef = db.collection("users").document(userId).collection("subscriptions").document(event.id)
        val eventSubRef = eventRef.collection("subscribers").document(userId)

        val isSubscribed = userSubscribedEventIds.contains(event.id)

        if (isSubscribed) {
            userSubRef.delete()
            eventSubRef.delete().addOnSuccessListener {
                syncSubscriberCount(eventRef)
                Toast.makeText(this, "Inscrição cancelada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        } else {
            val userName = if (!user.displayName.isNullOrEmpty()) user.displayName!! else user.email?.substringBefore("@") ?: "Usuário"
            val subscriber = Subscriber(
                id = userId,
                userId = userId,
                userName = userName,
                eventId = event.id,
                creatorId = event.creatorId,
                title = event.title,
                description = event.description,
                date = event.date,
                location = event.location,
                category = event.category,
                imageUrl = event.imageUrl,
                status = event.status,
                maxParticipants = event.maxParticipants
            )

            userSubRef.set(subscriber)
            eventSubRef.set(subscriber).addOnSuccessListener {
                syncSubscriberCount(eventRef)
                Toast.makeText(this, "Inscrição realizada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFavorite(event: Event) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val eventRef = getEventRef(event)

        val userFavRef = db.collection("users").document(userId).collection("favorites").document(event.id)
        val eventFavRef = eventRef.collection("favorites").document(userId)

        val isFavorite = userFavoriteEventIds.contains(event.id)

        if (isFavorite) {
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
                description = event.description,
                date = event.date,
                location = event.location,
                category = event.category,
                imageUrl = event.imageUrl,
                status = event.status,
                maxParticipants = event.maxParticipants
            )

            userFavRef.set(favoriteObj)
            eventFavRef.set(favoriteObj).addOnSuccessListener {
                syncFavoriteCount(eventRef)
                Toast.makeText(this, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncSubscriberCount(eventRef: com.google.firebase.firestore.DocumentReference) {
        eventRef.collection("subscribers").get().addOnSuccessListener { snapshot ->
            val count = snapshot?.size() ?: 0
            eventRef.update("subscriberCount", count)
        }
    }

    private fun syncFavoriteCount(eventRef: com.google.firebase.firestore.DocumentReference) {
        eventRef.collection("favorites").get().addOnSuccessListener { snapshot ->
            val count = snapshot?.size() ?: 0
            eventRef.update("favoriteCount", count)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshActiveTab()
    }

    override fun onDestroy() {
        super.onDestroy()
        myEventsListener?.remove()
        userSubscriptionsListener?.remove()
        userFavoritesListener?.remove()
    }
}
