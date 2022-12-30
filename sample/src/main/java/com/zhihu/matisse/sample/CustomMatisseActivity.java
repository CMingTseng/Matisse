package com.zhihu.matisse.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.ImageEngine;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.listener.SelectionDelegate;


import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Custom Matisse
 */
public class CustomMatisseActivity extends AppCompatActivity implements View.OnClickListener, SelectionDelegate {

    private static final int REQUEST_CODE_CHOOSE = 23;
    private static final String TAG = CustomMatisseActivity.class.getSimpleName();

    private List<Uri> mSelectedUris;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_matisse);

        findViewById(R.id.btn_go).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            // Use Uri result
            TextView resultTextView = (TextView) findViewById(R.id.tv_result);
            resultTextView.setText("");
            mSelectedUris = Matisse.Companion.obtainResult(data);
            for (Uri uri : mSelectedUris) {
                int or = getExifOrientation(CustomMatisseActivity.this, uri);
                resultTextView.append(uri.toString());
                resultTextView.append("\n");
            }

            // Use absolute path result
            TextView pathTextView = (TextView) findViewById(R.id.tv_path_result);
            pathTextView.setText("");
            List<String> pathResult = Matisse.Companion.obtainPathResult(data);
            for (String path : pathResult) {
                pathTextView.append(path);
                pathTextView.append("\n");
            }
        }
    }

    @Override
    public void onClick(View v) {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe(aBoolean -> {
                if (aBoolean) {
                    startAction();
                } else {
                    Toast.makeText(CustomMatisseActivity.this, R.string.permission_request_denied, Toast.LENGTH_LONG)
                        .show();
                }
            }, Throwable::printStackTrace);
    }

    private void startAction() {
        CheckBox imageCheckBox = (CheckBox) findViewById(R.id.cb_choice_image);
        CheckBox videoCheckBox = (CheckBox) findViewById(R.id.cb_choice_video);
        RadioButton zhihuRadioButton = (RadioButton) findViewById(R.id.rb_theme_zhihu);
        RadioButton draculaRadioButton = (RadioButton) findViewById(R.id.rb_theme_dracula);
        RadioButton customThemeButton = (RadioButton) findViewById(R.id.rb_theme_custom);
        RadioButton glideRadioButton = (RadioButton) findViewById(R.id.rb_glide);
        RadioButton picassoRadioButton = (RadioButton) findViewById(R.id.rb_picasso);
        RadioButton uilRadioButton = (RadioButton) findViewById(R.id.rb_uil);
        EditText selectCountEditor = (EditText) findViewById(R.id.et_selectable_count);
        EditText selectVideoCountEditor = (EditText) findViewById(R.id.et_video_selectable_count);

        CheckBox countableCheckBox = (CheckBox) findViewById(R.id.cb_countable);
        CheckBox captureCheckBox = (CheckBox) findViewById(R.id.cb_capture);

        Set<MimeType> mimeTypes;
        if (imageCheckBox.isChecked() && videoCheckBox.isChecked()) {
            mimeTypes = MimeType.Companion.ofAll();
        } else if (imageCheckBox.isChecked()) {
            mimeTypes = MimeType.Companion.ofImage();
        } else {
            mimeTypes = MimeType.Companion.ofVideo();
        }

        ImageEngine imageEngine = null;
        if (glideRadioButton.isChecked()) {
            imageEngine = new GlideEngine();
        } else if (picassoRadioButton.isChecked()) {
            imageEngine = new PicassoEngine();
        } else if (uilRadioButton.isChecked()) {
            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
            imageEngine = new UILEngine();
        }

        String maxCount = selectCountEditor.getText().toString();
        String maxVideoCount = selectVideoCountEditor.getText().toString();
        int maxSelectable = Integer.parseInt(maxCount);
        int maxVideoSeletable = Integer.parseInt(maxVideoCount);

        int theme = R.style.Matisse_Dracula;
        if (zhihuRadioButton.isChecked()) {
            theme = R.style.Matisse_Zhihu;
        } else if (draculaRadioButton.isChecked()) {
            theme = R.style.Matisse_Dracula;
        } else if (customThemeButton.isChecked()) {
            theme = R.style.CustomTheme;
        } else {
            // custom theme
        }

        boolean countable = countableCheckBox.isChecked();
        boolean capture = captureCheckBox.isChecked();

        Matisse.Companion.from(this)
            .choose(mimeTypes, false)
            .showSingleMediaType(true)
            .capture(capture)
            .captureStrategy(
                new CaptureStrategy(true, "com.zhihu.matisse.sample.fileprovider",""))
            .countable(countable)
            .maxSelectable(maxSelectable)
            .enablePreview(true)

            //                .maxSelectablePerMediaType(maxSelectable, maxVideoSeletable)
            .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
            .gridExpectedSize(
                getResources().getDimensionPixelSize(R.dimen.grid_expected_size))

            .originalEnable(true)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(imageEngine)
            .theme(theme)
            .delegate(this)
            .allowsMultipleSelection(false)
            //.maxVideoLength(120)
            .hasFeatureEnabled(false)
            .dontShowVideoAlert(true)
            . forResult(REQUEST_CODE_CHOOSE, mSelectedUris);
    }

    public static final String LOCAL_CONTENT_SCHEME = "content";
    public static String getSchemeOrNull(@Nullable Uri uri) {
        return uri == null ? null : uri.getScheme();
    }
    public static boolean isLocalContentUri(@Nullable Uri uri) {
        final String scheme = getSchemeOrNull(uri);
        return LOCAL_CONTENT_SCHEME.equals(scheme);
    }

    public static int exifOrientationFromDegrees(int degrees) {
        switch (degrees) {
            case 90:
                return ExifInterface.ORIENTATION_ROTATE_90;
            case 180:
                return ExifInterface.ORIENTATION_ROTATE_180;
            case 270:
                return ExifInterface.ORIENTATION_ROTATE_270;
            default:
                return 0;
        }
    }

    public static int getExifOrientation(Context context, Uri uri) {
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;
        if (uri.getScheme() != null && isLocalContentUri(uri)) {
            String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (null != cursor) {
                try {
                    if (cursor.moveToFirst()) {
                        int degrees = cursor.getInt(0);
                        return exifOrientationFromDegrees(degrees);
                    }
                } catch (IllegalStateException | CursorIndexOutOfBoundsException e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }
        } else {
            try {
                ExifInterface originalExif = new ExifInterface(uri.getPath());
                orientation =
                    originalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return orientation;
    }

    @Override
    public String getCause(SelectedItemCollection.MaxItemReach reach) {

        switch (reach) {
            case MIX_REACH:
                return "Mix cause";
            case IMAGE_REACH:
                return "Image cause";
            case VIDEO_REACH:
                return "Video cause";
            default:
                return "My cause";
        }

    }

    @Override
    public void onTapItem(Item item, Boolean isDontShow) {
        if (item != null) {
            Log.d("ACTIVITY_MATISSE", String.format("DURATION: %d seconds", (item.getDuration()/1000)));
        }
    }
}
