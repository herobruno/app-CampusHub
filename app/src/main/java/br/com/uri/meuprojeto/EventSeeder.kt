package br.com.uri.meuprojeto

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object EventSeeder {

    private const val TAG = "EventSeeder"

    fun seedEventsIfEmpty(onComplete: ((Boolean) -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()

        // Garante que o evento 1 seja marcado como "ENDED" mesmo se a coleção já existir
        db.collection("events").document("event_1").update("status", "ENDED")

        db.collection("events").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "Coleção 'events' vazia. Criando eventos iniciais...")
                    createSampleEvents(db, onComplete)
                } else {
                    Log.d(TAG, "Coleção 'events' já possui ${snapshot.size()} documentos. Mantendo dados do banco.")
                    onComplete?.invoke(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao verificar coleção 'events': ${e.message}", e)
                onComplete?.invoke(false)
            }
    }

    private fun createSampleEvents(db: FirebaseFirestore, onComplete: ((Boolean) -> Unit)?) {
        val sampleEvents = listOf(
            Event(
                id = "event_1",
                title = "Workshop de Jetpack Compose & Kotlin",
                description = "Aprenda na prática a criar interfaces modernas e reativas no Android utilizando Jetpack Compose, gerenciamento de estado e boas práticas.",
                date = "15/10/2026 - 19:00",
                location = "Auditório Central - Bloco A (URI)",
                category = "Tecnologia",
                maxParticipants = 50,
                subscribers = emptyList(),
                favorites = emptyList(),
                commentCount = 0,
                status = "ENDED",
                ratings = mapOf("sample_user_1" to 5.0, "sample_user_2" to 4.0),
                imageUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97",
            ),
            Event(
                id = "event_2",
                title = "Summit de Inteligência Artificial Generativa",
                description = "Exploração prática de Large Language Models, integração de APIs de IA no ecossistema mobile e criação de agentes inteligentes.",
                date = "22/10/2026 - 18:30",
                location = "Laboratório de Informática 04",
                category = "Inteligência Artificial",
                maxParticipants = 40,
                subscribers = emptyList(),
                favorites = emptyList(),
                commentCount = 0,
                status = "OPEN",
                imageUrl = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e",
            ),
            Event(
                id = "event_3",
                title = "Bootcamp de UI/UX Design para Apps Mobile",
                description = "Técnicas de prototipação no Figma, sistemas de design (Material Design 3), arquitetura de informação e usabilidade focada no usuário.",
                date = "05/11/2026 - 14:00",
                location = "Sala Inovação - Prédio 2",
                category = "Design",
                maxParticipants = 30,
                subscribers = emptyList(),
                favorites = emptyList(),
                commentCount = 0,
                status = "OPEN",
                imageUrl = "https://images.unsplash.com/photo-1581291518633-83b4ebd1d83e",
            ),
            Event(
                id = "event_4",
                title = "Hackathon de Soluções Sustentáveis",
                description = "Maratona de programação de 24 horas para desenvolver soluções tecnológicas com impacto positivo e sustentável na comunidade local.",
                date = "12/11/2026 - 08:00",
                location = "Centro de Eventos Universitário",
                category = "Hackathon",
                maxParticipants = 100,
                subscribers = emptyList(),
                favorites = emptyList(),
                commentCount = 0,
                status = "OPEN",
                imageUrl = "https://images.unsplash.com/photo-1504384308090-c894fdcc538d",
            ),
            Event(
                id = "event_5",
                title = "Meetup de Arquitetura de Software & Firebase",
                description = "Discussão técnica sobre Clean Architecture, MVVM, persistência reativa com Firestore, autenticação segura e Firebase Cloud Functions.",
                date = "20/11/2026 - 19:30",
                location = "Auditório Técnico - Bloco C",
                category = "Backend & Cloud",
                maxParticipants = 60,
                subscribers = emptyList(),
                favorites = emptyList(),
                commentCount = 0,
                status = "OPEN",
                imageUrl = "https://images.unsplash.com/photo-1531482615713-2afd69097998",
            ),
        )

        var insertedCount = 0
        var hasError = false

        for (event in sampleEvents) {
            db.collection("events").document(event.id).set(event)
                .addOnSuccessListener {
                    insertedCount++
                    Log.d(TAG, "Evento '${event.title}' criado com sucesso (${insertedCount}/${sampleEvents.size}).")
                    if (insertedCount == sampleEvents.size) {
                        onComplete?.invoke(true)
                    }
                }
                .addOnFailureListener { e ->
                    hasError = true
                    Log.e(TAG, "Erro ao criar evento '${event.title}': ${e.message}", e)
                    if (!hasError) {
                        onComplete?.invoke(false)
                    }
                }
        }
    }
}
