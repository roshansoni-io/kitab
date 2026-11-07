# Kitab — Task Board

> Status legend: `[ ]` pending · `[~]` in progress · `[x]` done · `[-]` cancelled / out of scope

## Milestone 0 — MuPDF Repository Access (unblocker)

> **Finding (2026-11-07):** `maven.artifex.com` is dead (NXDOMAIN). The live MuPDF artifact repo is
> `https://maven.ghostscript.com` — verified serving `com.artifex.mupdf:fitz` 1.11–1.28.5 (latest 1.28.5).
> Also: Gradle's JVM could not resolve the host on this machine, so the `fitz` AAR is vendored
> locally under `third_party/maven/` (Maven layout) as the primary source; the remote repo stays
> configured as fallback for other machines.

- [x] Test artifact-repo reachability from this machine
- [x] Vendor `com.artifex.mupdf:fitz:1.28.5` AAR into `third_party/maven/com/artifex/mupdf/fitz/1.28.5/`
- [x] Configure both local + remote repos with `includeGroup("com.artifex.mupdf")` in `settings.gradle.kts`

## Milestone 1 — MuPDF Integration + Material 3 PDF Reader

### 1. Gradle & Dependencies
- [x] Add `mupdfFitz` to `gradle/libs.versions.toml` (Coil dropped — pages are rendered to `Bitmap` directly, no loader needed)
- [x] Wire `artifex-mupdf-fitz` + `material-icons-core` into `app/build.gradle.kts`
- [x] Add AGPL license attribution in in-app About dialog (`HomeScreen.kt`)

### 2. File Opening (data layer)
- [x] SAF picker via `ActivityResultContracts.OpenDocument` (mime: `application/pdf`)
- [x] `takePersistableUriPermission` granting handled defensively in `BookRepository`
- [x] `book/Book.kt` + `book/BookRepository.kt`: display name via `OpenableColumns`, copy stream to
  `filesDir/books/`, recents stored in `SharedPreferences` as JSON (max 30)

### 3. MuPDF Core Wrapper
- [x] `reader/MuPdfDocument.kt`: open, `countPages()`, page size, password detection, `Matrix`+`RectI` render setup
- [x] Render page → `Bitmap` via `DisplayList` + `AndroidDrawDevice` on a dedicated
  `Dispatchers.IO.limitedParallelism(1)` worker in `reader/ReaderBook.kt` (MuPDF is not thread-safe)
- [x] `android.util.LruCache` (64 MB budget) + strict native disposal (`Page`/`DisplayList`/`Cookie`/`Device` destroyed; doc closed on dispose)

### 4. Material 3 Compose Reader
- [x] `ReaderScreen` inside `KitabTheme` (follows light/dark/dynamic)
- [x] `HorizontalPager` page flip + pinch-to-zoom and double-tap zoom (`reader/ui/ZoomState.kt`,
  gesture interleaving with pager: pager free at scale 1, panning+zooming consumed when zoomed)
- [x] `TopAppBar` (book title + back) + bottom bar with page counter / slider
- [x] Loading / error / page-failure states as M3 components
- [x] Home screen: Extended FAB "Open a book", recents list, remove entry, empty state, About dialog
- [x] `MainActivity` route state (`KitabScreen.Home` / `KitabScreen.Reading`), `BackHandler`, external-URI handling via `onNewIntent`

### 5. Open-from-other-apps
- [x] `ACTION_VIEW` intent filter for `application/pdf` in manifest, `launchMode="singleTask"` → router opens reader

### 6. Build & Verify
- [x] `:app:compileDebugKotlin` green (all fitz/gesture/API issues resolved)
- [x] `./gradlew assembleDebug` succeeds
- [x] Lint clean (`lintDebug`: 0 errors; advisory version-bump warnings for agp/activity/BOM intentionally disabled in `lint {}` — those upgrades require compileSdk 37 / minSdk 23 which this SDK setup cannot provide)
- [x] Merged into Milestone 2 commit (bug fixes shipped alongside)

## Milestone 2 — Reader Bug Fixes, Build Speed & Tooling

### 1. Bug fixes (all user-verified scenarios)
- [x] Blank first page — `MuPdfDocument.renderPage` guards `targetWidthPx > 0`; `ReaderPage`
  skips render until `pageWidthPx > 0`; page bitmap cache keyed by `PageCacheKey(pageIndex, targetWidthPx)`
  so base + hi-res renders coexist (budget 96 MB)
- [x] Blocky text while zoomed — sharp re-render after gesture end at
  `(pageWidthPx * scale).coerceAtMost(ZOOM_RENDER_MAX_WIDTH_PX)`; thresholds
  `ZOOM_SHARPEN_THRESHOLD=1.15`, `DOUBLE_TAP_ZOOM_FACTOR=2.5`
- [x] Zoom resets when finger lifts — `ZoomState` now takes `viewportSize`/`contentSize` as lambdas
  (state created before the bitmap exists); `zoomable()` gained `onGestureEnd` invoked after `settle()`;
  hi-res swap keeps `transformable` so the gesture doesn't get cancelled
- [x] Pager drag blocked while a page is zoomed — `ReaderContent` tracks zoomed pages in
  `mutableStateMapOf` and sets `userScrollEnabled = zoomedPages.isEmpty()`

