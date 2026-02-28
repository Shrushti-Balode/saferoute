package com.example.saferoute.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.data.PoiFts

class SearchSuggestionAdapter(
    private val suggestions: MutableList<PoiFts>,
    private val onItemClick: (PoiFts) -> Unit
) : RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    fun updateData(newSuggestions: List<PoiFts>) {
        suggestions.clear()
        suggestions.addAll(newSuggestions)
        notifyDataSetChanged()
    }

    inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.suggestion_name)
        private val typeTextView: TextView = itemView.findViewById(R.id.suggestion_type)

        fun bind(suggestion: PoiFts) {
            nameTextView.text = suggestion.name
            typeTextView.text = suggestion.type
            itemView.setOnClickListener { onItemClick(suggestion) }
        }
    }
}
