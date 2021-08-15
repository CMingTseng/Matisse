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
package com.zhihu.matisse;

import android.app.Activity;

import java.util.Set;

import androidx.fragment.app.Fragment;

/**
 * Entry for Matisse's media selection.
 */
public final class Matisse extends BaseMatisse {
    protected Matisse(Activity activity) {
        super(activity);
    }

    protected Matisse(Fragment fragment) {
        super(fragment);
    }

    protected Matisse(Activity activity, Fragment fragment) {
        super(activity, fragment);
    }

    public static BaseMatisse from(Activity activity) {
        return new Matisse(activity);
    }

    public static BaseMatisse from(Fragment fragment) {
        return new Matisse(fragment);
    }

    public BaseSelectionCreator choose(Set<MimeType> mimeTypes) {
        return this.choose(mimeTypes, true);
    }

    /**
     * MIME types the selection constrains on.
     * <p>
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param mimeTypes          MIME types set user can choose from.
     * @param mediaTypeExclusive Whether can choose images and videos at the same time during one
     *                           single choosing process. true corresponds to not being able to
     *                           choose images and videos at the same time, and false corresponds to
     *                           being able to do this.
     * @return {@link BaseSelectionCreator} to build select specifications.
     * @see MimeType
     * @see BaseSelectionCreator
     */
    public BaseSelectionCreator choose(Set<MimeType> mimeTypes, boolean mediaTypeExclusive) {
        return new SelectionCreator(this, mimeTypes, mediaTypeExclusive);
    }
}
