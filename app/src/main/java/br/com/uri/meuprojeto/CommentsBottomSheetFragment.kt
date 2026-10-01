package br.com.uri.meuprojeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var eventId: String = ""
    private var commentsListener: ListenerRegistration? = null

    private lateinit var rvComments: RecyclerView
    private lateinit var tvNoComments: TextView
    private lateinit var commentsProgressBar: ProgressBar
    private lateinit var etCommentInput: EditText
    private lateinit var btnSendComment: Button

    private lateinit var commentAdapter: CommentAdapter
    private var editingComment: Comment? = null

    companion object {
        private const val ARG_EVENT_ID = "arg_event_id"

        fun newInstance(eventId: String): CommentsBottomSheetFragment {
            val fragment = CommentsBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_EVENT_ID, eventId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = arguments?.getString(ARG_EVENT_ID) ?: ""
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        if (currentUser == null || eventId.isEmpty()) {
            dismiss()
            return
        }

        rvComments = view.findViewById(R.id.rvComments)
        tvNoComments = view.findViewById(R.id.tvNoComments)
        commentsProgressBar = view.findViewById(R.id.commentsProgressBar)
        etCommentInput = view.findViewById(R.id.etCommentInput)
        btnSendComment = view.findViewById(R.id.btnSendComment)

        rvComments.layoutManager = LinearLayoutManager(requireContext())
        commentAdapter = CommentAdapter(
            comments = emptyList(),
            currentUserId = currentUser.uid,
            onEditClick = { comment -> startEditingComment(comment) },
            onDeleteClick = { comment -> confirmDeleteComment(comment) },
        )
        rvComments.adapter = commentAdapter

        btnSendComment.setOnClickListener {
            val text = etCommentInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Digite um comentário", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editingComment != null) {
                // Modo Edição
                updateCommentText(editingComment!!, text)
            } else {
                // Modo Novo Comentário
                createComment(currentUser.uid, currentUser.displayName ?: "Usuário", text)
            }
        }

        loadCommentsRealtime()
    }

    private fun loadCommentsRealtime() {
        commentsProgressBar.visibility = View.VISIBLE

        commentsListener = db.collection("events")
            .document(eventId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                commentsProgressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(requireContext(), "Erro ao carregar comentários: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val commentsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)
                    }

                    // Sincroniza a contagem exata dos comentários diretamente com o total de documentos
                    db.collection("events").document(eventId).update("commentCount", commentsList.size)

                    if (commentsList.isEmpty()) {
                        tvNoComments.visibility = View.VISIBLE
                        rvComments.visibility = View.GONE
                    } else {
                        tvNoComments.visibility = View.GONE
                        rvComments.visibility = View.VISIBLE
                        commentAdapter.updateComments(commentsList)
                    }
                }
            }
    }

    private fun createComment(userId: String, userName: String, text: String) {
        btnSendComment.isEnabled = false

        val commentsRef = db.collection("events").document(eventId).collection("comments")
        val commentId = commentsRef.document().id

        val newComment = Comment(
            id = commentId,
            eventId = eventId,
            userId = userId,
            userName = userName,
            text = text,
            createdAt = System.currentTimeMillis(),
        )

        commentsRef.document(commentId).set(newComment)
            .addOnSuccessListener {
                etCommentInput.setText("")
                btnSendComment.isEnabled = true
                Toast.makeText(requireContext(), "Comentário publicado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                btnSendComment.isEnabled = true
                Toast.makeText(requireContext(), "Erro ao publicar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startEditingComment(comment: Comment) {
        editingComment = comment
        etCommentInput.setText(comment.text)
        etCommentInput.requestFocus()
        btnSendComment.text = "Salvar"
    }

    private fun updateCommentText(comment: Comment, newText: String) {
        btnSendComment.isEnabled = false

        db.collection("events")
            .document(eventId)
            .collection("comments")
            .document(comment.id)
            .update("text", newText)
            .addOnSuccessListener {
                editingComment = null
                etCommentInput.setText("")
                btnSendComment.text = "Publicar"
                btnSendComment.isEnabled = true
                Toast.makeText(requireContext(), "Comentário atualizado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                btnSendComment.isEnabled = true
                Toast.makeText(requireContext(), "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmDeleteComment(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Comentário")
            .setMessage("Tem certeza de que deseja excluir seu comentário?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteComment(comment)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteComment(comment: Comment) {
        db.collection("events")
            .document(eventId)
            .collection("comments")
            .document(comment.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Comentário excluído!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commentsListener?.remove()
    }
}
