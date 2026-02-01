package com.timecurrency.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;

public class IconConfigActivity extends AppCompatActivity {

    private ImageView previewShortcut;
    private Bitmap selectedBitmap;
    
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    loadBitmapEfficiently(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        setContentView(R.layout.activity_icon_config);
        
        // Manual inset handling removed in favor of fitsSystemWindows="true" in XML
        
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // --- System Icon Switching ---
        RadioGroup rgIcons = findViewById(R.id.rgIcons);
        String currentAlias = AppIconHelper.getCurrentAlias(this);
        
        if (".MainActivityAlias1".equals(currentAlias)) {
            rgIcons.check(R.id.rbIcon1);
        } else if (".MainActivityAlias2".equals(currentAlias)) {
            rgIcons.check(R.id.rbIcon2);
        } else {
            rgIcons.check(R.id.rbDefault);
        }
        
        Button btnApplyIcon = findViewById(R.id.btnApplyIcon);
        btnApplyIcon.setOnClickListener(v -> {
            int checkedId = rgIcons.getCheckedRadioButtonId();
            String targetAlias = ".MainActivity";
            if (checkedId == R.id.rbIcon1) targetAlias = ".MainActivityAlias1";
            else if (checkedId == R.id.rbIcon2) targetAlias = ".MainActivityAlias2";
            
            if (!targetAlias.equals(AppIconHelper.getCurrentAlias(this))) {
                showIconChangeConfirmation(targetAlias);
            } else {
                Toast.makeText(this, "This icon is already active", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Custom Shortcut ---
        previewShortcut = findViewById(R.id.previewShortcut);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnAddShortcut = findViewById(R.id.btnAddShortcut);

        btnSelectImage.setOnClickListener(v -> pickImage.launch("image/*")); 

        btnAddShortcut.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                if (shortcutManager.isRequestPinShortcutSupported() && selectedBitmap != null) {
                    
                    // Resize to standard icon size (approx 192px)
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(selectedBitmap, 192, 192, true);
                    
                    ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, "custom-icon-" + System.currentTimeMillis())
                        .setIcon(Icon.createWithBitmap(scaledBitmap))
                        .setShortLabel("Time Currency")
                        .setIntent(new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN))
                        .build();

                    shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                    Toast.makeText(this, "Shortcut requested", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Pinned shortcuts not supported or no image", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Android 8.0+ required for pinned shortcuts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showIconChangeConfirmation(String targetAlias) {
        new AlertDialog.Builder(this)
            .setTitle("Change App Icon")
            .setMessage("Changing the app icon requires the app to restart. The app will close immediately.")
            .setPositiveButton("Change & Restart", (dialog, which) -> {
                AppIconHelper.setIcon(this, targetAlias);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void loadBitmapEfficiently(Uri uri) {
        try {
            // 1. Decode bounds only to check dimensions
            InputStream inputForBounds = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputForBounds, null, options);
            if (inputForBounds != null) inputForBounds.close();

            // 2. Calculate scale factor to avoid OOM
            // Target roughly 512x512 for a high quality source
            options.inSampleSize = calculateInSampleSize(options, 512, 512);
            options.inJustDecodeBounds = false;

            // 3. Decode actual bitmap
            InputStream inputForDecode = getContentResolver().openInputStream(uri);
            Bitmap rawBitmap = BitmapFactory.decodeStream(inputForDecode, null, options);
            if (inputForDecode != null) inputForDecode.close();

            if (rawBitmap != null) {
                // 4. Crop to Square
                selectedBitmap = cropToSquare(rawBitmap);
                
                previewShortcut.setImageBitmap(selectedBitmap);
                previewShortcut.setColorFilter(null); // Remove the tint
                findViewById(R.id.btnAddShortcut).setEnabled(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    
    private Bitmap cropToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        
        return Bitmap.createBitmap(source, cropW, cropH, newWidth, newHeight);
    }
}