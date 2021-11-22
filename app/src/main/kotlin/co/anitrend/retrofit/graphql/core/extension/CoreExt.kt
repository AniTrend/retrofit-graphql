package co.anitrend.retrofit.graphql.core.extension

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import co.anitrend.arch.ui.view.image.SupportImageView
import co.anitrend.retrofit.graphql.core.model.FragmentItem
import coil.Coil
import coil.load
import coil.request.Disposable
import coil.transform.Transformation
import coil.transition.CrossfadeTransition
import timber.log.Timber

/**
 * Helper extension for changing parcels to bundles
 */
fun Parcelable.toBundle(key: String) =
    Bundle().apply {
        putParcelable(key, this@toBundle)
    }


/**
 * Checks for existing fragment in [FragmentManager], if one exists that is used otherwise
 * a new instance is created.
 *
 * @return tag of the fragment
 *
 * @see androidx.fragment.app.commit
 */
inline fun FragmentManager.commit(
    @IdRes contentFrame: Int,
    fragmentItem: FragmentItem<*>?,
    action: FragmentTransaction.() -> Unit
) : String? {
    if (fragmentItem != null) {
        val fragmentTag = fragmentItem.tag()
        val backStack = findFragmentByTag(fragmentTag)

        commit {
            action()
            backStack?.let {
                replace(contentFrame, it, fragmentTag)
            } ?: replace(
                contentFrame,
                fragmentItem.fragment,
                fragmentItem.parameter,
                fragmentTag
            )
        }
        return fragmentTag
    }

    Timber.tag(javaClass.simpleName).v("FragmentItem model is null")
    return null
}

/**
 * Image loader helper for [Coil]
 */
fun SupportImageView.using(
    imageUrl: String?,
    placeHolder: Drawable? = null,
    vararg transformations: Transformation = emptyArray()
): Disposable {
    return load(imageUrl) {
        placeholder(placeHolder)
        transition(CrossfadeTransition(350))
        transformations(transformations.toList())
    }
}