package com.zhihu.matisse.filter

import android.content.Context
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item

/**
 * Filter for choosing a [Item]. You can add multiple Filters through
 * [SelectionCreator.addFilter].
 */
abstract class Filter {
    /**
     * Against what mime types this filter applies.
     */
    protected abstract fun constraintTypes(): Set<MimeType>

    /**
     * Invoked for filtering each item.
     *
     * @return null if selectable, [IncapableCause] if not selectable.
     */
    abstract fun filter(context: Context, item: Item): IncapableCause?

    /**
     * Whether an [Item] need filtering.
     */
    protected fun needFiltering(context: Context, item: Item): Boolean {
        return constraintTypes().any { type ->
            type.checkType(
                context.contentResolver,
                item.contentUri
            )
        }
    }

    companion object {
        /**
         * Convenient constant for a minimum value.
         */
        const val MIN = 0

        /**
         * Convenient constant for a maximum value.
         */
        const val MAX = Int.MAX_VALUE

        /**
         * Convenient constant for 1024.
         */
        const val K = 1024
    }
}