@file:OptIn(ExperimentalReadiumApi::class)

package io.github.roshansoni_io.kitab.feature.reader.epub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import io.github.roshansoni_io.kitab.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import kotlin.math.max

/**
 * Hosts the Readium [EpubNavigatorFragment] in its own [FrameLayout].
 *
 * The navigator factory is installed on [onCreate] before the child fragment
 * manager restores its state, which is required by Readium.
 */
internal class EpubReaderFragment : Fragment() {
    private var session: EpubSession? = null
    private var navigator: EpubNavigatorFragment? = null
    private var directionalNavigationAdapter: DirectionalNavigationAdapter? = null

    private val externalLinkListener =
        object : EpubNavigatorFragment.Listener {
            override fun onExternalLinkActivated(url: AbsoluteUrl) {
                session?.onExternalLink?.invoke(url.toString())
            }
        }

    private val paginationListener =
        object : EpubNavigatorFragment.PaginationListener {
            override fun onPageChanged(
                pageIndex: Int,
                totalPages: Int,
                locator: Locator,
            ) {
                session?.onPageChanged(pageIndex, totalPages, locator)
            }

            override fun onPageLoaded() {
                injectPaginationCss()
            }
        }

    private val centerTapListener =
        object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val navigator = navigator ?: return false
                val width = navigator.publicationView.width.toFloat()
                if (width <= 0f) return false
                val edgeSize = max(MIN_CENTER_TAP_EDGE_PX, width * CENTER_TAP_EDGE_FRACTION)
                if (event.point.x <= edgeSize || event.point.x >= width - edgeSize) {
                    return false
                }
                session?.onCenterTap?.invoke()
                return false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val path = requireArguments().getString(ARG_PATH).orEmpty()
        val session = EpubSessionRegistry.current(path)
        this.session = session
        if (session == null) {
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            requireActivity().finish()
            return
        }

        childFragmentManager.fragmentFactory =
            session.navigatorFactory.createFragmentFactory(
                initialLocator = if (savedInstanceState == null) session.initialLocator else null,
                initialPreferences = session.preferences,
                listener = externalLinkListener,
                paginationListener = paginationListener,
                configuration =
                    EpubNavigatorFragment.Configuration(
                        shouldApplyInsetsPadding = false,
                    ),
            )
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                replace(R.id.epub_navigator_host, EpubNavigatorFragment::class.java, null)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        FrameLayout(inflater.context).apply {
            id = R.id.epub_navigator_host
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val session = session
        val navigator = childFragmentManager.findFragmentById(R.id.epub_navigator_host) as? EpubNavigatorFragment
        if (session == null || navigator == null) {
            return
        }
        this.navigator = navigator
        session.navigator = navigator
        val directionalAdapter = DirectionalNavigationAdapter(navigator)
        directionalNavigationAdapter = directionalAdapter
        navigator.addInputListener(centerTapListener)
        navigator.addInputListener(directionalAdapter)
        navigator.submitPreferences(session.preferences)

        viewLifecycleOwner.lifecycleScope.launch {
            navigator.currentLocator.collectLatest { locator ->
                session.onLocatorChanged(locator)
                delay(PROGRESS_SAVE_DELAY_MS)
                session.publishProgress(session.locator.value ?: locator)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val locator = session?.locator?.value ?: navigator?.currentLocator?.value ?: return
        session?.publishProgress(locator)
    }

    override fun onDestroyView() {
        navigator?.removeInputListener(centerTapListener)
        directionalNavigationAdapter?.let { navigator?.removeInputListener(it) }
        directionalNavigationAdapter = null
        navigator = null
        session?.navigator = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        val currentSession = session
        if (currentSession != null && (isRemoving || requireActivity().isFinishing)) {
            EpubSessionRegistry.clear(currentSession.path)
            currentSession.close()
        }
        session = null
        super.onDestroy()
    }

    private fun injectPaginationCss() {
        if (view == null || session?.isFixedLayout == true) return
        val nav = navigator ?: return
        if (nav.view == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                nav.evaluateJavascript(INJECT_PAGINATION_CSS_JS)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val PROGRESS_SAVE_DELAY_MS = 750L
        private const val MIN_CENTER_TAP_EDGE_PX = 80f
        private const val CENTER_TAP_EDGE_FRACTION = 0.3f

        fun newInstance(path: String): EpubReaderFragment =
            EpubReaderFragment().apply {
                arguments = Bundle().apply { putString(ARG_PATH, path) }
            }
    }
}

internal const val INJECT_PAGINATION_CSS_JS =
    """
    (function() {
      var head = document.head || document.documentElement;
      if (!head || document.getElementById('kitab-pagination-fix')) return;
      var style = document.createElement('style');
      style.id = 'kitab-pagination-fix';
      style.textContent =
        'table, pre, blockquote, figure, div, section, article {' +
        '  break-inside: auto !important;' +
        '  -webkit-column-break-inside: auto !important;' +
        '  page-break-inside: auto !important;' +
        '}' +
        'img, svg, video, canvas {' +
        '  max-width: 100% !important;' +
        '  max-height: 92vh !important;' +
        '  height: auto !important;' +
        '  object-fit: contain !important;' +
        '}';
      head.appendChild(style);
    })();
    """
