package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class FamiQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "widget_action"
        const val ACTION_WRITE = "write"
        const val ACTION_VOICE = "voice"
        const val ACTION_SCAN = "scan"
        const val ACTION_GALLERY = "gallery"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_fami_quick)

            views.setOnClickPendingIntent(
                R.id.widget_btn_write,
                buildActionIntent(context, ACTION_WRITE, 201)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_send,
                buildActionIntent(context, ACTION_WRITE, 202)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_voice,
                buildActionIntent(context, ACTION_VOICE, 203)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_scan,
                buildActionIntent(context, ACTION_SCAN, 204)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_gallery,
                buildActionIntent(context, ACTION_GALLERY, 205)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildActionIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, WidgetQuickActivity::class.java).apply {
                this.action = "app.somosfami.WIDGET_OVERLAY_$action"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, action)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
