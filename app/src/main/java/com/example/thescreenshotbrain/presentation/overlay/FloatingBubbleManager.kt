package com.example.thescreenshotbrain.presentation.overlay

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import com.example.thescreenshotbrain.core.common.DateTimeParser
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.thescreenshotbrain.R
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FloatingBubbleManager @Inject constructor(
    @ApplicationContext private val context : Context
){
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    fun showFloatingBubble(text: String, type: String){
        // Check permission
        if(!Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        //Check main thread
        if(Looper.myLooper() != Looper.getMainLooper()){
            Handler(Looper.getMainLooper()).post {
                showFloatingBubble(text, type)
            }
            return
        }

        hide()

        //Configuration layout params
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = dpToPx(100)

        //Create view contains compose
        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.onCreate()

        val themeContext = ContextThemeWrapper(context, R.style.Theme_TheScreenshotBrain)
        overlayView = ComposeView(themeContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(null)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent{
                floatingBubble(
                    text = text,
                    type = type,
                    onOpenAction = { handleAction(text, type); hide() },
                    onDismiss = { hide() }
                )
            }
        }

        // resume lifecycle
        lifecycleOwner.onResume()


        try{
            windowManager.addView(overlayView, params)
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    fun hide(){
        overlayView?.let{ view ->
            view.disposeComposition()
            windowManager.removeView(view)
            overlayView = null
        }
    }

    private fun dpToPx(dp: Int): Int{
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun handleAction(text: String, type: String){
        try {
            val intent = when (type) {
                ScreenshotEntity.TYPE_URL -> {
                    var finalUrl = text.trim()
                    if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                        finalUrl = "https://$finalUrl"
                    }

                    Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                }

                ScreenshotEntity.TYPE_PHONE -> {
                    val cleanPhone = text.replace(Regex("[^0-9]"), "")
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                }

                ScreenshotEntity.TYPE_MAP -> {
                    //open ggmap, address
                    val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(text)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    
                    //If don't have ggmap, use web
                    if (mapIntent.resolveActivity(context.packageManager) == null) {
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(text)}"))
                    } else {
                        mapIntent
                    }
                }

                ScreenshotEntity.TYPE_EVENT -> {
                    // Parse thời gian từ text
                    android.util.Log.d("FloatingBubble", "Parsing event text: $text")
                    val eventCalendar = DateTimeParser.findAndParseDateTime(text)
                    
                    if (eventCalendar != null) {
                        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        android.util.Log.d("FloatingBubble", "Parsed date: ${dateFormat.format(eventCalendar.time)}")
                    } else {
                        android.util.Log.w("FloatingBubble", "Failed to parse date from: $text")
                    }
                    
                    //open calendar
                    Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, "Sự kiện từ Screenshot")
                        putExtra(CalendarContract.Events.DESCRIPTION, "Nội dung:\n$text")
                        
                        //set time if can parse
                        eventCalendar?.let { cal ->
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis + (60 * 60 * 1000)) // 1 giờ sau
                            putExtra(CalendarContract.Events.ALL_DAY, false)
                        }
                    }
                }

                else -> null
            }

            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Không hỗ trợ loại này: $type", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Không thể mở: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

class MyLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner{
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate(){
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume(){
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy(){
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}