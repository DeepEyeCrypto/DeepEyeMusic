# Final Handover — Account-Music Sections

## State
- HEAD 6740b663 (main). Nothing committed or pushed.
- Prior patches preserved (autoplay/loop): SmartTubeEngine, AutoplayRepository,
  ContentFetcher, PlayerController, QueueManager untouched this round.

## Files changed this round
| File | Change |
|---|---|
| InnerTubeRemoteClient.kt | MusicShelfFailureReason enum; InnerTubeMusicUnavailableException(reason, message, httpStatus); token-block + typed mapping + cancellation rethrows in fetchMusicHomeShelves |
| PersonalizedModels.kt | PersonalizedSection.failureReason (nullable, additive default) |
| PersonalizationRepositoryImpl.kt | repo caller: cancellation rethrow; auth error section; unavailable error section; EMPTY stays emptyList so tab shows honest empty |
| MusicViewModel.kt | cancellation rethrow; auth->reconnect message; unavailable->unavailable message |
| Tests | MusicViewModelTest (6), InnerTubeMusicHomeContractTest (4) |
| Docs | docs/account_music/{shelf_source_audit,provider_capability_matrix,root_cause,red_green_evidence,final_handover}.md |

## Patch-specific rollback
```sh
git checkout -- \
  app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt \
  app/src/main/java/com/deepeye/musicpro/domain/model/personalization/PersonalizedModels.kt \
  app/src/main/java/com/deepeye/musicpro/data/repository/PersonalizationRepositoryImpl.kt \
  app/src/main/java/com/deepeye/musicpro/ui/music/MusicViewModel.kt
```
New test/doc files removable individually with `rm` (ask first).

## Pending / limitations
- lint + assemble + full-suite rerun: queued below in final validation.
- Live account runtime (shelf provenance, refresh, unavailable, sign-out,
  account switch, item playback): BLOCKED unless user exercises them on an
  authorized device — mocks and BUILD SUCCESSFUL do not prove personalization.
- Home-discovery hardcoded search fallbacks (AuthenticatedYouTubeClient
  movies/gaming/news/music-history) flagged audit-only; separate product
  decision required.
- Music unavailable-cache labelling (labelled stale cache on NETWORK): not
  implemented — currently errors honestly; follow-up if offline-cache UX wanted.
