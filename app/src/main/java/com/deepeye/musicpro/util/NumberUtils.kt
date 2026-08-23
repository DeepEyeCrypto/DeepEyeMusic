package com.deepeye.musicpro.util

fun formatCompactNumber(number: Long): String {
    if (number < 1000) return number.toString()
    val exp = (Math.log(number.toDouble()) / Math.log(1000.0)).toInt()
    val format = java.text.DecimalFormat("0.#")
    val value = number / Math.pow(1000.0, exp.toDouble())
    return format.format(value) + "kMGTPE"[exp - 1]
}
