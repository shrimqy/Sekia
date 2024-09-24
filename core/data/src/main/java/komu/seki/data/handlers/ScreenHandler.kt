package komu.seki.data.handlers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.Builder
import android.accessibilityservice.InputMethod
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import komu.seki.domain.models.InteractiveControl
import komu.seki.domain.models.KeyboardActionType
import komu.seki.domain.models.ScrollDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max


class ScreenHandler : AccessibilityService() {

    companion object {
        private var instance: ScreenHandler? = null

        fun getInstance(): ScreenHandler? {
            return instance
        }
    }
    private lateinit var deviceScreen: Point
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

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

        val info = serviceInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR
        }
        serviceInfo = info
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

        Log.d("ScreenHandler", "Hold Scaled coordinates: x=$scaledX, y=$scaledY")

        if (scaledX < 0 || scaledX > deviceScreen.x || scaledY < 0 || scaledY > deviceScreen.y) {
            Log.e("ScreenHandler", "Scaled coordinates out of bounds")
            return
        }

        val path = Path()
        path.moveTo(scaledX, scaledY)
        val stroke = GestureDescription.StrokeDescription(path, 0,500)
        gestureBuilder.addStroke(stroke)
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun performScroll(scroll: InteractiveControl.ScrollEvent) {
        when (scroll.direction) {
            ScrollDirection.UP -> performGlobalAction(GESTURE_SWIPE_DOWN)
            ScrollDirection.DOWN -> performGlobalAction(GESTURE_SWIPE_UP)
        }
    }

    private var gestureBuilder: Builder? = null
    private var maxStrokeCount = 19 // Adjust according to the system limit
    private var currentStrokeCount = 0

    private val swipeBuffer = mutableListOf<SwipeSegment>()
    private var swipeBufferStartTime: Long = 0

    private data class SwipeSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val duration: Long
    )



    fun performSwipe(swipe: InteractiveControl.SwipeEvent) {
        val scaledStartX = (swipe.startX / swipe.frameWidth * deviceScreen.x).toFloat()
        val scaledStartY = (swipe.startY / swipe.frameHeight * deviceScreen.y).toFloat()
        val scaledEndX = (swipe.endX / swipe.frameWidth * deviceScreen.x).toFloat()
        val scaledEndY = (swipe.endY / swipe.frameHeight * deviceScreen.y).toFloat()

        Log.d("ScreenHandler", "Swipe coordinates: startX=$scaledStartX, startY=$scaledStartY endX=$scaledEndX, endY=$scaledEndY, duration:${swipe.duration}, WillContinue: ${swipe.willContinue}")

        // Handle bottom edge swipes
        if (handleBottomEdgeSwipe(scaledStartY, scaledEndY, swipe.duration)) {
            return
        }

        if (swipeBuffer.isEmpty()) {
            swipeBufferStartTime = System.currentTimeMillis()
        }

        swipeBuffer.add(SwipeSegment(scaledStartX, scaledStartY, scaledEndX, scaledEndY, swipe.duration.toLong()))

        if (!swipe.willContinue) {
            dispatchBufferedSwipe()
        }
    }

    private fun dispatchBufferedSwipe() {
        if (swipeBuffer.isEmpty()) return

        val gestureBuilder = GestureDescription.Builder()
        val path = Path()

        var totalDuration: Long = 0
        swipeBuffer.forEachIndexed { index, segment ->
            if (index == 0) {
                path.moveTo(segment.startX, segment.startY)
            }
            path.lineTo(segment.endX, segment.endY)
            totalDuration += segment.duration
        }

        // Ensure a minimum duration
        totalDuration = max(totalDuration, 100)

        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, totalDuration))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d("ScreenHandler", "Buffered swipe completed successfully. Total duration: $totalDuration ms, Segments: ${swipeBuffer.size}")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d("ScreenHandler", "Buffered swipe gesture failed. Total duration: $totalDuration ms, Segments: ${swipeBuffer.size}")
            }
        }, null)

        swipeBuffer.clear()
    }

    private fun handleBottomEdgeSwipe(startY: Float, endY: Float, duration: Double): Boolean {
        val bottomEdgeThreshold = deviceScreen.y * 0.01
        val minimumSwipeDurationForRecents = 60.0 // in milliseconds

        if (startY > deviceScreen.y - bottomEdgeThreshold && endY < startY) {
            if (duration >= minimumSwipeDurationForRecents) {
                Log.d("ScreenHandler", "Swipe from bottom detected, opening recents")
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            } else {
                Log.d("ScreenHandler", "Swipe too short, triggering home")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            return true
        }
        return false
    }
    // Dispatch the gesture and reset the builder
    private fun updateGesture(gesture: Builder?) {
        CoroutineScope(Dispatchers.Main).launch {
            gesture?.let { builder ->
                dispatchGesture(builder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d("ScreenHandler", "Swipe segment updated $currentStrokeCount")
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.d("ScreenHandler", "Swipe gesture failed $currentStrokeCount")
                        gestureBuilder = null
                    }
                }, null)
            }
        }

    }

    // Reset and initialize the gesture builder
    private fun createGestureBuilder() {
        gestureBuilder = Builder() // Start a new gesture builder
        currentStrokeCount = 0 // Reset stroke count
    }


