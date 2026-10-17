package br.com.uri.meuprojeto

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvWelcome: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var etSearchQuery: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var fabAddEvent: FloatingActionButton

    private lateinit var adapter: EventAdapter
    private var eventsListener: ListenerRegistration? = null
    private var userSubscriptionsListener: ListenerRegistration? = null
    private var userSubscribedEventIds: Set<String> = emptySet()
    private var userFavoritesListener: ListenerRegistration? = null
    private var userFavoriteEventIds: Set<String> = emptySet()

    private var allEvents: List<Event> = emptyList()
    private val categoryList = mutableListOf("Todas", "Tecnologia", "Inteligência Artificial", "Design", "Hackathon", "Backend & Cloud")
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private lateinit var statusAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvWelcome = findViewById(R.id.tvWelcome)
        rvEvents = findViewById(R.id.rvEvents)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        etSearchQuery = findViewById(R.id.etSearchQuery)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        fabAddEvent = findViewById(R.id.fabAddEvent)

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)

        // Configuração do RecyclerView
        rvEvents.layoutManager = LinearLayoutManager(this)
        adapter = EventAdapter(
            events = emptyList(),
            currentUserId = currentUser.uid,
            subscribedEventIds = userSubscribedEventIds,
            favoriteEventIds = userFavoriteEventIds,
            onSubscribeClick = { event -> toggleSubscription(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onCommentsClick = { event -> openCommentsBottomSheet(event.id, event.creatorId) },
            onRateClick = { event -> showRatingDialog(event) },
            onEventClick = { event -> openEventDetail(event) },
        )
        rvEvents.adapter = adapter

        // Configuração dos Filtros (Search & Spinners)
        setupFilters()

        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Início")
            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == 1) {
                    rvEvents.scrollToPosition(0)
                    true
                } else {
                    false
                }
            }
            popup.show()
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        fabAddEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }

        loadUserSubscriptionsRealtime()
        loadUserFavoritesRealtime()
        loadEventsRealtime()
    }

    private fun loadUserSubscriptionsRealtime() {
        val userId = auth.currentUser?.uid ?: return
        userSubscriptionsListener = db.collection("users").document(userId).collection("subscriptions")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    userSubscribedEventIds = snapshot.documents.map { it.id }.toSet()
                    adapter.updateSubscribedEvents(userSubscribedEventIds)
                }
            }
    }

    private fun loadUserFavoritesRealtime() {
        val userId = auth.currentUser?.uid ?: return
        userFavoritesListener = db.collection("users").document(userId).collection("favorites")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    userFavoriteEventIds = snapshot.documents.map { it.id }.toSet()
                    adapter.updateFavoriteEvents(userFavoriteEventIds)
                }
            }
    }

    private fun setupFilters() {
        categoryAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            categoryList
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        spinnerCategory.adapter = categoryAdapter

        val statusList = listOf("Todos", "Abertos", "Encerrados")
        statusAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            statusList
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        spinnerStatus.adapter = statusAdapter

        etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEvents()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterEvents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerCategory.onItemSelectedListener = spinnerListener
        spinnerStatus.onItemSelectedListener = spinnerListener
    }

    private fun filterEvents() {
        val query = etSearchQuery.text.toString().trim()
        val selectedCategory = spinnerCategory.selectedItem?.toString() ?: "Todas"
        val selectedStatus = spinnerStatus.selectedItem?.toString() ?: "Todos"

        val filteredList = allEvents.filter { event ->
            // 1. Pesquisa textual no título (case-insensitive)
            val matchesTitle = query.isEmpty() || event.title.contains(query, ignoreCase = true)

            // 2. Categoria
            val matchesCategory = selectedCategory == "Todas" || event.category.equals(selectedCategory, ignoreCase = true)

            // 3. Situação / Status ("Todos", "Abertos", "Encerrados")
            val matchesStatus = when (selectedStatus) {
                "Abertos" -> !event.isEnded && event.status != "ENDED"
                "Encerrados" -> event.isEnded || event.status == "ENDED"
                else -> true
            }

            matchesTitle && matchesCategory && matchesStatus
        }

        if (filteredList.isEmpty()) {
            tvEmpty.text = if (allEvents.isEmpty()) {
                "Nenhum evento disponível no momento."
            } else {
                "Nenhum evento encontrado para os filtros selecionados."
            }
            tvEmpty.visibility = View.VISIBLE
            rvEvents.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvEvents.visibility = View.VISIBLE
        }

        adapter.updateEvents(filteredList)
    }

    private fun updateCategoriesFromEvents(eventsList: List<Event>) {
        val uniqueCategories = eventsList.map { it.category }.filter { it.isNotEmpty() }.toSet()
        var updated = false
        for (cat in uniqueCategories) {
            if (!categoryList.contains(cat)) {
                categoryList.add(cat)
                updated = true
            }
        }
        if (updated) {
            categoryAdapter.notifyDataSetChanged()
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

    private fun loadEventsRealtime() {
        progressBar.visibility = View.VISIBLE

        eventsListener = db.collectionGroup("user_events")
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(this, "Erro ao carregar eventos: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val eventsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)
                    }

                    allEvents = eventsList
                    updateCategoriesFromEvents(eventsList)
                    filterEvents()
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
            if (event.subscriberCount >= event.maxParticipants && event.maxParticipants > 0) {
                Toast.makeText(this, "Desculpe, este evento já atingiu o limite de vagas!", Toast.LENGTH_SHORT).show()
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
        updateWelcomeMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventsListener?.remove()
        userSubscriptionsListener?.remove()
        userFavoritesListener?.remove()
    }

    private fun updateWelcomeMessage() {
        val user = auth.currentUser
        if (user != null) {
            val name = if (!user.displayName.isNullOrEmpty()) user.displayName else "Usuário"
            tvWelcome.text = "Bem-vindo, $name!"
        }
    }
}
