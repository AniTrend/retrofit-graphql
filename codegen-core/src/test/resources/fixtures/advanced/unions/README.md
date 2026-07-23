# Activity Union Fixtures

## `activities`
Multi-member union selection with three concrete types:
- ListActivity: status, progress
- TextActivity: text
- MessageActivity: recipient, message

Common field `id` selected on all three.

## `activityFeed`
Wrapper container that returns `ActivityFeed` with `items: [ActivityUnion]`.
Tests nested union access through a wrapper type.
