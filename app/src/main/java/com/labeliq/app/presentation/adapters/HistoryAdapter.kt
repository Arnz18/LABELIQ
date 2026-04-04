package com.labeliq.app.presentation.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.labeliq.app.data.local.ScanResult
import com.labeliq.app.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val items: List<ScanResult>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    // ── ViewHolder ────────────────────────────────────────────────────────────
    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScanResult) {
            // ── Status label + color ─────────────────────────────────
            val (label, color) = when (item.status) {
                "HIGH_RISK" -> "❌  High Risk"  to "#EF5350"
                "MODERATE"  -> "⚠️  Moderate Risk" to "#FFA726"
                else        -> "✅  Safe"        to "#66BB6A"
            }
            binding.tvItemStatus.text = "Status: $label"
            binding.tvItemStatus.setTextColor(Color.parseColor(color))

            // ── Formatted timestamp ──────────────────────────────────
            val formattedTime = dateFormat.format(Date(item.timestamp))
            binding.tvItemTimestamp.text = "Time: $formattedTime"
        }
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
