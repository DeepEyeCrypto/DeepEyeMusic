# QUEUE HISTORY SPEC

## Snapshotting
- Save exact state of `QueueManager` (items, index) to JSON in Room/DataStore on every change.
- Allow one-tap restore.