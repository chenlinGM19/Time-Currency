package com.timecurrency.app;

import android.app.AppOpsManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    
    // UI References
    private RadioGroup rgStyles;
    private RadioGroup rgLayouts;
    private SeekBar seekBarTransparency;
    private SeekBar seekBarRadius;
    
    private Bitmap currentSourceBitmap = null;
    
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

        // Set the result to CANCELED initially
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

        rgStyles = findViewById(R.id.rgStyles);
        rgLayouts = findViewById(R.id.rgLayouts);
        seekBarTransparency = findViewById(R.id.seekBarTransparency);
        seekBarRadius = findViewById(R.id.seekBarRadius);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnClearImage = findViewById(R.id.btnClearImage);
        Button btnSave = findViewById(R.id.btnSaveWidget);
        previewImage = findViewById(R.id.previewImage);
        
        // Load existing settings if any (for re-configuration)
        loadSavedSettings();
        
        // Initial refresh
        refreshPreview();

        btnSelectImage.setOnClickListener(v -> pickImage.launch("image/*"));
        
        btnClearImage.setOnClickListener(v -> {
            selectedImagePath = null;
            currentSourceBitmap = null;
            refreshPreview();
        });
        
        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                refreshPreview();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSave.setOnClickListener(v -> {
            final Context context = WidgetConfigActivity.this;

            // 1. Save Style
            int selectedStyleId = rgStyles.getCheckedRadioButtonId();
            int style = 0;
            if (selectedStyleId == R.id.style1) style = 0;
            else if (selectedStyleId == R.id.style2) style = 1;
            else if (selectedStyleId == R.id.style3) style = 2;
            else if (selectedStyleId == R.id.style4) style = 3;
            else if (selectedStyleId == R.id.style5) style = 4;
            else if (selectedStyleId == R.id.style6) style = 5;
            WidgetSettingsHelper.saveStyle(context, appWidgetId, style);

            // 2. Save Layout Mode
            int selectedLayoutId = rgLayouts.getCheckedRadioButtonId();
            int layoutMode = 0;
            if (selectedLayoutId == R.id.layoutDefault) layoutMode = 0;
            else if (selectedLayoutId == R.id.layoutSidebarRight) layoutMode = 1;
            else if (selectedLayoutId == R.id.layoutSidebarLeft) layoutMode = 2;
            else if (selectedLayoutId == R.id.layoutVertical) layoutMode = 3;
            else if (selectedLayoutId == R.id.layoutBarBottom) layoutMode = 4;
            else if (selectedLayoutId == R.id.layoutBarTop) layoutMode = 5;
            else if (selectedLayoutId == R.id.layoutDiagonal) layoutMode = 6;
            WidgetSettingsHelper.saveLayoutMode(context, appWidgetId, layoutMode);

            // 3. Save Transparency
            WidgetSettingsHelper.saveTransparency(context, appWidgetId, seekBarTransparency.getProgress());
            
            // 4. Save Corner Radius
            WidgetSettingsHelper.saveCornerRadius(context, appWidgetId, seekBarRadius.getProgress());
            
            // 5. Save Image Path
            WidgetSettingsHelper.saveImagePath(context, appWidgetId, selectedImagePath);

            // 6. Update the widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            CurrencyWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId);

            // 7. Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        });
    }
    
    private void loadSavedSettings() {
        // Style
        int style = WidgetSettingsHelper.loadStyle(this, appWidgetId);
        int styleId = R.id.style1; // Default
        switch (style) {
            case 0: styleId = R.id.style1; break;
            case 1: styleId = R.id.style2; break;
            case 2: styleId = R.id.style3; break;
            case 3: styleId = R.id.style4; break;
            case 4: styleId = R.id.style5; break;
            case 5: styleId = R.id.style6; break;
        }
        rgStyles.check(styleId);

        // Layout
        int layout = WidgetSettingsHelper.loadLayoutMode(this, appWidgetId);
        int layoutId = R.id.layoutDefault; // Default
        switch (layout) {
            case 0: layoutId = R.id.layoutDefault; break;
            case 1: layoutId = R.id.layoutSidebarRight; break;
            case 2: layoutId = R.id.layoutSidebarLeft; break;
            case 3: layoutId = R.id.layoutVertical; break;
            case 4: layoutId = R.id.layoutBarBottom; break;
            case 5: layoutId = R.id.layoutBarTop; break;
            case 6: layoutId = R.id.layoutDiagonal; break;
        }
        rgLayouts.check(layoutId);

        // SeekBars
        seekBarTransparency.setProgress(WidgetSettingsHelper.loadTransparency(this, appWidgetId));
        seekBarRadius.setProgress(WidgetSettingsHelper.loadCornerRadius(this, appWidgetId));

        // Image
        selectedImagePath = WidgetSettingsHelper.loadImagePath(this, appWidgetId);
        if (selectedImagePath != null) {
            currentSourceBitmap = BitmapFactory.decodeFile(selectedImagePath);
        }
    }
    
    private void refreshPreview() {
        if (previewImage == null) return;
        
        int w = previewImage.getWidth();
        int h = previewImage.getHeight();
        if (w == 0) w = 800; // fallback estimate
        if (h == 0) h = 400;
        
        float radius = seekBarRadius.getProgress() * getResources().getDisplayMetrics().density;
        
        // Generate the exact same bitmap the widget will use
        Bitmap preview = CurrencyWidgetProvider.createSmartRoundedBitmap(currentSourceBitmap, w, h, radius);
        
        previewImage.setImageBitmap(preview);
        previewImage.setBackground(null);
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
            currentSourceBitmap = scaled;
            refreshPreview();
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}