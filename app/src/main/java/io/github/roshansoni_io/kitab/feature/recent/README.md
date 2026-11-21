# Recent feature

This destination shows books ordered by recent activity and supports filtering, title search, starring, removal, and opening a book. `RecentViewModel` observes `BookRepository.books`, derives the visible list, and loads covers. Filter and search inputs are restored through `SavedStateHandle`; removal confirmation is transient UI state.

`RecentRoute` connects the ViewModel to the screen and handles one-shot effects. Keep rendering in the screen/components and persistence in `BookRepository`.

See [`components`](components/README.md) for the feature's list presentation.
