package com.budgettracker.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** AppWidgetProvider entry point for the Weekly widget - see res/xml/weekly_widget_info.xml. */
class WeeklyBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeeklyBudgetWidget()
}
