#!/bin/bash
sed -i '' '/windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())/a \
            // Fix for SurfaceView edge-to-edge black bar bug on Android 14+\
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES\
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {\
                window.setDecorFitsSystemWindows(false)\
            }
' /Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/MainActivity.kt
