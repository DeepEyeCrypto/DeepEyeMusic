# SmartTube Source Pinning

**Clone Path:** `/tmp/SmartTube-ref`
**Repository URL:** https://github.com/yuliskov/SmartTube
**Inspected Commit SHA:** `ffa16426ea3c7c82077e0f00228818b85f373588`
**Branch:** master (default)
**Clone Date:** 2026-10-18
**Clone Type:** Full history (not --depth=1) to capture submodule references

## Remote Configuration
```
origin	https://github.com/yuliskov/SmartTube (fetch)
origin	https://github.com/yuliskov/SmartTube (push)
```

## .gitmodules Content
```
[submodule "SharedModules"]
	path = SharedModules
	url = https://github.com/yuliskov/SharedModules
[submodule "MediaServiceCore"]
	path = MediaServiceCore
	url = https://github.com/yuliskov/MediaServiceCore
```

## Submodule Status (after init)
```
7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5 MediaServiceCore (pinned gitlink)
d4f09b11133036d90826baf012e120d06486ae73 SharedModules (pinned gitlink)
```

- `MediaServiceCore` contains a nested `SharedModules` submodule; it resolved to `d4f09b11133036d90826baf012e120d06486ae73` as well.
- Root-level LICENSE files were not present in either submodule checkout; see `license_review.md` (`UNKNOWN`, not inherited by assumption).

```
-rw-r--r--  1 ejaj  wheel  1072 Oct 18 19:53 LICENSE
MIT License

Copyright (c) 2020-present yuliskov

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