//    private val keyEventQueue: Queue<String> = LinkedList()
//    private var isProcessing = false
//
//    // Function to receive new key events
//    fun receiveKeyEvent(key: String) {
//        keyEventQueue.add(key)
//        processNextKeyEvent()
//    }
//
//    private fun processNextKeyEvent() {
//        if (isProcessing || keyEventQueue.isEmpty()) return
//
//        isProcessing = true
//        val key = keyEventQueue.poll() ?: return
//
//        // Simulate waiting for the performAction to complete
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            performTextInput(key)
//            isProcessing = false
//        } else {
//            Handler(Looper.getMainLooper()).postDelayed({
//                isProcessing = false
//                processNextKeyEvent() // Process the next key after a delay
//            }, 110) // Adjust the delay based on actual input speed
//        }
//    }


    fun performTextInput(key: String) {
        val rootNode = rootInActiveWindow ?: return

        // Find the focused input field
        val inputNode = findEditableNode(rootNode) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val inputConnection = inputMethod?.currentInputConnection ?: return
            inputConnection.commitText(key, key.length, null)
        } else {
            val currentText = inputNode.text?.toString() ?: ""
            val caretPosition = getCurrentCaretPosition(inputNode)

            // Create new text based on the caret position
            val newText = currentText.substring(0, caretPosition) + key + currentText.substring(caretPosition)

            // Set the new text directly
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)

            })
            return
        }
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

    private fun getCurrentCaretPosition(inputNode: AccessibilityNodeInfo): Int {
        // Check if the node is editable
        if (!inputNode.isEditable) return 0

        // Get selection start and end positions
        val selectionStart = inputNode.getExtras().getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
        val selectionEnd = inputNode.getExtras().getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 0)

        // If there's a selection, return the start position
        return if (selectionStart != selectionEnd) {
            selectionStart
        } else {
            // If no selection, return the length of the text as the caret position for appending
            inputNode.text?.length ?: 0
        }
    }


    private fun getScreenSize(): Point {
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }

    fun performKeyboardAction(action: KeyboardActionType) {
        val rootNode = rootInActiveWindow ?: return

        // Find the focused input field
        val inputNode = findEditableNode(rootNode)
        // Get the current text and placeholder
        val currentText = inputNode?.text?.toString() ?: ""
        val placeholderText = inputNode?.hintText?.toString() ?: ""

        inputNode?.let {
            when(action) {
                KeyboardActionType.Tab -> {
                    // Usually, TAB moves focus to the next field, so we can use ACTION_FOCUS here
                    inputNode.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
                }
                KeyboardActionType.Backspace -> {
                    if (currentText.isNotEmpty() && currentText != placeholderText) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val inputConnection = inputMethod?.currentInputConnection ?: return
                            inputConnection.deleteSurroundingText(1, 0)
                        } else {
                            val newText = currentText.dropLast(1) // Remove the last character
                            setInputText(inputNode, newText) // Set the new text directly
                        }
                    } else {
                        return
                    }
                }
                KeyboardActionType.Enter -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val inputConnection = inputMethod?.currentInputConnection ?: return
                        val editorInfo = inputMethod?.currentInputEditorInfo ?: return
                        when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
                            EditorInfo.IME_ACTION_GO -> {
                                // "Done" action, e.g., submit a form
                                inputConnection.performEditorAction(EditorInfo.IME_ACTION_DONE)
                            }
                            EditorInfo.IME_ACTION_SEARCH -> {
                                // "Search" action, e.g., initiate a search
                                inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                            }
                            EditorInfo.IME_ACTION_SEND -> {
                                // "Send" action, e.g., send a message
                                inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND)
                            }
                            EditorInfo.IME_ACTION_NEXT -> {
                                // "Next" action, move to the next input field
                                inputConnection.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                            }
                            else -> {
                                // Default action: insert a new line
                                inputConnection.commitText("\n", 1, null)
                            }
                        }
                    } else
                    {
                        // Handle for versions below TIRAMISU
                        val newText = currentText + "\n" // Add a new line
                        setInputText(inputNode, newText)
                    }
                }
                KeyboardActionType.Escape -> {
                    // Typically Escape doesn't have a direct equivalent in Android, so no action.
                }
                KeyboardActionType.CtrlC -> {
                    // Copy text to clipboard
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val inputConnection = inputMethod?.currentInputConnection ?: return
                        inputConnection.performContextMenuAction(android.R.id.copy)
                    } else {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("CopiedText", currentText)
                        clipboard.setPrimaryClip(clip)
                    }
                }
                KeyboardActionType.CtrlV -> {
                    // Paste text from clipboard

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val inputConnection = inputMethod?.currentInputConnection ?: return
                        inputConnection.performContextMenuAction(android.R.id.paste)
                    } else {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        val pasteData = clip?.getItemAt(0)?.text?.toString() ?: ""
                        setInputText(inputNode, currentText + pasteData)
                    }
                }
                KeyboardActionType.CtrlX -> {
                    // Cut text (copy to clipboard and delete from input)
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CutText", currentText)
                    clipboard.setPrimaryClip(clip)
                    setInputText(inputNode, "")
                }
                KeyboardActionType.CtrlA -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val inputConnection = inputMethod?.currentInputConnection ?: return
                        inputConnection.performContextMenuAction(android.R.id.selectAll)
                    } else {
                        // Select all text
                        val arguments = Bundle()
                        arguments.putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                            0
                        )
                        arguments.putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                            currentText.length
                        )
                        inputNode.performAction(
                            AccessibilityNodeInfo.ACTION_SET_SELECTION,
                            arguments
                        )
                    }
                }
            }
        }
    }

    // Helper function to set text in an editable field
    private fun setInputText(node: AccessibilityNodeInfo, newText: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }


    fun wakeDevice() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ScreenHandler::WakeLockTag"
        )
        wakeLock.acquire(3000) // Wake up the device for 3 seconds
        wakeLock.release()
    }
}

