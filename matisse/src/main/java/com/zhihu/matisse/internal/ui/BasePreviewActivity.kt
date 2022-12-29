package com.zhihu.matisse.internal.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter
import com.zhihu.matisse.internal.ui.widget.CheckRadioView
import com.zhihu.matisse.internal.ui.widget.CheckView
import com.zhihu.matisse.internal.ui.widget.IncapableDialog
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import com.zhihu.matisse.listener.OnFragmentInteractionListener

abstract class BasePreviewActivity : AppCompatActivity(),
    View.OnClickListener, OnPageChangeListener, OnFragmentInteractionListener {

    protected var spec = SelectionSpec
    protected val selectedCollection = SelectedItemCollection(this)
    protected var previousPos = -1
    protected var originalEnable = false

    protected var pager: ViewPager? = null
    protected var adapter: PreviewPagerAdapter? = null
    protected var checkView: CheckView? = null
    protected var buttonBack: TextView? = null
    protected var buttonApply: TextView? = null
    protected var size: TextView? = null

    private var originalLayout: LinearLayout? = null
    private var original: CheckRadioView? = null
    private var bottomToolbar: FrameLayout? = null
    private var topToolbar: FrameLayout? = null
    private var isToolbarHide = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(SelectionSpec.themeId)
        super.onCreate(savedInstanceState)
        if (!spec.hasInit) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setContentView(R.layout.activity_media_preview)
        if (spec.needOrientationRestriction) {
            requestedOrientation = spec.orientation
        }
        originalEnable = if (savedInstanceState == null) {
            selectedCollection.onCreate(intent.getBundleExtra(EXTRA_DEFAULT_BUNDLE))
            intent.getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false)
        } else {
            selectedCollection.onCreate(savedInstanceState)
            savedInstanceState.getBoolean(CHECK_STATE)
        }
        buttonBack = findViewById<View>(R.id.button_back) as TextView
        buttonApply = findViewById<View>(R.id.button_apply) as TextView
        size = findViewById<View>(R.id.size) as TextView
        pager = findViewById<View>(R.id.pager) as ViewPager
        checkView = findViewById<View>(R.id.check_view) as CheckView
        bottomToolbar = findViewById(R.id.bottom_toolbar)
        topToolbar = findViewById(R.id.top_toolbar)
        originalLayout = findViewById(R.id.originalLayout)
        original = findViewById(R.id.original)

        adapter = PreviewPagerAdapter(supportFragmentManager, null)
        pager!!.adapter = adapter
        checkView!!.setCountable(spec.countable)

        buttonBack!!.setOnClickListener(this)
        buttonApply!!.setOnClickListener(this)
        pager!!.addOnPageChangeListener(this)
        checkView!!.setOnClickListener {
            val item = adapter!!.getMediaItem(pager!!.currentItem)
            if (selectedCollection.isSelected(item)) {
                selectedCollection.remove(item)
                if (spec.countable) {
                    checkView!!.setCheckedNum(CheckView.UNCHECKED)
                } else {
                    checkView!!.setChecked(false)
                }
            } else {
                if (assertAddSelection(item)) {
                    selectedCollection.add(item)
                    if (spec.countable) {
                        checkView!!.setCheckedNum(selectedCollection.checkedNumOf(item))
                    } else {
                        checkView!!.setChecked(true)
                    }
                }
            }
            updateApplyButton()
            if (spec.onSelectedListener != null) {
                spec.onSelectedListener!!.onSelected(
                    selectedCollection.asListOfUri(), selectedCollection.asListOfString()
                )
            }
        }
        originalLayout!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val count = countOverMaxSize()
                if (count > 0) {
                    val incapableDialog = IncapableDialog.newInstance(
                        "",
                        getString(
                            R.string.error_over_original_count,
                            count,
                            spec.originalMaxSize
                        )
                    )
                    incapableDialog.show(
                        supportFragmentManager,
                        IncapableDialog::class.java.name
                    )
                    return
                }
                originalEnable = !originalEnable
                original!!.setChecked(originalEnable)
                if (!originalEnable) {
                    original!!.setColor(Color.WHITE)
                }
                spec.onCheckedListener?.onCheck(originalEnable)
            }
        })
        updateApplyButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedCollection.onSaveInstanceState(outState)
        outState.putBoolean("checkState", originalEnable)
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
        if (!spec!!.autoHideToolbar) {
            return
        }
        if (isToolbarHide) {
            topToolbar!!.animate()
                .setInterpolator(FastOutSlowInInterpolator())
                .translationYBy(topToolbar!!.measuredHeight.toFloat())
                .start()
            bottomToolbar!!.animate()
                .translationYBy(-bottomToolbar!!.measuredHeight.toFloat())
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        } else {
            topToolbar!!.animate()
                .setInterpolator(FastOutSlowInInterpolator())
                .translationYBy(-topToolbar!!.measuredHeight.toFloat())
                .start()
            bottomToolbar!!.animate()
                .setInterpolator(FastOutSlowInInterpolator())
                .translationYBy(bottomToolbar!!.measuredHeight.toFloat())
                .start()
        }
        isToolbarHide = !isToolbarHide
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        val adapter = pager!!.adapter as PreviewPagerAdapter?
        if (previousPos != -1 && previousPos != position) {
            (adapter!!.instantiateItem((pager)!!, previousPos) as PreviewItemFragment).resetView()
            val item = adapter.getMediaItem(position)
            if (spec.countable) {
                val checkedNum = selectedCollection.checkedNumOf(item)
                checkView!!.setCheckedNum(checkedNum)
                if (checkedNum > 0) {
                    checkView!!.isEnabled = true
                } else {
                    checkView!!.isEnabled = !selectedCollection.maxSelectableReached()
                }
            } else {
                val checked = selectedCollection.isSelected(item)
                checkView!!.setChecked(checked)
                if (checked) {
                    checkView!!.isEnabled = true
                } else {
                    checkView!!.isEnabled = !selectedCollection.maxSelectableReached()
                }
            }
            updateSize(item)
        }
        previousPos = position
    }

    override fun onPageScrollStateChanged(state: Int) {}
    private fun updateApplyButton() {
        val selectedCount = selectedCollection.count()
        if (selectedCount == 0) {
            buttonApply!!.setText(R.string.button_apply_default)
            buttonApply!!.isEnabled = false
        } else if (selectedCount == 1 && spec.singleSelectionModeEnabled) {
            buttonApply!!.setText(R.string.button_apply_default)
            buttonApply!!.isEnabled = true
        } else {
            buttonApply!!.isEnabled = true
            buttonApply!!.text = getString(R.string.button_apply, selectedCount)
        }
        if (spec.originalable) {
            originalLayout!!.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            originalLayout!!.visibility = View.GONE
        }
    }

    private fun updateOriginalState() {
        original!!.setChecked(originalEnable)
        if (!originalEnable) {
            original!!.setColor(Color.WHITE)
        }
        if (countOverMaxSize() > 0) {
            if (originalEnable) {
                val incapableDialog = IncapableDialog.newInstance(
                    "",
                    getString(R.string.error_over_original_size, spec.originalMaxSize)
                )
                incapableDialog.show(
                    supportFragmentManager,
                    IncapableDialog::class.java.name
                )
                original!!.setChecked(false)
                original!!.setColor(Color.WHITE)
                originalEnable = false
            }
        }
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = selectedCollection.count()
        for (i in 0 until selectedCount) {
            val item = selectedCollection.asList()[i]
            if (item.isImage) {
                val size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > spec.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    protected fun updateSize(item: Item) {
        if (item.isGif) {
            size!!.visibility = View.VISIBLE
            size!!.text = "${PhotoMetadataUtils.getSizeInMB(item.size).toString()}M"
        } else {
            size!!.visibility = View.GONE
        }
        if (item.isVideo) {
            originalLayout!!.visibility = View.GONE
        } else if (spec.originalable) {
            originalLayout!!.visibility = View.VISIBLE
        }
    }

    protected fun sendBackResult(apply: Boolean) {
        val intent = Intent()
        intent.putExtra(EXTRA_RESULT_BUNDLE, selectedCollection.dataWithBundle)
        intent.putExtra(EXTRA_RESULT_APPLY, apply)
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        setResult(RESULT_OK, intent)
    }

    private fun assertAddSelection(item: Item): Boolean {
        val cause: IncapableCause? = selectedCollection.isAcceptable(item)
        IncapableCause.handleCause(this, cause)
        return cause == null
    }

    companion object {
        const val EXTRA_DEFAULT_BUNDLE = "extra_default_bundle"
        const val EXTRA_RESULT_BUNDLE = "extra_result_bundle"
        const val EXTRA_RESULT_APPLY = "extra_result_apply"
        const val EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable"
        const val CHECK_STATE = "checkState"
    }
}