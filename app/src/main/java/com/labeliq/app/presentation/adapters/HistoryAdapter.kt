package com.labeliq.app.presentation.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.labeliq.app.data.local.ScanResult
import com.labeliq.app.databinding.ItemHistoryBinding
import com.labeliq.app.presentation.activities.ResultDetailActivity
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
                "HIGH_RISK" -> "❌  High Risk"      to "#EF5350"
                "MODERATE"  -> "⚠️  Moderate Risk" to "#FFA726"
                else        -> "✅  Safe"            to "#66BB6A"
            }
            binding.tvItemStatus.text = "Status: $label"
            binding.tvItemStatus.setTextColor(Color.parseColor(color))

            // ── Formatted timestamp ──────────────────────────────────
            val formattedTime = dateFormat.format(Date(item.timestamp))
            binding.tvItemTimestamp.text = "Time: $formattedTime"

            // ── Click → open ResultDetailActivity ────────────────────
            binding.root.setOnClickListener {
                val ctx = binding.root.context
                val intent = Intent(ctx, ResultDetailActivity::class.java).apply {
                    putExtra(ResultDetailActivity.EXTRA_STATUS,    item.status)
                    putExtra(ResultDetailActivity.EXTRA_TIMESTAMP, item.timestamp)
                    putStringArrayListExtra(ResultDetailActivity.EXTRA_HIGH_RISK, ArrayList(item.highRisk))
                    putStringArrayListExtra(ResultDetailActivity.EXTRA_MODERATE,  ArrayList(item.moderate))
                    putStringArrayListExtra(ResultDetailActivity.EXTRA_SAFE,      ArrayList(item.safe))
                    putExtra(ResultDetailActivity.EXTRA_IMAGE_PATH, item.imagePath)
                }
                ctx.startActivity(intent)
            }
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
