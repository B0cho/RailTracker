package com.b0cho.railtracker

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class URI_ImageViewAdapter(private val context: Context,
                           val imageURIs: MutableList<Uri> = mutableListOf()
) : RecyclerView.Adapter<URI_ImageViewAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pictureImageView = itemView.findViewById<ImageView>(R.id.thumbnailImageView)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_thumbnail, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = imageURIs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(imageURIs.elementAt(position)).into(holder.pictureImageView)
    }

    fun clear() {
        val lastPos = imageURIs.size
        imageURIs.clear()
        notifyItemRangeRemoved(0, lastPos)
    }

    fun add(items: List<Uri>) {
        val lastPos = imageURIs.size
        imageURIs.addAll(items)
        notifyItemRangeInserted(lastPos, items.size)
    }
}