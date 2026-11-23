package io.github.roshansoni_io.kitab.feature.reader.epub

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

internal fun Context.findFragmentActivity(): FragmentActivity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}
