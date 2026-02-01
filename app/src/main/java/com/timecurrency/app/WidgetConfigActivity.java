package com.timecurrency.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class WidgetConfigActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ImageView previewImage;
    private String selectedImagePath = null;
    
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveImageLocally(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Edge to Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_widget_config);

        // Set the result to CANCELED.
        setResult(RESULT_CANCELED);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        RadioGroup rgStyles = findViewById(R.id.rgStyles);
        SeekBar seekBarTransparency = findViewById(R.id.seekBarTransparency);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnClearImage = findViewById(R.id.btnClearImage);
        Button btnSave = findViewById(R.id.btnSaveWidget);
        previewImage = findViewById(R.id.previewImage);

        btnSelectImage.setOnClickListener(v -> pickImage.launch("image/*"));
        
        btnClearImage.setOnClickListener(v -> {
            selectedImagePath = null;
            previewImage.setImageDrawable(null);
        });

        btnSave.setOnClickListener(v -> {
            final Context context = WidgetConfigActivity.this;

            // 1. Save Style
            int selectedId = rgStyles.getCheckedRadioButtonId();
            int style = 0;
            if (selectedId == R.id.style1) style = 0;
            else if (selectedId == R.id.style2) style = 1;
            else if (selectedId == R.id.style3) style = 2;
            else if (selectedId == R.id.style4) style = 3;
            else if (selectedId == R.id.style5) style = 4;
            else if (selectedId == R.id.style6) style = 5;
            WidgetSettingsHelper.saveStyle(context, appWidgetId, style);

            // 2. Save Transparency
            WidgetSettingsHelper.saveTransparency(context, appWidgetId, seekBarTransparency.getProgress());
            
            // 3. Save Image Path
            WidgetSettingsHelper.saveImagePath(context, appWidgetId, selectedImagePath);

            // 4. Update the widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            CurrencyWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId);

            // 5. Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        });
    }

    private void saveImageLocally(Uri sourceUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            if (bitmap == null) return;
            
            // Adaptive scaling logic: Fit within 1024x1024 without distorting aspect ratio
            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            int maxDimension = 1024;
            
            int newWidth = originalWidth;
            int newHeight = originalHeight;
            
            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                float aspectRatio = (float) originalWidth / originalHeight;
                if (originalWidth > originalHeight) {
                    newWidth = maxDimension;
                    newHeight = Math.round(maxDimension / aspectRatio);
                } else {
                    newHeight = maxDimension;
                    newWidth = Math.round(maxDimension * aspectRatio);
                }
            }
            
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            
            File file = new File(getFilesDir(), "widget_bg_" + appWidgetId + ".png");
            FileOutputStream out = new FileOutputStream(file);
            scaled.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            
            selectedImagePath = file.getAbsolutePath();
            previewImage.setImageBitmap(scaled);
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}