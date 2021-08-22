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

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.zhihu.matisse.*
import com.zhihu.matisse.databinding.ActivityMediaPreviewBinding
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter
import com.zhihu.matisse.internal.ui.widget.CheckView
import com.zhihu.matisse.internal.ui.widget.IncapableDialog
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils.Companion.getSizeInMB
import com.zhihu.matisse.internal.utils.Platform.hasKitKat
import com.zhihu.matisse.listener.OnFragmentInteractionListener

abstract class BasePreviewActivity : AppCompatActivity(), View.OnClickListener, OnPageChangeListener, OnFragmentInteractionListener {
    @JvmField
    protected var mBinding: ActivityMediaPreviewBinding? = null
    @JvmField
    protected val mSelectedCollection = SelectedItemCollection(this)
    protected  lateinit var mSpec: SelectionSpec
    @JvmField
    protected var mAdapter: PreviewPagerAdapter? = null
    @JvmField
    protected var mPreviousPos = -1
    protected var mOriginalEnable = false
    private var mIsToolbarHide = false
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(SelectionSpec.getInstance().themeId)
        super.onCreate(savedInstanceState)
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        mBinding = ActivityMediaPreviewBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)
        if (hasKitKat()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        mSpec = SelectionSpec.getInstance()
        if (mSpec.needOrientationRestriction()) {
            requestedOrientation = mSpec.orientation
        }
        mOriginalEnable = if (savedInstanceState == null) {
            mSelectedCollection.onCreate(intent.getBundleExtra(EXTRA_DEFAULT_BUNDLE))
            intent.getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false)
        } else {
            mSelectedCollection.onCreate(savedInstanceState)
            savedInstanceState.getBoolean(CHECK_STATE)
        }
        mBinding!!.buttonBack.setOnClickListener(this)
        mBinding!!.buttonApply.setOnClickListener(this)
        mBinding!!.pager.addOnPageChangeListener(this)
        mAdapter = PreviewPagerAdapter(supportFragmentManager, null)
        mBinding!!.pager.adapter = mAdapter
        mBinding!!.checkView.setCountable(mSpec.countable)
        mBinding!!.checkView.setOnClickListener {
            val item = mAdapter!!.getMediaItem(mBinding!!.pager.currentItem)
            if (mSelectedCollection.isSelected(item)) {
                mSelectedCollection.remove(item)
                if (mSpec.countable) {
                    mBinding!!.checkView.setCheckedNum(CheckView.UNCHECKED)
                } else {
                    mBinding!!.checkView.setChecked(false)
                }
            } else {
                if (assertAddSelection(item)) {
                    mSelectedCollection.add(item)
                    if (mSpec.countable) {
                        mBinding!!.checkView.setCheckedNum(mSelectedCollection.checkedNumOf(item))
                    } else {
                        mBinding!!.checkView.setChecked(true)
                    }
                }
            }
            updateApplyButton()
            if (mSpec.onSelectedListener != null) {
                mSpec.onSelectedListener.onSelected(
                        mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
            }
        }
        mBinding!!.originalLayout.setOnClickListener(View.OnClickListener {
            val count = countOverMaxSize()
            if (count > 0) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_count, count, mSpec.originalMaxSize))
                incapableDialog.show(supportFragmentManager,
                        IncapableDialog::class.java.name)
                return@OnClickListener
            }
            mOriginalEnable = !mOriginalEnable
            mBinding!!.original.setChecked(mOriginalEnable)
            if (!mOriginalEnable) {
                mBinding!!.original.setColor(Color.WHITE)
            }
            if (mSpec.onCheckedListener != null) {
                mSpec.onCheckedListener.onCheck(mOriginalEnable)
            }
        })
        updateApplyButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mSelectedCollection.onSaveInstanceState(outState)
        outState.putBoolean(CHECK_STATE, mOriginalEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        sendBackResult(false)
        super.onBackPressed()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.button_back) {
            onBackPressed()
        } else if (v.id == R.id.button_apply) {
            sendBackResult(true)
            finish()
        }
    }

    override fun onClick() {
        if (!mSpec!!.autoHideToobar) {
            return
        }
        if (mIsToolbarHide) {
            mBinding!!.topToolbar.animate()
                    .setInterpolator(FastOutSlowInInterpolator())
                    .translationYBy(mBinding!!.topToolbar.measuredHeight.toFloat())
                    .start()
            mBinding!!.bottomToolbar.animate()
                    .translationYBy(-mBinding!!.bottomToolbar.measuredHeight.toFloat())
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
        } else {
            mBinding!!.topToolbar.animate()
                    .setInterpolator(FastOutSlowInInterpolator())
                    .translationYBy(-mBinding!!.topToolbar.measuredHeight.toFloat())
                    .start()
            mBinding!!.bottomToolbar.animate()
                    .setInterpolator(FastOutSlowInInterpolator())
                    .translationYBy(mBinding!!.bottomToolbar.measuredHeight.toFloat())
                    .start()
        }
        mIsToolbarHide = !mIsToolbarHide
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        val adapter = mBinding!!.pager.adapter as PreviewPagerAdapter?
        if (mPreviousPos != -1 && mPreviousPos != position) {
            (adapter!!.instantiateItem(mBinding!!.pager, mPreviousPos) as PreviewItemFragment).resetView()
            val item = adapter.getMediaItem(position)
            if (mSpec!!.countable) {
                val checkedNum = mSelectedCollection.checkedNumOf(item)
                mBinding!!.checkView.setCheckedNum(checkedNum)
                if (checkedNum > 0) {
                    mBinding!!.checkView.isEnabled = true
                } else {
                    mBinding!!.checkView.isEnabled = !mSelectedCollection.maxSelectableReached()
                }
            } else {
                val checked = mSelectedCollection.isSelected(item)
                mBinding!!.checkView.setChecked(checked)
                if (checked) {
                    mBinding!!.checkView.isEnabled = true
                } else {
                    mBinding!!.checkView.isEnabled = !mSelectedCollection.maxSelectableReached()
                }
            }
            updateSize(item)
        }
        mPreviousPos = position
    }

    override fun onPageScrollStateChanged(state: Int) {}
    private fun updateApplyButton() {
        val selectedCount = mSelectedCollection.count()
        if (selectedCount == 0) {
            mBinding!!.buttonApply.setText(R.string.button_apply_default)
            mBinding!!.buttonApply.isEnabled = false
        } else if (selectedCount == 1 && mSpec!!.singleSelectionModeEnabled()) {
            mBinding!!.buttonApply.setText(R.string.button_apply_default)
            mBinding!!.buttonApply.isEnabled = true
        } else {
            mBinding!!.buttonApply.isEnabled = true
            mBinding!!.buttonApply.text = getString(R.string.button_apply, selectedCount)
        }
        if (mSpec!!.originalable) {
            mBinding!!.originalLayout.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            mBinding!!.originalLayout.visibility = View.GONE
        }
    }

    private fun updateOriginalState() {
        mBinding!!.original.setChecked(mOriginalEnable)
        if (!mOriginalEnable) {
            mBinding!!.original.setColor(Color.WHITE)
        }
        if (countOverMaxSize() > 0) {
            if (mOriginalEnable) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec!!.originalMaxSize))
                incapableDialog.show(supportFragmentManager,
                        IncapableDialog::class.java.name)
                mBinding!!.original.setChecked(false)
                mBinding!!.original.setColor(Color.WHITE)
                mOriginalEnable = false
            }
        }
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = mSelectedCollection.count()
        for (i in 0 until selectedCount) {
            val item = mSelectedCollection.asList()[i]
            if (item.isImage) {
                val size = getSizeInMB(item.size)
                if (size > mSpec!!.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    protected fun updateSize(item: Item) {
        if (item.isGif) {
            mBinding!!.size.visibility = View.VISIBLE
            mBinding!!.size.text = getSizeInMB(item.size).toString() + "M"
        } else {
            mBinding!!.size.visibility = View.GONE
        }
        if (item.isVideo) {
            mBinding!!.originalLayout.visibility = View.GONE
        } else if (mSpec!!.originalable) {
            mBinding!!.originalLayout.visibility = View.VISIBLE
        }
    }

    protected fun sendBackResult(apply: Boolean) {
        val intent = Intent()
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.dataWithBundle)
        intent.putExtra(EXTRA_RESULT_APPLY, apply)
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
        setResult(RESULT_OK, intent)
    }

    private fun assertAddSelection(item: Item): Boolean {
        val cause = mSelectedCollection.isAcceptable(item)
        IncapableCause.handleCause(this, cause)
        return cause == null
    }
}