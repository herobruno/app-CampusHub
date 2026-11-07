package br.com.uri.meuprojeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EventSubscribersBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var eventId: String = ""
    private var creatorId: String = ""

    private lateinit var tvOccupiedSpots: TextView
    private lateinit var tvCheckedInCount: TextView
    private lateinit var tvWaitlistCount: TextView
    private lateinit var pbSubscribers: ProgressBar
    private lateinit var tvEmptySubscribers: TextView
    private lateinit var rvSubscribers: RecyclerView
    private lateinit var btnCloseSheet: ImageButton
    private lateinit var btnToggleStatus: MaterialButton
    private lateinit var btnDeleteEvent: MaterialButton

    private lateinit var adapter: EventSubscriberAdapter
    private var subscribersListener: ListenerRegistration? = null
    private var eventListener: ListenerRegistration? = null

    private var currentEvent: Event? = null

    companion object {
        private const val ARG_EVENT_ID = "arg_event_id"
        private const val ARG_CREATOR_ID = "arg_creator_id"

        fun newInstance(eventId: String, creatorId: String): EventSubscribersBottomSheetFragment {
            val fragment = EventSubscribersBottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_EVENT_ID, eventId)
                putString(ARG_CREATOR_ID, creatorId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = arguments?.getString(ARG_EVENT_ID) ?: ""
        creatorId = arguments?.getString(ARG_CREATOR_ID) ?: ""
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_event_subscribers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvOccupiedSpots = view.findViewById(R.id.tvOccupiedSpots)
        tvCheckedInCount = view.findViewById(R.id.tvCheckedInCount)
        tvWaitlistCount = view.findViewById(R.id.tvWaitlistCount)
        pbSubscribers = view.findViewById(R.id.pbSubscribers)
        tvEmptySubscribers = view.findViewById(R.id.tvEmptySubscribers)
        rvSubscribers = view.findViewById(R.id.rvSubscribers)
        btnCloseSheet = view.findViewById(R.id.btnCloseSheet)
        btnToggleStatus = view.findViewById(R.id.btnToggleStatus)
        btnDeleteEvent = view.findViewById(R.id.btnDeleteEvent)

        btnCloseSheet.setOnClickListener { dismiss() }

        rvSubscribers.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventSubscriberAdapter(
            subscribers = emptyList(),
            onToggleCheckInClick = { subscriber -> toggleCheckIn(subscriber) }
        )
        rvSubscribers.adapter = adapter

        val isOwner = auth.currentUser?.uid == creatorId
        btnToggleStatus.visibility = if (isOwner) View.VISIBLE else View.GONE
        btnDeleteEvent.visibility = if (isOwner) View.VISIBLE else View.GONE

        btnToggleStatus.setOnClickListener { toggleEventStatus() }
        btnDeleteEvent.setOnClickListener { confirmDeleteEvent() }

        loadEventDetailsRealtime()
        loadSubscribersRealtime()
    }

    private fun getEventRef(): DocumentReference {
        return db.collection("events").document(creatorId).collection("user_events").document(eventId)
    }

    private fun loadEventDetailsRealtime() {
        if (eventId.isEmpty() || creatorId.isEmpty()) return
        eventListener = getEventRef().addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                currentEvent = snapshot.toObject(Event::class.java)
                updateMetricsDisplay()
            }
        }
    }

    private fun loadSubscribersRealtime() {
        if (eventId.isEmpty() || creatorId.isEmpty()) return

        pbSubscribers.visibility = View.VISIBLE
        subscribersListener = getEventRef().collection("subscribers")
            .orderBy("userName")
            .addSnapshotListener { snapshot, error ->
                pbSubscribers.visibility = View.GONE
                if (error != null) {
                    Toast.makeText(context, "Erro ao carregar inscritos: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val subscribers = snapshot.documents.mapNotNull { it.toObject(Subscriber::class.java) }
                    if (subscribers.isEmpty()) {
                        tvEmptySubscribers.visibility = View.VISIBLE
                        rvSubscribers.visibility = View.GONE
                    } else {
                        tvEmptySubscribers.visibility = View.GONE
                        rvSubscribers.visibility = View.VISIBLE
                    }

                    adapter.updateSubscribers(subscribers)
                    updateMetricsWithSubscribers(subscribers)
                }
            }
    }

    private fun updateMetricsDisplay() {
        val event = currentEvent ?: return
        val max = event.maxParticipants
        val count = event.subscriberCount
        tvOccupiedSpots.text = "$count / $max"
        tvWaitlistCount.text = "${event.waitlistCount}"

        if (event.isEnded) {
            btnToggleStatus.text = "Reabrir Evento"
        } else {
            btnToggleStatus.text = "Encerrar Evento"
        }
    }

    private fun updateMetricsWithSubscribers(subscribers: List<Subscriber>) {
        val checkedInCount = subscribers.count { it.checkedIn }
        tvCheckedInCount.text = "$checkedInCount"

        val event = currentEvent
        val max = event?.maxParticipants ?: 0
        tvOccupiedSpots.text = "${subscribers.size} / $max"
    }

    private fun toggleCheckIn(subscriber: Subscriber) {
        val newCheckedIn = !subscriber.checkedIn
        val timestamp = if (newCheckedIn) System.currentTimeMillis() else null

        val eventRef = getEventRef()
        val eventSubRef = eventRef.collection("subscribers").document(subscriber.userId)
        val userSubRef = db.collection("users").document(subscriber.userId).collection("subscriptions").document(eventId)

        val updates = mapOf(
            "checkedIn" to newCheckedIn,
            "checkedInAt" to timestamp
        )

        eventSubRef.update(updates).addOnSuccessListener {
            userSubRef.update(updates)

            // Sincroniza a contagem total de check-ins no documento do evento
            eventRef.collection("subscribers")
                .whereEqualTo("checkedIn", true)
                .get()
                .addOnSuccessListener { snapshot ->
                    val totalCheckedIn = snapshot?.size() ?: 0
                    eventRef.update("checkedInCount", totalCheckedIn)
                }

            val msg = if (newCheckedIn) "Check-in realizado!" else "Check-in desfeito!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Erro ao atualizar check-in: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleEventStatus() {
        val event = currentEvent ?: return
        val newStatus = if (event.isEnded) "OPEN" else "ENDED"
        val statusMessage = if (newStatus == "ENDED") "Evento encerrado com sucesso!" else "Evento reaberto com sucesso!"

        val eventRef = getEventRef()
        eventRef.update("status", newStatus)
            .addOnSuccessListener {
                eventRef.collection("subscribers").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc ->
                        val subUserId = doc.getString("userId") ?: doc.id
                        db.collection("users").document(subUserId).collection("subscriptions").document(event.id).update("status", newStatus)
                        eventRef.collection("subscribers").document(subUserId).update("status", newStatus)
                    }
                }
                eventRef.collection("favorites").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc ->
                        val favUserId = doc.getString("userId") ?: doc.id
                        db.collection("users").document(favUserId).collection("favorites").document(event.id).update("status", newStatus)
                        eventRef.collection("favorites").document(favUserId).update("status", newStatus)
                    }
                }
                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao alterar status: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmDeleteEvent() {
        val event = currentEvent ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Evento")
            .setMessage("Tem certeza de que deseja excluir o evento '${event.title}'? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteEvent()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteEvent() {
        getEventRef().delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Evento excluído com sucesso!", Toast.LENGTH_SHORT).show()
                dismiss()
                activity?.finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao excluir evento: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        subscribersListener?.remove()
        eventListener?.remove()
    }
}
