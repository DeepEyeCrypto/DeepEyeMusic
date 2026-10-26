# License Review

**SmartTube repository:** https://github.com/yuliskov/SmartTube  
**Inspected commit:** `ffa16426ea3c7c82077e0f00228818b85f373588`  
**License file:** repository-root `LICENSE`  
**Finding:** MIT License (copyright 2020-present yuliskov).

## Submodules

| Module | Pinned SHA | License file in checkout | Finding |
|---|---|---|---|
| MediaServiceCore | `7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5` | No `LICENSE*` at module root | `UNKNOWN` — do not assume parent MIT applies |
| SharedModules | `d4f09b11133036d90826baf012e120d06486ae73` | No `LICENSE*` at module root | `UNKNOWN` — do not assume parent MIT applies |

## License status follow-up (pinned source review)

- The submodule root directories do not have tracked root LICENSE/COPYING/NOTICE files at the pinned revisions.
- `SharedModules` does contain nested third-party license files: `appupdatechecker2/LICENSE` (MIT), `commons-io-2.8.0/LICENSE.txt` + `NOTICE.txt` (Apache-2.0 with ASF notice), and `j2v8/LICENSE.txt` (EPL-1.0). These apply to the named vendored components, not automatically to all of SharedModules.
- `MediaServiceCore/README.md` only describes it as an unofficial YouTube Java API; it does not specify a license.
- Therefore license for original `MediaServiceCore` code and original `SharedModules` code remains `UNKNOWN`; do not treat them as MIT based on parent repo.
- No code was copied or reused, so no third-party notice was added to DeepEye. Reuse remains blocked until license provenance for the exact copied file/module is resolved.

## License implications


SmartTube root MIT permits use, modification, distribution, sublicensing, and sale subject to preserving the copyright and license notice in copies or substantial portions. No SmartTube code was copied into DeepEye in Phase A. Prefer independent implementation through existing DeepEye abstractions. If future work copies any code, first resolve each submodule’s license and include required notices.

## Boundaries

- No SmartTube OAuth client ID/secret was copied.
- No credential, access token, refresh token, cookie, account identifier, or authenticated payload is included in deliverables.
- `google-services.json` exists in the SmartTube checkout but was not opened or copied; secret/config material is out of scope.
- Root license: `VERIFIED_IN_SOURCE`. Submodule licenses: `UNKNOWN`.
