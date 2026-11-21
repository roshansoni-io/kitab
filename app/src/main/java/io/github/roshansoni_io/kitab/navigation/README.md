# App navigation

`KitabRoot` owns the Navigation Compose graph, the three top-level tabs, and the Reader destination. Tab navigation saves and restores each tab's back stack. The bottom bar is hidden while a non-tab destination such as Reader is active.

`KitabViewModel` coordinates app-shell workflows: importing or opening a PDF, reporting reading progress, checking storage access, and exposing shell dialogs/effects. `KitabRoot` handles platform activity results and performs navigation or external actions from those effects. The ViewModel does not duplicate the `NavController` back stack.

## State ownership

- Navigation and saved tab stacks belong to Navigation Compose.
- Recent and Library screen state belongs to their feature ViewModels and is derived from `BookRepository.books`.
- Settings are persisted by `SettingsRepository`; the Settings feature and `MainActivity` observe that flow.
- Import dialogs and pending shell workflows belong to `KitabViewModel`; platform launchers remain in `KitabRoot`.

See [`feature`](../feature/README.md) for the destination responsibilities and state boundaries.
