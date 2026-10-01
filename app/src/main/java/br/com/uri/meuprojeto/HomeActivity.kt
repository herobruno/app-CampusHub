package br.com.uri.meuprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvWelcome: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: EventAdapter
    private var eventsListener: ListenerRegistration? = null

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

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)

        // Configuração do RecyclerView
        rvEvents.layoutManager = LinearLayoutManager(this)
        adapter = EventAdapter(
            events = emptyList(),
            currentUserId = currentUser.uid,
            onSubscribeClick = { event -> toggleSubscription(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onCommentsClick = { event -> openCommentsBottomSheet(event.id) },
        )
        rvEvents.adapter = adapter

        // Popula automaticamente a coleção 'events' no Firestore caso esteja vazia
        EventSeeder.seedEventsIfEmpty()

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

        // Carrega eventos em tempo real do Firestore
        loadEventsRealtime()
    }

    private fun openCommentsBottomSheet(eventId: String) {
        val fragment = CommentsBottomSheetFragment.newInstance(eventId)
        fragment.show(supportFragmentManager, "CommentsBottomSheetFragment")
    }

    private fun loadEventsRealtime() {
        progressBar.visibility = View.VISIBLE

        eventsListener = db.collection("events")
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

                    if (eventsList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvEvents.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvEvents.visibility = View.VISIBLE
                        adapter.updateEvents(eventsList)
                    }
                }
            }
    }

    private fun toggleSubscription(event: Event) {
        val userId = auth.currentUser?.uid ?: return
        val eventRef = db.collection("events").document(event.id)

        val isSubscribed = event.subscribers.contains(userId)

        if (isSubscribed) {
            // Cancelar inscrição
            eventRef.update("subscribers", FieldValue.arrayRemove(userId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Inscrição cancelada com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao cancelar inscrição: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Verificar limite de participantes
            if (event.subscribers.size >= event.maxParticipants && event.maxParticipants > 0) {
                Toast.makeText(this, "Desculpe, este evento já atingiu o limite de vagas!", Toast.LENGTH_SHORT).show()
                return
            }

            // Inscrever-se
            eventRef.update("subscribers", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Inscrição realizada com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao realizar inscrição: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun toggleFavorite(event: Event) {
        val userId = auth.currentUser?.uid ?: return
        val eventRef = db.collection("events").document(event.id)

        val isFavorite = event.favorites.contains(userId)

        if (isFavorite) {
            eventRef.update("favorites", FieldValue.arrayRemove(userId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao desfavoritar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            eventRef.update("favorites", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao favoritar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        updateWelcomeMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventsListener?.remove()
    }

    private fun updateWelcomeMessage() {
        val user = auth.currentUser
        if (user != null) {
            val name = if (!user.displayName.isNullOrEmpty()) user.displayName else "Usuário"
            tvWelcome.text = "Bem-vindo, $name!"
        }
    }
}
