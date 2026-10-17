package br.com.uri.meuprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
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

class MyEventsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var rvMyEvents: RecyclerView
    private lateinit var tvNoMyEvents: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var eventAdapter: EventAdapter
    private var myEventsListener: ListenerRegistration? = null
    private var userSubscriptionsListener: ListenerRegistration? = null
    private var userFavoritesListener: ListenerRegistration? = null
    private var userSubscribedEventIds: Set<String> = emptySet()
    private var userFavoriteEventIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_my_events)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user == null) {
            finish()
            return
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        rvMyEvents = findViewById(R.id.rvMyEvents)
        tvNoMyEvents = findViewById(R.id.tvNoMyEvents)
        progressBar = findViewById(R.id.progressBar)

        btnBack.setOnClickListener { finish() }

        rvMyEvents.layoutManager = LinearLayoutManager(this)
        eventAdapter = EventAdapter(
            events = emptyList(),
            currentUserId = user.uid,
            subscribedEventIds = userSubscribedEventIds,
            favoriteEventIds = userFavoriteEventIds,
            onSubscribeClick = { event -> toggleSubscription(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onCommentsClick = { event -> openCommentsBottomSheet(event.id, event.creatorId) },
            onEventClick = { event -> openEventDetail(event) }
        )
        rvMyEvents.adapter = eventAdapter

        loadUserSubscriptionsRealtime()
        loadUserFavoritesRealtime()
        loadMyEventsRealtime(user.uid)
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

    private fun loadMyEventsRealtime(userId: String) {
        progressBar.visibility = View.VISIBLE
        myEventsListener?.remove()

        myEventsListener = db.collection("events").document(userId).collection("user_events")
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(this, "Erro ao carregar eventos criados: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val myCreatedEvents = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)
                    }

                    if (myCreatedEvents.isEmpty()) {
                        tvNoMyEvents.visibility = View.VISIBLE
                        rvMyEvents.visibility = View.GONE
                        eventAdapter.updateEvents(emptyList())
                    } else {
                        tvNoMyEvents.visibility = View.GONE
                        rvMyEvents.visibility = View.VISIBLE
                        eventAdapter.updateEvents(myCreatedEvents)
                    }
                }
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
                date = event.date,
                location = event.location,
                imageUrl = event.imageUrl
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

    override fun onDestroy() {
        super.onDestroy()
        myEventsListener?.remove()
        userSubscriptionsListener?.remove()
        userFavoritesListener?.remove()
    }
}
