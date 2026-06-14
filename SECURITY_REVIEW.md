# SECURITY_REVIEW

## Assessment
The application primarily deals with public streaming URLs and local media. It does not handle PII (Personally Identifiable Information).

## Vulnerabilities
1. **Cleartext Proxy**: The WebView bridge (`StreamTeeProxy`) binds to `localhost` via HTTP. While localhost is generally secure from external networks, malicious local apps could theoretically sniff the stream. 
2. **Untrusted Parsers**: If a future update introduces Innertube protobuf parsing, it must be hardened against malformed protobuf crashes (OOM attacks).
3. **Storage Access**: The app requests `READ_MEDIA_AUDIO`. We must ensure it gracefully handles missing permissions without crashing.

## Mitigation Plan
- Migrate `StreamTeeProxy` to a local HTTPS server if possible, or tightly restrict port binding.
- Implement strict try/catch blocks around all network and IO operations to prevent silent ANRs.
