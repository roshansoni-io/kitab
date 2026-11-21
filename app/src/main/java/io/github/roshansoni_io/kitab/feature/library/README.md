# Library feature

This destination presents books marked as part of the library. `LibraryViewModel` observes the shared book flow, derives the filtered/search-matched collection, loads covers, and handles starring and removal requests.

The ViewModel keeps filter, search, and list/grid preferences in `SavedStateHandle`. The route collects its UI state and effects; the screen forwards actions. Book changes are written through `BookRepository`.

See [`components`](components/README.md) for the collection and card UI.
