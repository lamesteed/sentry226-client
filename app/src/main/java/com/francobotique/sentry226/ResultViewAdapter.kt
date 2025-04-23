package com.francobotique.sentry226

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.francobotique.sentry226.viewmodel.data.ResultListItem

class ResultViewAdapter(
    private var resultList: List<ResultListItem>,
    private val onDownloadClick: (ResultListItem) -> Unit
) : RecyclerView.Adapter<ResultViewAdapter.ResultViewHolder>() {

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.textViewFileName)
        val statusIcon: ImageView = itemView.findViewById(R.id.imageViewStatus)
        val downloadButton: ImageButton = itemView.findViewById(R.id.buttonDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val resultItem = resultList[position]
        holder.fileName.text = resultItem.fileName
        if (resultItem.isDownloaded) {
            holder.statusIcon.visibility = View.VISIBLE
        } else {
            holder.statusIcon.visibility = View.INVISIBLE
        }

        holder.downloadButton.setOnClickListener { onDownloadClick(resultItem) }
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    fun updateData(newResultList: List<ResultListItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = resultList.size
            override fun getNewListSize() = newResultList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return resultList[oldItemPosition].fileName == newResultList[newItemPosition].fileName
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return resultList[oldItemPosition] == newResultList[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        resultList = newResultList
        diffResult.dispatchUpdatesTo(this)
    }
}