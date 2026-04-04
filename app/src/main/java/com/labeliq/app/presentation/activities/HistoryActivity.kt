package com.labeliq.app.presentation.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.labeliq.app.data.local.loadScanHistory
import com.labeliq.app.databinding.ActivityHistoryBinding
import com.labeliq.app.presentation.adapters.HistoryAdapter

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar back navigation ──────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Load scan history ────────────────────────────────────────
        val history = loadScanHistory(this)
        Log.d("HISTORY", "Loaded history size: ${history.size}")
        for (item in history) {
            Log.d("HISTORY_ITEM", item.toString())
        }

        // ── Empty state ──────────────────────────────────────────────
        if (history.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerHistory.visibility = View.GONE
            return
        }

        // ── Setup RecyclerView ───────────────────────────────────────
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = HistoryAdapter(history)
    }
}
