package komu.seki.data.handlers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import komu.seki.domain.models.InteractiveControl
import komu.seki.domain.models.ScrollDirection


class ScreenHandler : AccessibilityService() {

    companion object {
        private var instance: ScreenHandler? = null

        fun getInstance(): ScreenHandler? {
            return instance
        }
    }
    private lateinit var deviceScreen: Point

    override fun onCreate() {
        super.onCreate()
        Log.d("ScreenHandler", "onCreate called")
        instance = this
        deviceScreen = getScreenSize()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ScreenHandler", "onServiceConnected called")
        instance = this
    }


    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("ScreenHandler", "onAccessibilityEvent called")
    }

    override fun onInterrupt() {
        Log.d("ScreenHandler", "onInterrupt called")
    }

    fun performTap(mouse: InteractiveControl.SingleTap) {

        val gestureBuilder = GestureDescription.Builder()
        // Scale the coordinates
        val scaledX = (mouse.x / mouse.frameWidth * deviceScreen.x).toFloat()
        val scaledY = (mouse.y / mouse.frameHeight * deviceScreen.y).toFloat()

        Log.d("ScreenHandler", "Scaled coordinates: x=$scaledX, y=$scaledY")

        if (scaledX < 0 || scaledX > deviceScreen.x || scaledY < 0 || scaledY > deviceScreen.y) {
            Log.e("ScreenHandler", "Scaled coordinates out of bounds")
            return
        }

        val path = android.graphics.Path()
        path.moveTo(scaledX, scaledY)
        val stroke = GestureDescription.StrokeDescription(path, 0,50)
        gestureBuilder.addStroke(stroke)
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun performHoldTap(mouse: InteractiveControl.HoldTap) {

        val gestureBuilder = GestureDescription.Builder()
        // Scale the coordinates
        val scaledX = (mouse.x / mouse.frameWidth * deviceScreen.x).toFloat()
        val scaledY = (mouse.y / mouse.frameHeight * deviceScreen.y).toFloat()

        Log.d("ScreenHandler", "Scaled coordinates: x=$scaledX, y=$scaledY")

        if (scaledX < 0 || scaledX > deviceScreen.x || scaledY < 0 || scaledY > deviceScreen.y) {
            Log.e("ScreenHandler", "Scaled coordinates out of bounds")
            return
        }

        val path = android.graphics.Path()
        path.moveTo(scaledX, scaledY)
        val stroke = GestureDescription.StrokeDescription(path, 0,300)
        gestureBuilder.addStroke(stroke)
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun performScroll(scroll: InteractiveControl.ScrollEvent) {
        when (scroll.direction) {
            ScrollDirection.UP -> performGlobalAction(GESTURE_SWIPE_UP)
            ScrollDirection.DOWN -> performGlobalAction(GESTURE_SWIPE_DOWN)
        }
    }

    fun performSwipe(swipe: InteractiveControl.SwipeEvent) {
        val scaledStartX = (swipe.startX / swipe.frameWidth * deviceScreen.x).toFloat()
        val scaledStartY = (swipe.startY / swipe.frameHeight * deviceScreen.y).toFloat()
        val scaledEndX = (swipe.endX / swipe.frameWidth * deviceScreen.x).toFloat()
        val scaledEndY = (swipe.endY / swipe.frameHeight * deviceScreen.y).toFloat()

        val path = android.graphics.Path().apply {
            moveTo(scaledStartX, scaledStartY)
            lineTo(scaledEndX, scaledEndY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun performKeyboardInput(text: String) {
        val rootNode = rootInActiveWindow ?: return
        // Find the focused input field
        val inputNode = findEditableNode(rootNode)
        inputNode?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        })
    }

    private fun performTextInput(text: String) {
        performKeyboardInput(text)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i) ?: continue
            val editableNode = findEditableNode(childNode)
            if (editableNode != null) return editableNode
        }
        return null
    }


    private fun getScreenSize(): Point {
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }
}

