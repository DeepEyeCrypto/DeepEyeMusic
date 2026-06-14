# YOUTUBE_ARCHITECTURE

## The SourceResolver Pattern
To guarantee ExoPlayer playback, we must extract the raw `.m4a` or `.webm` stream URL from YouTube. Relying solely on the WebView Tee Proxy is innovative but resource-heavy.

## Resolvers (In Priority Order)
1. **NewPipeResolver**: Uses NewPipeExtractor backend. Fastest, cleanest, yields direct `googlevideo.com/videoplayback` URLs.
2. **PipedResolver / InnertubeResolver**: Uses public/private Piped APIs or Innertube protobuf clients to fetch streaming URLs.
3. **WebViewBridgeResolver** (The Current System): Fallback mechanism. Loads the video silently in a WebView, intercepts the `OkHttp` stream, and pipes it via `StreamTeeProxy` to ExoPlayer.

## Execution Flow
`PlayerController.play(youtubeId)` 
→ `SourceResolverManager.resolve(youtubeId)`
→ Try #1. If fail → Try #2. If fail → Try #3.
→ Yields `MediaSource` (Direct URI or Proxy URI).
→ `ExoPlayer.setMediaSource()`
→ Playback begins.

## Enforcement
- **NO DIRECT WEBVIEW PLAYBACK**. The WebView is muted and visually hidden. It exists *only* as a headless stream scraper.
