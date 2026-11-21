package com.leonai.android

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LeonAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    companion object {
        @Volatile private var instance: LeonAccessibilityService? = null
        fun navigateBack(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        fun navigateHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
    }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }
}
