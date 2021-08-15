/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse;

import android.app.Activity;
import android.content.Intent;

import com.zhihu.matisse.ui.MatisseActivity;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Fluent API for building media select specification.
 */
@SuppressWarnings("unused")
public final class SelectionCreator extends BaseSelectionCreator{

    /**
     * Constructs a new specification builder on the context.
     *
     * @param matisse            a requester context wrapper.
     * @param mimeTypes          MIME type set to select.
     * @param mediaTypeExclusive
     */
    SelectionCreator(BaseMatisse matisse, @NonNull Set<MimeType> mimeTypes, boolean mediaTypeExclusive) {
        super(matisse, mimeTypes, mediaTypeExclusive);
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    public void forResult(int requestCode) {
        Activity activity = mMatisse.getActivity();
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, MatisseActivity.class);

        Fragment fragment = mMatisse.getFragment();
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

}
