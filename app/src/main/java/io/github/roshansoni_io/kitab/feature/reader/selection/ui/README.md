# Text selection UI

`SelectionOverlay` draws the selected text and draggable handles; `SelectionToolbar` presents actions for the current selection. These composables are used by Reader pages and receive selection details and callbacks from their caller.

Keep text-range decisions in the parent selection state and visual presentation here.
