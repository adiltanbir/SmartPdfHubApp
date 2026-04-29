package com.smartpdfhub.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip  // BUG FIX #10: tvSource is a Chip in the layout, not a MaterialTextView
import com.google.android.material.textview.MaterialTextView
import com.smartpdfhub.R
import com.smartpdfhub.data.model.PDFFile

class PDFAdapter(
    private val onItemClick: (PDFFile) -> Unit,
    private val onFavoriteClick: (PDFFile) -> Unit,
    private val onShareClick: (PDFFile) -> Unit
) : ListAdapter<PDFFile, PDFAdapter.PDFViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PDFViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf, parent, false)
        return PDFViewHolder(view)
    }

    override fun onBindViewHolder(holder: PDFViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PDFViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val tvName: MaterialTextView = itemView.findViewById(R.id.tvFileName)
        private val tvSize: MaterialTextView = itemView.findViewById(R.id.tvFileSize)
        private val tvDate: MaterialTextView = itemView.findViewById(R.id.tvDate)
        // BUG FIX #10: item_pdf.xml uses Chip for tvSource, not MaterialTextView — caused ClassCastException
        private val tvSource: Chip = itemView.findViewById(R.id.tvSource)
        private val btnFavorite: MaterialButton = itemView.findViewById(R.id.btnFavorite)
        private val btnShare: MaterialButton = itemView.findViewById(R.id.btnShare)

        fun bind(pdf: PDFFile) {
            tvName.text = pdf.displayName
            tvSize.text = pdf.formattedSize
            tvDate.text = pdf.formattedDate
            tvSource.text = pdf.sourceType.displayName

            btnFavorite.icon = itemView.context.getDrawable(
                if (pdf.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )

            cardView.setOnClickListener { onItemClick(pdf) }
            btnFavorite.setOnClickListener { onFavoriteClick(pdf) }
            btnShare.setOnClickListener { onShareClick(pdf) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PDFFile>() {
        override fun areItemsTheSame(oldItem: PDFFile, newItem: PDFFile) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PDFFile, newItem: PDFFile) = oldItem == newItem
    }
}
