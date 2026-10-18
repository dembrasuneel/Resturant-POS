package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val receiptDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatReceiptDateTime(timestamp: Long): String = receiptDateFormat.format(Date(timestamp))

    fun isToday(timestamp: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        return fmt.format(Date(timestamp)) == fmt.format(Date())
    }
}
