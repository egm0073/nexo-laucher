package com.nexo.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/** Host de widgets cuyas vistas detectan una pulsación larga sobre el propio widget. */
class NexoWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
        LongPressWidgetView(context)
}

class LongPressWidgetView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var fired = false
    private val check = Runnable {
        val action = onLongPress
        if (action != null && isAttachedToWindow) {
            fired = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            action()
        }
    }

    private fun track(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fired = false
                downX = ev.x
                downY = ev.y
                if (onLongPress != null) postDelayed(check, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) removeCallbacks(check)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(check)
        }
    }

    // Tras la pulsación larga se quedan los toques restantes, para que el widget no lo tome como un clic.
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        track(ev)
        return fired
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Si ningún hijo recoge el toque, llega aquí: se sigue escuchando para poder cancelar a tiempo.
        if (ev.actionMasked != MotionEvent.ACTION_DOWN || fired) track(ev)
        return super.onTouchEvent(ev) || onLongPress != null
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        removeCallbacks(check)
    }
}
