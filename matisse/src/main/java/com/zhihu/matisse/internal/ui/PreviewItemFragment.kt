/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.zhihu.matisse.ARGS_ITEM
import com.zhihu.matisse.R
import com.zhihu.matisse.databinding.FragmentPreviewItemBinding
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils.Companion.getBitmapSize
import com.zhihu.matisse.listener.OnFragmentInteractionListener
import it.sephiroth.android.library.imagezoom.ImageViewTouch
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase

class PreviewItemFragment : Fragment() {
    private var mBinding: FragmentPreviewItemBinding? = null
    private var mListener: OnFragmentInteractionListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentPreviewItemBinding.inflate(inflater)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireArguments()?.let { arguments ->
            arguments.getParcelable<Item>(ARGS_ITEM)?.let { item ->
                if (item.isVideo) {
                    mBinding!!.videoPlayButton.visibility = View.VISIBLE
                    mBinding!!.videoPlayButton.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(item.uri, "video/*")
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.error_no_video_activity, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    mBinding!!.videoPlayButton.visibility = View.GONE
                }
                val size = getBitmapSize(item.contentUri, requireActivity())
                val context = requireContext()
                if (item.isGif) {
                    SelectionSpec.getInstance().imageEngine.loadGifImage(context, size.x, size.y, mBinding!!.imageView, item.contentUri)
                } else {
                    SelectionSpec.getInstance().imageEngine.loadImage(context, size.x, size.y, mBinding!!.imageView, item.contentUri)
                }
                mBinding!!.imageView.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN
                mBinding!!.imageView.setSingleTapListener {
                    if (mListener != null) {
                        mListener!!.onClick()
                    }
                }
            } ?: run {
                return
            }
        }
    }

    fun resetView() {
        requireView()?.let {view->
            (view.findViewById<View>(R.id.image_view) as ImageViewTouch).resetMatrix()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        fun newInstance(item: Item): PreviewItemFragment {
            val fragment = PreviewItemFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }
}