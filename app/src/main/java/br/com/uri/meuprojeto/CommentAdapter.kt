package br.com.uri.meuprojeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private var comments: List<Comment>,
    private val currentUserId: String,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit,
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    fun updateComments(newComments: List<Comment>) {
        this.comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment, currentUserId, onEditClick, onDeleteClick)
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCommentInitial: TextView = itemView.findViewById(R.id.tvCommentInitial)
        private val tvCommentAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
        private val tvCommentText: TextView = itemView.findViewById(R.id.tvCommentText)
        private val tvCommentDate: TextView = itemView.findViewById(R.id.tvCommentDate)
        private val btnCommentMore: ImageButton = itemView.findViewById(R.id.btnCommentMore)

        fun bind(
            comment: Comment,
            currentUserId: String,
            onEditClick: (Comment) -> Unit,
            onDeleteClick: (Comment) -> Unit,
        ) {
            val authorName = if (comment.userName.isNotEmpty()) comment.userName else "Usuário"
            tvCommentAuthor.text = authorName
            tvCommentInitial.text = authorName.substring(0, 1).uppercase()
            tvCommentText.text = comment.text

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvCommentDate.text = sdf.format(Date(comment.createdAt))

            val isOwner = comment.userId == currentUserId
            if (isOwner) {
                btnCommentMore.visibility = View.VISIBLE
                btnCommentMore.setOnClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    popup.menu.add(0, 1, 0, "Editar")
                    popup.menu.add(0, 2, 1, "Excluir")
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            1 -> {
                                onEditClick(comment)
                                true
                            }
                            2 -> {
                                onDeleteClick(comment)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            } else {
                btnCommentMore.visibility = View.GONE
            }
        }
    }
}
