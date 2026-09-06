package com.example.data

/**
 * نگه‌دارنده سراسری نام کارگاه برای لایه‌هایی که مستقیماً به ViewModel دسترسی ندارند
 * (مانند Repository، NotificationManager، PrinterManager و SupabaseManager).
 * مقدار پیش‌فرض "کارگاه" است و از طریق پاسخ endpoint رسمی /driver-api/workshop به‌روزرسانی می‌شود.
 */
object WorkshopNameHolder {
    var current: String = "کارگاه"
}
