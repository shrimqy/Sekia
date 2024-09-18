package komu.seki.data.handlers

import android.app.Instrumentation
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import komu.seki.domain.models.InteractiveControl
import komu.seki.domain.models.InteractiveControlMessage

fun screenHandler(context: Context, message: InteractiveControlMessage) {
    when(val control = message.control) {
        is InteractiveControl.Keyboard -> TODO()
        is InteractiveControl.Mouse -> {
            val instrumentation = Instrumentation()
            val x = control.x.toInt()
            val y = control.y.toInt()

            // Simulate cursor movement
            instrumentation.sendPointerSync(MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_MOVE,
                x.toFloat(),
                y.toFloat(),
                0
            ))

            // Simulate single click (touch down and touch up)
            // Touch down
            instrumentation.sendPointerSync(MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                x.toFloat(),
                y.toFloat(),
                0
            ))

            // Touch up (release)
            instrumentation.sendPointerSync(MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                x.toFloat(),
                y.toFloat(),
                0
            ))
        }
        is InteractiveControl.Scroll -> TODO()
        is InteractiveControl.Swipe -> TODO()
        is InteractiveControl.TextInput -> TODO()
    }
}