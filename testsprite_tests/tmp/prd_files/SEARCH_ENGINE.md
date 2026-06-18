# DeepEye Music Pro — Search Engine & Autocomplete (STAGE 11)

This document specifies the search parsing, remote suggestion APIs, local result ranking algorithms, and search histories for DeepEye Music Pro.

---

## 1. Search Suggestion Pipeline

When the user types in the glassmorphic search bar, autocomplete suggestions are fetched using a dual local-remote resolver:

```
User Input Keypress
  │
  ├─► Local Cache ──► Queries room DB 'search_history' for matches
  │
  └─► Remote API ───► Fetches Google/YouTube Autocomplete suggestion endpoint
        │
        ▼
   Merge & Deduplicate (Removes duplicates, ranks local matches first)
        │
        ▼
   Render Suggestions Grid
```

* **Remote Suggestion Endpoint**: `https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=[QUERY]`
* **Network Debounce**: Keystrokes are debounced by `300ms` before triggering network requests, preserving battery and bandwidth.

---

## 2. Instant Results Ranking

Search results returned from YouTube are sorted dynamically using a lightweight scoring model inside the ViewModel:
* **Match Score**: Matches containing query terms in the title or artist name are promoted.
* **Format Ranking**: Audio-only content or tracks matching the user's preferred listening language are scored higher.
* **Taste Profile Priority**: Promotes content matching genres or artists identified in the database feedback logs.

---

## 3. Persistent Search History

Every executed search query is saved immediately:
* Saved to `search_history` Room table.
* Displays as history pills in the search main state.
* The search engine exposes a single-click "Clear Search History" option for privacy control.
