package com.zhihu.matisse.internal.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import com.zhihu.matisse.listener.OnFragmentInteractionListener
import com.zhihu.matisse.utils.ImageUtil.isLongBitmap
import it.sephiroth.android.library.imagezoom.ImageViewTouch
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase

class PreviewItemFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item: Item = requireArguments().getParcelable(ARGS_ITEM) ?: return
        val videoPlayButton = view.findViewById<View>(R.id.video_play_button)
        if (item.isVideo) {
            videoPlayButton.visibility = View.VISIBLE
            videoPlayButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(item.contentUri, "video/*")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_no_video_activity,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            videoPlayButton.visibility = View.GONE
        }
        val imageLong = view.findViewById<View>(R.id.image_view_long) as SubsamplingScaleImageView
        val image = view.findViewById<View>(R.id.image_view) as ImageViewTouch
        if (isLongBitmap(requireContext(), item.contentUri)) {
            imageLong.setExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            imageLong.visibility = View.VISIBLE
            image.visibility = View.GONE
            SelectionSpec.imageEngine?.loadLargeImage(
                requireContext(),
                0, 0,
                imageLong,
                item.contentUri
            )
        } else {
            val size = PhotoMetadataUtils.getBitmapSize(item.contentUri, requireActivity())
            image.visibility = View.VISIBLE
            imageLong.visibility = View.GONE
            image.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN
            image.setSingleTapListener {
                listener?.onClick()
            }
            if (item.isGif) {
                SelectionSpec.imageEngine?.loadGifImage(
                    requireContext(),
                    size.x, size.y,
                    image,
                    item.contentUri
                )
            } else {
                SelectionSpec.imageEngine?.loadImage(
                    requireContext(),
                    size.x, size.y,
                    image,
                    item.contentUri
                )
            }
        }
    }

    fun resetView() {
        (view?.findViewById<View>(R.id.image_view) as? ImageViewTouch)?.resetMatrix()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        private const val ARGS_ITEM = "args_item"

        fun newInstance(item: Item?): PreviewItemFragment {
            val bundle = Bundle().apply {
                putParcelable(ARGS_ITEM, item)
            }
            val fragment = PreviewItemFragment().apply {
                arguments = bundle
            }
            return fragment
        }
    }
}