package com.timecurrency.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        selectedBitmap = BitmapFactory.decodeStream(inputStream);
                        previewShortcut.setImageBitmap(selectedBitmap);
                        findViewById(R.id.btnAddShortcut).setEnabled(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_icon_config);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        
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
        
        rgIcons.setOnCheckedChangeListener((group, checkedId) -> {
            String targetAlias = ".MainActivity";
            if (checkedId == R.id.rbIcon1) targetAlias = ".MainActivityAlias1";
            else if (checkedId == R.id.rbIcon2) targetAlias = ".MainActivityAlias2";
            
            if (!targetAlias.equals(AppIconHelper.getCurrentAlias(this))) {
                AppIconHelper.setIcon(this, targetAlias);
                Toast.makeText(this, "Icon changed. The app may close.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Custom Shortcut ---
        previewShortcut = findViewById(R.id.previewShortcut);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnAddShortcut = findViewById(R.id.btnAddShortcut);

        btnSelectImage.setOnClickListener(v -> pickImage.launch("image/png"));

        btnAddShortcut.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                if (shortcutManager.isRequestPinShortcutSupported() && selectedBitmap != null) {
                    
                    // Resize if too big (Shortcut icon limits usually around 108dp or 192px)
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
}