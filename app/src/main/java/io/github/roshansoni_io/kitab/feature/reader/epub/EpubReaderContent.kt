package io.github.roshansoni_io.kitab.feature.reader.epub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.ui.theme.isDarkTheme
import io.github.roshansoni_io.kitab.feature.reader.ReaderBottomBar
import io.github.roshansoni_io.kitab.feature.reader.ReaderScaffold
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubPreferences
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun EpubReaderContent(
    session: EpubSession,
    bookTitle: String,
    preferences: EpubPreferences,
    settings: AppSettings,
    onProgress: (locatorJson: String, progression: Double) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()
    val locator by session.locator.collectAsStateWithLifecycle()
    val smoothProgression by session.progression.collectAsStateWithLifecycle()
    val totalPositions = session.totalPositions
    var chromeVisible by remember { mutableStateOf(settings.readerControlsInitiallyVisible) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var seekGeneration by remember { mutableIntStateOf(0) }
    var seekJob by remember { mutableStateOf<Job?>(null) }
    val darkTheme = isDarkTheme(settings.readerTheme)
    val pageBackground = settings.readerPageTone.epubBackgroundColor(darkTheme)
    val progression = smoothProgression.toFloat().coerceIn(0f, 1f)

    SideEffect {
        session.onCenterTap = { chromeVisible = !chromeVisible }
        session.onExternalLink = onOpenExternalLink
        session.onProgress = onProgress
    }

    LaunchedEffect(preferences) {
        if (session.preferences != preferences) {
            session.preferences = preferences
            session.navigator?.submitPreferences(preferences)
        }
    }

    // The navigator fragment is hosted by the Activity's FragmentManager, so it has to be
    // removed explicitly when the reader leaves the screen. A configuration change keeps it
    // for restoration.
    DisposableEffect(session, activity) {
        onDispose {
            val activity = activity ?: return@onDispose
            if (activity.isChangingConfigurations || activity.isFinishing) return@onDispose
            val fragment = activity.supportFragmentManager.findFragmentByTag(EPUB_FRAGMENT_TAG) ?: return@onDispose
            activity.supportFragmentManager
                .beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        }
    }

    val displayedProgress = (if (isSeeking) seekValue else progression).coerceIn(0f, 1f)
    val pageLabel =
        epubPageLabel(
            progress = displayedProgress,
            isSeeking = isSeeking,
            currentPosition = locator?.locations?.position,
            totalPositions = totalPositions,
        )

    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        chromeVisible = chromeVisible,
        bottomBar = {
            ReaderBottomBar(
                progress = displayedProgress,
                label = pageLabel,
                onSeek = { value ->
                    seekGeneration++
                    seekJob?.cancel()
                    isSeeking = true
                    seekValue = value
                },
                onSeekFinished = {
                    val targetValue = seekValue.toDouble()
                    val generation = ++seekGeneration
                    seekJob?.cancel()
                    seekJob =
                        scope.launch {
                            if (session.seekTo(targetValue)) {
                                // Keep seek value displayed briefly while WebView settles on the new page.
                                delay(SEEK_SETTLE_DELAY_MS)
                            }
                            if (seekGeneration == generation) {
                                isSeeking = false
                            }
                        }
                },
                onPrevious = {
                    seekGeneration++
                    seekJob?.cancel()
                    isSeeking = false
                    session.navigator?.goBackward()
                },
                onNext = {
                    seekGeneration++
                    seekJob?.cancel()
                    isSeeking = false
                    session.navigator?.goForward()
                },
                isInteracting = isSeeking,
            )
        },
    ) {
        EpubNavigatorHost(
            session = session,
            activity = activity,
            backgroundColor = pageBackground,
        )
    }
}

private fun epubPageLabel(
    progress: Float,
    isSeeking: Boolean,
    currentPosition: Int?,
    totalPositions: Int,
): String {
    if (totalPositions > 0) {
        val displayedPosition =
            if (!isSeeking && currentPosition != null) {
                currentPosition
            } else {
                (progress * (totalPositions - 1)).roundToInt() + 1
            }
        return "${displayedPosition.coerceIn(1, totalPositions)} / $totalPositions"
    }
    return String.format(Locale.US, "%d%%", (progress * 100).roundToInt())
}

@Composable
private fun EpubNavigatorHost(
    session: EpubSession,
    activity: FragmentActivity?,
    backgroundColor: Color,
) {
    var requested by remember(session, activity) { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply { id = R.id.epub_reader_host }
            },
            update = {
                val activity = activity ?: return@AndroidView
                val fragmentManager = activity.supportFragmentManager
                if (fragmentManager.isStateSaved) return@AndroidView
                if (!requested && fragmentManager.findFragmentById(R.id.epub_reader_host) == null) {
                    requested = true
                    fragmentManager.commit {
                        add(
                            R.id.epub_reader_host,
                            EpubReaderFragment.newInstance(session.path),
                            EPUB_FRAGMENT_TAG,
                        )
                    }
                }
            },
        )
    }
}

private const val EPUB_FRAGMENT_TAG = "epub_reader_fragment"
private const val SEEK_SETTLE_DELAY_MS = 650L
