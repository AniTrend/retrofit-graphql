package io.github.wax911.retgraph.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.wax911.retgraph.R
import io.github.wax911.retgraph.model.parent.Entry
import java.util.*

class AdapterExample : RecyclerView.Adapter<AdapterExample.EntryAdapter>() {

    private val model by lazy { ArrayList<Entry>() }

    val isDataSetEmpty: Boolean
        get() = model.isEmpty()

    fun addItems(model: List<Entry>) {
        this.model.addAll(model)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryAdapter {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_example, parent, false)
        return EntryAdapter(view)
    }

    override fun onBindViewHolder(holder: EntryAdapter, position: Int) {
        holder.bindModel(model[position])
    }

    override fun getItemCount(): Int {
        return model.size
    }

    /** View Holder for our list items  */
    inner class EntryAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val repoName by lazy {
            itemView.findViewById<TextView>(R.id.repoName)
        }
        private val postedBy by lazy {
            itemView.findViewById<TextView>(R.id.postedBy) }
        private val repoStars by lazy {
            itemView.findViewById<TextView>(R.id.repoStars)
        }
        private val ownerAvatar by lazy {
            itemView.findViewById<ImageView>(R.id.ownerAvatar)
        }

        fun bindModel(data: Entry) {
            val repository = data.repository
            if (repository != null) {
                val user = repository.owner
                Glide.with(ownerAvatar)
                        .load(user!!.avatar_url)
                        .into(ownerAvatar)
                repoName.text = repository.full_name
                repoStars.text = String.format(Locale.getDefault(), " %d", repository.stargazers_count)
            } else {
                repoName.text = "Unknown"
                repoStars.text = "--"
            }
            postedBy.text = data.postedBy!!.login
        }
    }
}