### 2. Build speed
- [x] `gradle.properties`: `org.gradle.caching=true`, larger heap, in-process Kotlin compiler strategy
- [x] Debug builds restricted to `arm64-v8a` (this machine's ABI; release keeps all four ABIs)

### 3. Tooling
- [x] Lint: `:app:lintDebug` clean (0 errors); advisory version-bump warnings disabled with justification
- [x] ktlint: custom `ktlintCheck`/`ktlintFormat` Gradle tasks running `com.pinterest.ktlint:ktlint-cli`
  (the `org.jlleitschuh.gradle.ktlint` plugin is incompatible with AGP 8.13's isolated gradle-api jar —
  `CommonExtension` NoSuchClass during configuration on every plugin version tested);
  `.editorconfig` disables the two Compose-unfriendly rules (`package-name`, `function-naming`);
  `ktlintFormat` applied, `ktlintCheck` wired into `check`

### 4. Build & Verify
- [x] `:app:compileDebugKotlin` green
- [x] `:app:assembleDebug` green
- [x] `ktlintCheck` green
- [ ] Manual device test (requires user device)
- [x] Commit

---

## Milestone 3 — Zoom Geometry, Links & Selection (in progress)

### 1. Zoom geometry + depth (user-verified scenarios)
- [x] Zoom jumps after hi-res swap — `ZoomState` now measures zoom against the
  *displayed letterboxed* size, not bitmap pixels: new `PageGeometry.fittedSize()` (matches
  `Image` + `ContentScale.Fit` exactly), so scale/offset clamps stay identical when the
  hi-res bitmap replaces the base one
- [x] Tiny accidental zooms snap back too hard — `SNAP_BACK_THRESHOLD` 1.15 → 1.05
- [x] Deeper zoom allowed — `MAX_SCALE` 4 → 5, hi-res re-render cap 2400 px → 3000 px

### 2. Sharp visible-region rendering (screen pixel density)
- [x] Only the visible part of a zoomed page is re-rendered, at ~1 bitmap pixel per screen pixel:
  `PageGeometry.visiblePageRegion()` maps the viewport through the zoom/pan transform into PDF
  points, `MuPdfDocument.renderRegion()` renders just that crop with a translated matrix
- [x] Result is drawn as an overlay inside the same zoom transform, so un-rendered parts stay
  visible (soft) and the sharp part stays perfectly aligned; no full-page hi-res bitmap anymore
- [x] Re-rendered on every gesture end (pinch, pan while zoomed, double-tap zoom), including after
  panning to a different area; skipped when the region/zoom is effectively unchanged
- [x] Overlay cleared when zooming back out below the snap-back threshold
- [x] Only the current page is rendered (`beyondViewportPageCount = 0`); neighbors load on swipe

### 3. Links
- [x] Data layer: cached `LinkTarget(rect, uri?, targetPage?)` per page (`getLinks` + `resolveLink`),
  extracted on the single MuPDF worker thread (`ReaderBook.linkTargets`)
- [x] Tap hit-test with touch-slop expansion (`PageMapper`), zoom-aware via `ZoomState.fromScreen`
- [x] Internal links jump the pager; external links show a "Open <host>?" confirm chip; opening uses
  `ACTION_VIEW` with a toast fallback when no app handles the URI

### 4. Resolution & zoom-jump fixes (user-reported)
- [x] Sharp region now supersampled 2× beyond screen pixels, capped at 12 MP / 4096 px per side
      (`PageGeometry.SUPERSAMPLE`, `MAX_REGION_PIXELS`, `MAX_REGION_DIMENSION`)
- [x] Zoom no longer jumps when a finger lifts mid-pinch — `ZoomState.zoomable` resets its
      centroid/span baseline on every pointer-count change instead of applying one stale delta
- [x] Long-press gesture: tap suppressed after a long press, selection only entered when a
      character was actually found (`onLongPress` returns Boolean)

### 5. Text selection & copy
- [x] Data layer: `MuPdfDocument.pageText()` extracts per-character quads + line indices via
      `toStructuredText("preserve-whitespace")`; `ReaderBook.pageText()` cached (LRU, 32 pages)
- [x] `reader/PageText.kt`: `PageChar`/`PageText`, `textOf(range)` with line-break insertion
- [x] `reader/ui/TextSelection.kt`: `charNear()` (exact hit or nearest within slop),
      `wordRangeAt()` word snap, `selectionFor()` word-granular extend from the anchor word
- [x] `reader/ui/SelectionOverlay.kt`: highlight quads drawn inside the zoom transform
      (`SelectionOverlay`) + Copy/Close floating toolbar (`SelectionToolbar`, clipboard via
      `LocalClipboardManager`)
- [x] `ZoomState.zoomable` long-press detection (timeout without movement), drag-to-extend,
      pager locked while selecting (`selectingPages`), tap clears an active selection

### 6. Build & Verify
- [x] `:app:compileDebugKotlin` + `:app:ktlintCheck` + `:app:assembleDebug` green
- [ ] Manual device test
- [x] Commit

---

## Backlog — Future Milestones (not started)

- [ ] EPUB / reflowable formats via Stories API
- [ ] CBZ, XPS, image-comic formats (same page-render path)
- [ ] Table of contents / outline
- [ ] Bookmarks
- [ ] Remember last page + restore position
- [ ] Text search within document
- [ ] Night / Sepia in-reader reading modes
- [ ] Password-protected PDFs (prompt dialog)
- [ ] Reading settings (page gap, fit modes)
- [ ] Chrome auto-hide on tap while reading
