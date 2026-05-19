package com.univiptv.orionhub.site.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.univiptv.orionhub.site.R
import com.univiptv.orionhub.site.data.model.CategoryInfo

class CategoryAdapter(
    private val onClick: (CategoryInfo) -> Unit
) : ListAdapter<CategoryInfo, CategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCategoryCount)
        private val badgeOnline: TextView = itemView.findViewById(R.id.badgeCatOnline)
        private val badgeOffline: TextView = itemView.findViewById(R.id.badgeCatOffline)

        fun bind(info: CategoryInfo) {
            tvName.text = info.name
            tvCount.text = itemView.context.getString(R.string.channels_count, info.totalChannels)
            badgeOnline.text = itemView.context.getString(R.string.online_count, info.onlineChannels)
            badgeOffline.text = itemView.context.getString(R.string.offline_count, info.offlineChannels)
            itemView.setOnClickListener { onClick(info) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryInfo>() {
        override fun areItemsTheSame(oldItem: CategoryInfo, newItem: CategoryInfo) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: CategoryInfo, newItem: CategoryInfo) = oldItem == newItem
    }
}
