package com.dabenxiang.jys

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dabenxiang.jys.R
import kotlinx.android.synthetic.main.uri_item.view.*

class UriAdapter : RecyclerView.Adapter<UriAdapter.UriViewHolder>() {
    private var mUris: List<Uri>? = null
    private var mPaths: List<String>? = null
    fun setData(uris: List<Uri>?, paths: List<String>?) {
        mUris = uris
        mPaths = paths
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (mUris == null) 0 else mUris!!.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UriViewHolder {
        return UriViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.uri_item, parent, false))
    }

    override fun onBindViewHolder(holder: UriViewHolder, position: Int) {
        val context = holder.itemView.context
        val gr=Glide.with(context)
        val uri =mUris!![position].toString()
        val path = mPaths!![position]
        holder.mUri.text = uri
        holder.mPath.text = path
        holder.mUri.alpha = if (position % 2 == 0) 1.0f else 0.54f
        holder.mPath.alpha = if (position % 2 == 0) 1.0f else 0.54f
        gr.load(Uri.parse(uri)).into(holder.preview_uri)
        gr.load(path).into(holder.preview_path)
    }

    class UriViewHolder(contentView: View) : RecyclerView.ViewHolder(contentView) {
        val mUri: TextView = itemView.uri
        val mPath: TextView = itemView.path
        val preview_uri: ImageView = itemView.preview_uri
        val preview_path: ImageView = itemView.preview_path
    }
}