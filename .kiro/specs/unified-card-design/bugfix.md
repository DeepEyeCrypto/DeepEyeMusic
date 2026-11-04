# Bugfix Requirements Document

## Introduction

Across every section and tab in DeepEyeMusicPro, media cards do not share a single, consistent design. Different surfaces render different card styles, different subsets of metadata, and different grid layouts. Not all cards show real data (views, upload date, channel name, channel avatar, duration), and several surfaces do not present cards in a 2-column grid. Additionally, when real data is unavailable, cards display placeholder text such as "0 views", "No views", or "—" instead of hiding the missing field.

The "For You" card is the declared reference standard. It already renders real view counts, upload dates, channel names, durations, channel avatars, and correct fallback thumbnails. Every other card surface must adopt this same design and the same 2-column grid layout. This bugfix covers all feed, rail, and card surfaces: For You, YouTube tab, Music tab, Video Hub, Quick Picks, New Releases, Subscriptions, Liked, History, and any other feed/rail/card surface.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN viewing the For You section THEN the system displays video cards using one card design style, while the YouTube tab displays video cards using a different design style (neon glow border, different badge set, different avatar styling), and the Video Hub displays video cards using yet another design style — so no two video surfaces share the same card design.

1.2 WHEN viewing the Music tab THEN the system displays music cards with a square 1:1 thumbnail and a distinct card design that differs from the For You video card, instead of using the unified 16:9 card design.

1.3 WHEN viewing Quick Picks, New Releases, Supermix, Discover Mix, or Because You Liked sections THEN the system displays cards that show only a thumbnail, title, and artist, omitting views, upload date, channel avatar, and duration.

1.4 WHEN viewing the Liked section THEN the system displays items in a single-column list with small square artwork showing only a title and subtitle, rather than as full cards with complete metadata in a 2-column grid.

1.5 WHEN viewing the History Recently Played section THEN the system displays items in a single-column list showing only a title and artist, rather than as full cards with complete metadata in a 2-column grid.

1.6 WHEN viewing the History Continue Watching section THEN the system displays cards in a horizontal rail with empty channel data (blank channel name), rather than as full cards with complete metadata in a 2-column grid.

1.7 WHEN viewing any music or video section THEN the system does not consistently show cards in a 2-column grid layout: the Music tab uses horizontal rails, the Liked and History Recently Played sections use single-column lists, the YouTube tab uses an adaptive column count, and other sections use a column count that changes by orientation or card width.

1.8 WHEN a card's view count is genuinely unavailable THEN the system displays "0 views" or "No views" placeholder text instead of hiding the row.

1.9 WHEN a card's upload date is genuinely unavailable THEN the system displays "—" placeholder text instead of hiding the field.

1.10 WHEN a card's channel name is blank or a placeholder character THEN the system displays the placeholder as the channel name instead of a clean fallback.

### Expected Behavior (Correct)

2.1 WHEN viewing the For You, YouTube tab, or Video Hub sections THEN the system SHALL display every video card using the unified card design that matches the For You reference standard (16:9 thumbnail, duration badge, quality/format badges, channel avatar with fallback, channel name, title up to 2 lines, and a metadata row of views and upload date).

2.2 WHEN viewing the Music tab THEN the system SHALL display every music card using the unified card design with a 16:9 thumbnail, duration badge, channel avatar with fallback, channel name, title up to 2 lines, and a metadata row of views and upload date.

2.3 WHEN viewing Quick Picks, New Releases, Supermix, Discover Mix, or Because You Liked sections THEN the system SHALL display every card with the full metadata set: views, upload date, channel avatar, channel name, title, and duration.

2.4 WHEN viewing the Liked section THEN the system SHALL display every item as a card with complete metadata (thumbnail, title, channel/subtitle, views, upload date, duration, avatar) in a 2-column grid.

2.5 WHEN viewing the History section THEN the system SHALL display every item — both recently played tracks and continue-watching videos — as full cards with complete metadata in a 2-column grid.

2.6 WHEN viewing any music or video section THEN the system SHALL display cards in a 2-column grid layout (exactly 2 cards side-by-side) for vertical-scrolling sections, and for sections that must remain horizontal rails the system SHALL size cards so exactly 2 cards fit side-by-side.

2.7 WHEN a card's view count is genuinely unavailable THEN the system SHALL hide the view count rather than display "0 views" or "No views".

2.8 WHEN a card's upload date is genuinely unavailable THEN the system SHALL hide the date rather than display "—".

2.9 WHEN a card's channel name is blank or a placeholder character THEN the system SHALL display a clean fallback (e.g., "DeepEye") rather than the raw placeholder, and the channel avatar SHALL fall back to the thumbnail derived from the video ID.

2.10 WHEN a card's thumbnail fails to load THEN the system SHALL fall back to the thumbnail derived from the video ID, matching the For You reference behavior.

### Unchanged Behavior (Regression Prevention)

3.1 WHEN viewing the For You section with real data THEN the system SHALL CONTINUE TO render real view counts, upload dates, channel names, durations, and channel avatars with correct fallback thumbnails.

3.2 WHEN a card thumbnail fails to load THEN the system SHALL CONTINUE TO fall back to the thumbnail derived from the video ID.

3.3 WHEN a channel avatar URL is blank THEN the system SHALL CONTINUE TO fall back to the avatar thumbnail derived from the video ID.

3.4 WHEN viewing any card THEN the system SHALL CONTINUE TO allow tapping the card to open the corresponding content (video or music playback) without changing the existing navigation behavior.

3.5 WHEN quality or format information is available (LIVE, HDR, 4K, etc.) THEN the system SHALL CONTINUE TO display the appropriate badge.

3.6 WHEN viewing a section that contains items with partial data (some items have full metadata, some do not) THEN the system SHALL CONTINUE TO show real data where available and hide only the missing fields per item, without showing placeholder text for any item.

3.7 WHEN a section is loading or empty THEN the system SHALL CONTINUE TO show the existing loading skeletons and empty states.

3.8 WHEN viewing the Continue Watching section THEN the system SHALL CONTINUE TO show watch-progress indicators where progress data exists.

### Bug Condition

```pascal
FUNCTION isBugCondition(section, card)
  INPUT: section of type Section, card of type Card
  OUTPUT: boolean

  // Returns true when the card or section deviates from the unified standard
  RETURN card.design != UNIFIED_FOR_YOU_DESIGN
      OR NOT card.showsFullMetadata
      OR NOT section.usesTwoColumnGrid
      OR card.showsBlankPlaceholderForMissingData
END FUNCTION
```

### Properties

```pascal
// Property: Fix Checking — Unified Design, Full Metadata, 2-Column Grid
FOR ALL (section, card) WHERE isBugCondition(section, card) DO
  result <- renderCard'(section, card)
  ASSERT result.design = UNIFIED_FOR_YOU_DESIGN
  ASSERT result.layout = TWO_COLUMN_GRID
  ASSERT result.showsFullMetadata WHERE data available
  ASSERT NOT result.showsBlankPlaceholderForMissingData
END FOR

// Property: Preservation Checking — Existing Good Behavior
FOR ALL (section, card) WHERE NOT isBugCondition(section, card) DO
  ASSERT renderCard(section, card) = renderCard'(section, card)
END FOR
```
