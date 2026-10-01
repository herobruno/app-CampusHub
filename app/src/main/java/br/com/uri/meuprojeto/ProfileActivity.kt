package br.com.uri.meuprojeto

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
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

    private lateinit var tvProfileHeaderName: TextView
    private lateinit var tvProfileHeaderEmail: TextView
    private lateinit var tvUserInitial: TextView
    private lateinit var cardEditProfile: MaterialCardView

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
            onSubscribeClick = { event -> toggleSubscription(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onCommentsClick = { event -> openCommentsBottomSheet(event.id) },
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
                when (tab?.position) {
                    0 -> loadEventsByField("subscribers", user.uid, "Você ainda não está inscrito em nenhum evento.")
                    1 -> loadEventsByField("favorites", user.uid, "Você ainda não possui eventos favoritados.")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Carrega inicialmente os eventos inscritos
        loadEventsByField("subscribers", user.uid, "Você ainda não está inscrito em nenhum evento.")
    }

    private fun openCommentsBottomSheet(eventId: String) {
        val fragment = CommentsBottomSheetFragment.newInstance(eventId)
        fragment.show(supportFragmentManager, "CommentsBottomSheetFragment")
    }

    private fun updateHeaderInfo(name: String) {
        tvProfileHeaderName.text = name
        if (name.isNotEmpty()) {
            tvUserInitial.text = name.substring(0, 1).uppercase()
        }
    }

    private fun loadEventsByField(fieldName: String, userId: String, emptyMessage: String) {
        myEventsListener?.remove()

        myEventsListener = db.collection("events")
            .whereArrayContains(fieldName, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Erro ao carregar seus eventos: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val myEvents = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)
                    }

                    if (myEvents.isEmpty()) {
                        tvNoMyEvents.text = emptyMessage
                        tvNoMyEvents.visibility = View.VISIBLE
                        rvMyEvents.visibility = View.GONE
                    } else {
                        tvNoMyEvents.visibility = View.GONE
                        rvMyEvents.visibility = View.VISIBLE
                        eventAdapter.updateEvents(myEvents)
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

    override fun onDestroy() {
        super.onDestroy()
        myEventsListener?.remove()
    }
}
