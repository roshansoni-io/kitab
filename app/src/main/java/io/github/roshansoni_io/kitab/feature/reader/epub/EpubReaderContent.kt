package io.github.roshansoni_io.kitab.feature.reader.epub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.feature.reader.ReaderScaffold
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
    var chromeVisible by remember { mutableStateOf(settings.readerControlsInitiallyVisible) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val progression =
        locator
            ?.locations
            ?.totalProgression
            ?.toFloat()
            ?.coerceIn(0f, 1f)
            ?: 0f

    SideEffect {
        session.onCenterTap = { chromeVisible = !chromeVisible }
        session.onExternalLink = onOpenExternalLink
        session.onProgress = { current ->
            current.locations.totalProgression?.let { progression ->
                onProgress(current.toJSON().toString(), progression)
            }
        }
    }

    LaunchedEffect(preferences) {
        session.preferences = preferences
        session.navigator?.submitPreferences(preferences)
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

    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        chromeVisible = chromeVisible,
        bottomBar = {
            EpubReaderBottomBar(
                progress = if (isSeeking) seekValue else progression,
                onSeek = { value ->
                    isSeeking = true
                    seekValue = value
                },
                onSeekFinished = {
                    isSeeking = false
                    scope.launch { session.seekTo(seekValue.toDouble()) }
                },
                onPrevious = { session.navigator?.goBackward() },
                onNext = { session.navigator?.goForward() },
            )
        },
    ) {
        EpubNavigatorHost(session = session, activity = activity)
    }
}

@Composable
private fun EpubNavigatorHost(
    session: EpubSession,
    activity: FragmentActivity?,
) {
    var requested by remember(session, activity) { mutableStateOf(false) }
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

@Composable
private fun EpubReaderBottomBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sliderState = rememberSliderState(value = progress)
    LaunchedEffect(progress) {
        sliderState.value = progress
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Page",
                )
            }

            Slider(
                state = sliderState,
                onValueChange = onSeek,
                onValueChangeFinished = onSeekFinished,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = String.format(Locale.US, "%d%%", (progress * 100).roundToInt()),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Page",
                )
            }
        }
    }
}

internal const val EPUB_FRAGMENT_TAG = "epubReaderFragment"
