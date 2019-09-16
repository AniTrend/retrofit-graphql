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

    val model = ArrayList<Entry>()

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

        private val description by lazy {
            itemView.findViewById<TextView>(R.id.description)
        }
        private val ownerAvatar by lazy {
            itemView.findViewById<ImageView>(R.id.ownerAvatar)
        }

        fun bindModel(data: Entry) {
            postedBy.text = data.postedBy?.login
            data.repository?.apply {
                Glide.with(ownerAvatar)
                        .load(owner?.avatar_url ?: "")
                        .into(ownerAvatar)
                repoName.text = name
                description.text = full_name
            }
        }
    }
}
