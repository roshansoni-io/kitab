# Collection feature

This feature presents both the Recent and Library destinations. `BookCollectionViewModel` observes the shared book flow and derives the collection for its `BookCollectionSource`: Recent shows every book ordered by last activity, while Library shows only books marked as part of the library.

The ViewModel keeps search and list/grid preferences in `SavedStateHandle`. The route collects its UI state and effects; the screen forwards actions. Book changes are written through `BookRepository`.

See [`components`](components/README.md) for the collection layouts and book cards.
