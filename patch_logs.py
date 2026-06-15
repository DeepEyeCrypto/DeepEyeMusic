with open('app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt', 'r') as f:
    content = f.read()

content = content.replace('initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f', 'android.util.Log.e("VLC_GESTURE", "pointerInput started")\n                                        initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f')

with open('app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt', 'w') as f:
    f.write(content)
