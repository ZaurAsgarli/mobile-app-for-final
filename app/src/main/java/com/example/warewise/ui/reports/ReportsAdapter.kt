package com.example.warewise.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.warewise.ReportItem
import com.example.warewise.databinding.ItemReportBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsAdapter(private var reports: List<ReportItem>) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.binding.tvReportDescription.text = report.description
        
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = Date(report.timestamp.toLong())
        holder.binding.tvReportTimestamp.text = sdf.format(date)
    }

    override fun getItemCount(): Int = reports.size

    fun updateList(newReports: List<ReportItem>) {
        reports = newReports
        notifyDataSetChanged()
    }
}
