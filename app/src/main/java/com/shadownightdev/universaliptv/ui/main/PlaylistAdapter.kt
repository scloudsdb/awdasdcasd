package com.shadownightdev.universaliptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shadownightdev.universaliptv.R
import com.shadownightdev.universaliptv.data.model.Playlist

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onMenuClick: (Playlist, View) -> Unit
) : ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvPlaylistName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvChannelCount)
        private val badgeOnline: TextView = itemView.findViewById(R.id.badgeOnline)
        private val badgeOffline: TextView = itemView.findViewById(R.id.badgeOffline)
        private val progress: ProgressBar = itemView.findViewById(R.id.playlistProgress)
        private val btnMenu: ImageButton = itemView.findViewById(R.id.btnPlaylistMenu)

        fun bind(playlist: Playlist) {
            tvName.text = playlist.name
            tvCount.text = itemView.context.getString(R.string.channels_count, playlist.totalChannels)
            badgeOnline.text = itemView.context.getString(R.string.online_count, playlist.onlineChannels)
            badgeOffline.text = itemView.context.getString(R.string.offline_count, playlist.offlineChannels)
            progress.visibility = if (playlist.isLoading) View.VISIBLE else View.GONE
            btnMenu.visibility = if (playlist.isLoading) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onPlaylistClick(playlist) }
            btnMenu.setOnClickListener { onMenuClick(playlist, it) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist) = oldItem == newItem
    }
}
