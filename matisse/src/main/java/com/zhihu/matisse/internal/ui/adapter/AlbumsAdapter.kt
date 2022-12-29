package com.zhihu.matisse.internal.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.SelectionSpec

class AlbumsAdapter : CursorAdapter {

    private var placeholder: Drawable? = null

    constructor(context: Context, c: Cursor?, autoRequery: Boolean) : super(
        context,
        c,
        autoRequery
    ) {
        init(context)
    }

    constructor(context: Context, c: Cursor?, flags: Int) : super(context, c, flags) {
        init(context)
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.album_list_item, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val album = Album.valueOf(cursor)
        view.findViewById<TextView>(R.id.album_name).text = album.getDisplayName(context)
        view.findViewById<TextView>(R.id.album_media_count).text = album.count.toString()

        // do not need to load animated Gif
        SelectionSpec.imageEngine?.loadThumbnail(
            context,
            context.resources.getDimensionPixelSize(R.dimen.media_grid_size),
            placeholder,
            view.findViewById<View>(R.id.album_cover) as ImageView,
            album.coverUri
        )
    }

    private fun init(context: Context) {
        val ta =
            context.theme.obtainStyledAttributes(intArrayOf(R.attr.album_thumbnail_placeholder))
        placeholder = ta.getDrawable(0)
        ta.recycle()
    }
}