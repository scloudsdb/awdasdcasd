package com.univiptv.orionhub.site.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.univiptv.orionhub.site.R
import com.univiptv.orionhub.site.data.model.Channel

class ChannelAdapter(
    private val onClick: (Channel) -> Unit,
    private val onLongClick: ((Channel) -> Unit)? = null
) : ListAdapter<Channel, ChannelAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvChannelName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvChannelCategory)
        private val statusDot: View = itemView.findViewById(R.id.statusDot)
        private val badgeStatus: TextView = itemView.findViewById(R.id.badgeStatus)
        private val ivLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)

        fun bind(channel: Channel) {
            tvName.text = channel.name
            tvCategory.text = channel.category

            val logo = channel.logoUrl
            if (!logo.isNullOrEmpty()) {
                ivLogo.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(logo)
                    .circleCrop()
                    .placeholder(R.drawable.ic_tv)
                    .error(R.drawable.ic_tv)
                    .into(ivLogo)
            } else {
                ivLogo.visibility = View.GONE
            }

            if (channel.isOnline) {
                statusDot.setBackgroundResource(R.drawable.status_online)
                badgeStatus.text = itemView.context.getString(R.string.online)
                badgeStatus.setBackgroundResource(R.drawable.badge_online)
            } else {
                statusDot.setBackgroundResource(R.drawable.status_offline)
                badgeStatus.text = itemView.context.getString(R.string.offline)
                badgeStatus.setBackgroundResource(R.drawable.badge_offline)
            }
            itemView.setOnClickListener { onClick(channel) }
            onLongClick?.let { longCallback ->
                itemView.setOnLongClickListener {
                    longCallback(channel)
                    true
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}
