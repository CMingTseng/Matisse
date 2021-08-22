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
package com.zhihu.matisse.internal.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.zhihu.matisse.R;
import com.zhihu.matisse.databinding.ActivityMediaPreviewBinding;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.ui.widget.IncapableDialog;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;
import com.zhihu.matisse.internal.utils.Platform;
import com.zhihu.matisse.listener.OnFragmentInteractionListener;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager.widget.ViewPager;

import static com.zhihu.matisse.ConstantKt.CHECK_STATE;
import static com.zhihu.matisse.ConstantKt.EXTRA_DEFAULT_BUNDLE;
import static com.zhihu.matisse.ConstantKt.EXTRA_RESULT_APPLY;
import static com.zhihu.matisse.ConstantKt.EXTRA_RESULT_BUNDLE;
import static com.zhihu.matisse.ConstantKt.EXTRA_RESULT_ORIGINAL_ENABLE;


public abstract class BasePreviewActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener, OnFragmentInteractionListener {
    protected ActivityMediaPreviewBinding mBinding;
    protected final SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
    protected SelectionSpec mSpec;
    protected PreviewPagerAdapter mAdapter;
    protected int mPreviousPos = -1;
    protected boolean mOriginalEnable;
    private boolean mIsToolbarHide = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(SelectionSpec.getInstance().themeId);
        super.onCreate(savedInstanceState);
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        mBinding = ActivityMediaPreviewBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        if (Platform.hasKitKat()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mSpec = SelectionSpec.getInstance();
        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
            mOriginalEnable = getIntent().getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false);
        } else {
            mSelectedCollection.onCreate(savedInstanceState);
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        mBinding.buttonBack.setOnClickListener(this);
        mBinding.buttonApply.setOnClickListener(this);

        mBinding.pager.addOnPageChangeListener(this);
        mAdapter = new PreviewPagerAdapter(getSupportFragmentManager(), null);
        mBinding.pager.setAdapter(mAdapter);
        mBinding.checkView.setCountable(mSpec.countable);

        mBinding.checkView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Item item = mAdapter.getMediaItem(mBinding.pager.getCurrentItem());
                if (mSelectedCollection.isSelected(item)) {
                    mSelectedCollection.remove(item);
                    if (mSpec.countable) {
                        mBinding.checkView.setCheckedNum(CheckView.UNCHECKED);
                    } else {
                        mBinding.checkView.setChecked(false);
                    }
                } else {
                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item);
                        if (mSpec.countable) {
                            mBinding.checkView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                        } else {
                            mBinding.checkView.setChecked(true);
                        }
                    }
                }
                updateApplyButton();

                if (mSpec.onSelectedListener != null) {
                    mSpec.onSelectedListener.onSelected(
                            mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
                }
            }
        });
        mBinding.originalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int count = countOverMaxSize();
                if (count > 0) {
                    IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                            getString(R.string.error_over_original_count, count, mSpec.originalMaxSize));
                    incapableDialog.show(getSupportFragmentManager(),
                            IncapableDialog.class.getName());
                    return;
                }

                mOriginalEnable = !mOriginalEnable;
                mBinding.original.setChecked(mOriginalEnable);
                if (!mOriginalEnable) {
                    mBinding.original.setColor(Color.WHITE);
                }


                if (mSpec.onCheckedListener != null) {
                    mSpec.onCheckedListener.onCheck(mOriginalEnable);
                }
            }
        });

        updateApplyButton();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mSelectedCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        sendBackResult(false);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_back) {
            onBackPressed();
        } else if (v.getId() == R.id.button_apply) {
            sendBackResult(true);
            finish();
        }
    }

    @Override
    public void onClick() {
        if (!mSpec.autoHideToobar) {
            return;
        }

        if (mIsToolbarHide) {
            mBinding.topToolbar.animate()
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .translationYBy(mBinding.topToolbar.getMeasuredHeight())
                    .start();
            mBinding.bottomToolbar.animate()
                    .translationYBy(-mBinding.bottomToolbar.getMeasuredHeight())
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        } else {
            mBinding.topToolbar.animate()
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .translationYBy(-mBinding.topToolbar.getMeasuredHeight())
                    .start();
            mBinding.bottomToolbar.animate()
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .translationYBy(mBinding.bottomToolbar.getMeasuredHeight())
                    .start();
        }

        mIsToolbarHide = !mIsToolbarHide;

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mBinding.pager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((PreviewItemFragment) adapter.instantiateItem(mBinding.pager, mPreviousPos)).resetView();

            Item item = adapter.getMediaItem(position);
            if (mSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                mBinding.checkView.setCheckedNum(checkedNum);
                if (checkedNum > 0) {
                    mBinding.checkView.setEnabled(true);
                } else {
                    mBinding.checkView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            } else {
                boolean checked = mSelectedCollection.isSelected(item);
                mBinding.checkView.setChecked(checked);
                if (checked) {
                    mBinding.checkView.setEnabled(true);
                } else {
                    mBinding.checkView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            }
            updateSize(item);
        }
        mPreviousPos = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void updateApplyButton() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mBinding.buttonApply.setText(R.string.button_apply_default);
            mBinding.buttonApply.setEnabled(false);
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mBinding.buttonApply.setText(R.string.button_apply_default);
            mBinding.buttonApply.setEnabled(true);
        } else {
            mBinding.buttonApply.setEnabled(true);
            mBinding.buttonApply.setText(getString(R.string.button_apply, selectedCount));
        }

        if (mSpec.originalable) {
            mBinding.originalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mBinding.originalLayout.setVisibility(View.GONE);
        }
    }


    private void updateOriginalState() {
        mBinding.original.setChecked(mOriginalEnable);
        if (!mOriginalEnable) {
            mBinding.original.setColor(Color.WHITE);
        }

        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        IncapableDialog.class.getName());

                mBinding.original.setChecked(false);
                mBinding.original.setColor(Color.WHITE);
                mOriginalEnable = false;
            }
        }
    }


    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);
            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void updateSize(Item item) {
        if (item.isGif()) {
            mBinding.size.setVisibility(View.VISIBLE);
            mBinding.size.setText(PhotoMetadataUtils.getSizeInMB(item.size) + "M");
        } else {
            mBinding.size.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            mBinding.originalLayout.setVisibility(View.GONE);
        } else if (mSpec.originalable) {
            mBinding.originalLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        setResult(Activity.RESULT_OK, intent);
    }

    private boolean assertAddSelection(Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(this, cause);
        return cause == null;
    }
}
