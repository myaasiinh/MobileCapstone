package com.myaasiinh.core.util

import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_LONG,
    view: View = requireView()
) {
    Snackbar.make(view, message, duration).show()
}

// Extension function for search fragment recyclerView
inline fun <T : View> T.showIfOrInvisible(condition: (View) -> Boolean) {
    if (condition(this)) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.INVISIBLE
    }
}

// Extension function for search fragment query listener
inline fun SearchView.onQueryTextSubmit(crossinline listener: (String) -> Unit) {
    this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            if (!query.isNullOrBlank()) {
                listener(query)
            }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            return true
        }
    })
}

val <T> T.exhaustive: T get() = this