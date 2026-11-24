package com.shop.salesapp

import android.content.Context

/** Remembers which product labels have been physically printed on this device. */
internal object QrLabelStatusStore {
    private const val PREFS = "sales_app_qr_label_status"
    private const val KEY_PRINTED_SKUS = "printed_skus"

    fun loadPrintedSkus(context: Context): Set<String> = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_PRINTED_SKUS, emptySet())
        .orEmpty()
        .toSet()

    fun savePrintedSkus(context: Context, printedSkus: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PRINTED_SKUS, printedSkus.toSet())
            .apply()
    }
}
