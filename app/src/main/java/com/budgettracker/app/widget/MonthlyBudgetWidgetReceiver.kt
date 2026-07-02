package com.budgettracker.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** AppWidgetProvider entry point for the Monthly widget - see res/xml/monthly_widget_info.xml. */
class MonthlyBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthlyBudgetWidget()
}
