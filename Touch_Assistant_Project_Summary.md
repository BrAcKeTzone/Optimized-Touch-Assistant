# Touch Assistant Android App --- Project Summary

## 1. Project Goal

Build a lightweight Android Touch Assistant app in Kotlin using standard
Android Views, WindowManager overlays, Accessibility Services,
SharedPreferences, and system APIs.

Target: - Android API 24 through API 34+ - Kotlin - No Jetpack Compose -
Minimal dependencies - Target APK size under approximately 3 MB where
practical - R8/minification enabled for release builds - Vector
Drawables instead of large raster assets - SharedPreferences instead of
a heavy database/ORM

Package: `com.bracketzone.touchassistant`

------------------------------------------------------------------------

## 2. Original Required Features

### System Permissions / Components

Manifest requirements: - `SYSTEM_ALERT_WINDOW` - `FOREGROUND_SERVICE` -
`FOREGROUND_SERVICE_SPECIAL_USE` - `CAMERA` - `WRITE_SETTINGS`

Services: - `TouchAccessibilityService` - `FloatingWidgetService`

Accessibility service: - Bound using
`android.permission.BIND_ACCESSIBILITY_SERVICE` - Uses
`accessibility_service_config.xml`

Floating service: - Foreground service - Notification channel support -
Displays an overlay using `WindowManager` - Uses
`TYPE_APPLICATION_OVERLAY` on Android 8+

------------------------------------------------------------------------

## 3. Current Project Progress

The Android Studio project was successfully created using:

-   Empty Views Activity
-   Kotlin
-   Minimum SDK API 24
-   Kotlin DSL
-   Package `com.example.touchassistant`

Gradle build succeeded.

The initial app showed the default "Hello World!" screen.

The app was then developed enough that:

-   Required Android settings buttons open their appropriate settings
    pages.
-   The floating button appears over other applications.
-   The floating button can be dragged.
-   The floating button snaps to the left or right screen edge.

------------------------------------------------------------------------

# 4. Floating Widget Requirements

`FloatingWidgetService.kt` is responsible for the floating assistant
button.

Current implementation uses:

``` kotlin
WindowManager
TYPE_APPLICATION_OVERLAY
```

The button is an `ImageView`.

Current default size:

``` text
56dp × 56dp
```

Initial position:

``` text
x = 16dp
y = 220dp
```

The button currently uses an Android built-in icon:

``` kotlin
android.R.drawable.ic_menu_manage
```

and a circular blue background.

------------------------------------------------------------------------

## 5. Dragging

Dragging uses:

``` kotlin
ViewConfiguration.get(context).scaledTouchSlop
```

The touch system tracks:

-   `ACTION_DOWN`
-   `ACTION_MOVE`
-   `ACTION_UP`
-   `ACTION_CANCEL`

The implementation distinguishes taps from drags using touch slop.

When dragging: - `windowParams.x` is updated - `windowParams.y` is
updated - The position is clamped to screen boundaries -
`WindowManager.updateViewLayout()` updates the overlay

The floating button snaps to the nearest horizontal edge using:

``` kotlin
ValueAnimator
```

with:

``` text
250 ms
DecelerateInterpolator
```

------------------------------------------------------------------------

# 6. Idle / Auto-Hide Behavior

Current constants:

``` kotlin
private const val IDLE_DELAY_MS = 3_000L

private const val IDLE_ALPHA = 0.3f

private const val IDLE_ANIMATION_DURATION_MS = 250L
```

After 3 seconds of inactivity: - The button animates toward an idle
state. - Alpha becomes `0.3f`. - The button translates 60% of its width
off-screen. - An `AnimatorSet` runs alpha and translation animations
together.

When the user touches the button: - Idle callback is cancelled. - Idle
animation is cancelled. - Alpha is restored to the configured base
alpha. - Translation is restored to `0f`.

The base alpha is stored using `GesturePreferences`.

------------------------------------------------------------------------

# 7. Gesture System

Gestures are intended to support:

1.  Single tap
2.  Double tap
3.  Long press

They are recognized using Android's:

``` kotlin
GestureDetector
```

The gestures are configurable through:

``` text
GesturePreferences.kt
```

Default configuration:

``` text
Single tap  → MAIN
Double tap  → SCREENSHOT
Long press  → NOTIFICATIONS
```

------------------------------------------------------------------------

# 8. GesturePreferences.kt

Current package:

``` text
com.bracketzone.touchassistant.preferences
```

Current class:

``` kotlin
class GesturePreferences(context: Context)
```

SharedPreferences name:

``` text
touch_assistant_preferences
```

Keys:

``` text
single_tap_action
double_tap_action
long_press_action
base_alpha
```

Defaults:

``` text
Single tap  = MAIN
Double tap  = SCREENSHOT
Long press  = NOTIFICATIONS
```

Base alpha:

``` text
Default = 1.0
Minimum = 0.1
Maximum = 1.0
```

Actions are stored using:

``` kotlin
action.name
```

and reconstructed using:

``` kotlin
AssistantAction.valueOf(...)
```

Invalid/corrupted stored values safely fall back to the specified
default.

------------------------------------------------------------------------

# 9. Important Gesture Bug Found

At one point all gestures appeared to trigger Notifications.

The cause was investigated.

The current `FloatingWidgetService.kt` had **two `ACTION_UP` cases**
inside `handleTouch()`.

The first `ACTION_UP` returned before sending the event to
`GestureDetector`.

This meant:

``` text
ACTION_DOWN → GestureDetector
ACTION_MOVE → GestureDetector
ACTION_UP   → NOT sent to GestureDetector
```

Therefore `GestureDetector` could interpret the touch as a long press.

The duplicate `ACTION_UP` block was identified as the bug.

------------------------------------------------------------------------

# 10. Correct handleTouch() Structure

The current `handleTouch()` should contain exactly one `ACTION_UP`
block.

Use this structure:

``` kotlin
private fun handleTouch(
    event: MotionEvent
): Boolean {

    when (event.actionMasked) {

        MotionEvent.ACTION_DOWN -> {

            cancelIdleHide()

            edgeAnimator?.cancel()

            floatingView.translationX = 0f

            initialTouchX =
                event.rawX

            initialTouchY =
                event.rawY

            initialWindowX =
                windowParams.x

            initialWindowY =
                windowParams.y

            isDragging = false

            gestureDetector.onTouchEvent(event)

            return true
        }

        MotionEvent.ACTION_MOVE -> {

            val deltaX =
                event.rawX - initialTouchX

            val deltaY =
                event.rawY - initialTouchY

            val distance =
                kotlin.math.sqrt(
                    deltaX * deltaX +
                            deltaY * deltaY
                )

            if (!isDragging) {

                if (
                    distance >
                    touchSlop.scaledTouchSlop
                ) {

                    isDragging = true
                }
            }

            gestureDetector.onTouchEvent(event)

            if (isDragging) {

                windowParams.x =
                    initialWindowX +
                            deltaX.toInt()

                windowParams.y =
                    initialWindowY +
                            deltaY.toInt()

                clampFloatingViewPosition()

                windowManager.updateViewLayout(
                    floatingView,
                    windowParams
                )
            }

            return true
        }

        MotionEvent.ACTION_UP -> {

            gestureDetector.onTouchEvent(event)

            if (isDragging) {
                snapToNearestEdge()
            }

            scheduleIdleHide()

            isDragging = false

            return true
        }

        MotionEvent.ACTION_CANCEL -> {

            gestureDetector.onTouchEvent(event)

            isDragging = false

            scheduleIdleHide()

            return true
        }
    }

    return false
}
```

There must NOT be a second `ACTION_UP` branch.

------------------------------------------------------------------------

# 11. Current GestureDetector Test

The gesture detector was temporarily changed to Toast messages to
isolate gesture recognition.

Current test callbacks:

``` kotlin
gestureDetector =
    GestureDetector(
        this,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(
                e: MotionEvent
            ): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(
                e: MotionEvent
            ): Boolean {

                Toast.makeText(
                    this@FloatingWidgetService,
                    "SINGLE TAP",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }

            override fun onDoubleTap(
                e: MotionEvent
            ): Boolean {

                Toast.makeText(
                    this@FloatingWidgetService,
                    "DOUBLE TAP",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }

            override fun onLongPress(
                e: MotionEvent
            ) {

                Toast.makeText(
                    this@FloatingWidgetService,
                    "LONG PRESS",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
```

`onDown()` returning `true` is important so the detector handles the
gesture stream.

------------------------------------------------------------------------

# 12. Gesture Testing Procedure

Before reconnecting actual actions, verify:

### Single tap

Expected:

``` text
SINGLE TAP
```

### Double tap

Expected:

``` text
DOUBLE TAP
```

### Long press

Expected:

``` text
LONG PRESS
```

### Drag

Expected: - Button moves. - Button snaps to an edge. - No gesture Toast
should appear.

The goal is to first prove that the GestureDetector correctly
distinguishes the three gestures.

------------------------------------------------------------------------

# 13. Action Dispatching

After gesture recognition is confirmed, reconnect:

``` text
GestureDetector
    ↓
GesturePreferences
    ↓
AssistantAction
    ↓
executeGestureAction()
    ↓
ActionExecutor
```

The intended function is:

``` kotlin
private fun executeGestureAction(
    action: AssistantAction
) {

    when (action) {

        AssistantAction.MAIN -> {
            showMainMenu()
        }

        AssistantAction.NONE -> {
            // Do nothing.
        }

        else -> {
            ActionExecutor(this)
                .execute(action)
        }
    }
}
```

The temporary Toast inside this function should eventually be removed.

------------------------------------------------------------------------

# 14. AssistantAction

Package:

``` text
com.bracketzone.touchassistant.core
```

The enum is intended to contain actions including:

### Navigation

``` text
HOME
BACK
RECENTS
```

### System panels

``` text
NOTIFICATIONS
QUICK_SETTINGS
POWER
```

### Accessibility/system

``` text
SPLIT_SCREEN
LOCK
SCREENSHOT
```

### Audio

``` text
VOLUME_UP
VOLUME_DOWN
MUTE_UNMUTE
TOGGLE_RINGER
```

### Hardware

``` text
FLASHLIGHT
```

### Display

``` text
AUTO_ROTATE
MAX_BRIGHTNESS
```

### Synchronization

``` text
TOGGLE_SYNC
```

### Connectivity

``` text
WI_FI
BLUETOOTH
LOCATION
AIRPLANE_MODE
MOBILE_DATA
```

### UI actions

``` text
MAIN
NONE
```

------------------------------------------------------------------------

# 15. TouchAccessibilityService.kt

Package:

``` text
com.bracketzone.touchassistant.service
```

Class:

``` kotlin
class TouchAccessibilityService : AccessibilityService()
```

It does not inspect screen contents.

It exists primarily to perform Android global accessibility actions.

A weak reference singleton is used:

``` kotlin
private var instance:
    WeakReference<TouchAccessibilityService>? = null
```

When connected:

``` kotlin
instance = WeakReference(this)
```

When destroyed, the reference is cleared.

------------------------------------------------------------------------

## 16. Accessibility Action Mapping

Current mapping is correct:

``` text
HOME
→ GLOBAL_ACTION_HOME

BACK
→ GLOBAL_ACTION_BACK

RECENTS
→ GLOBAL_ACTION_RECENTS

NOTIFICATIONS
→ GLOBAL_ACTION_NOTIFICATIONS

QUICK_SETTINGS
→ GLOBAL_ACTION_QUICK_SETTINGS

POWER
→ GLOBAL_ACTION_POWER_DIALOG
```

Split screen:

``` text
GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
```

for Android N/API 24+.

Lock:

``` text
GLOBAL_ACTION_LOCK_SCREEN
```

for Android P/API 28+.

Screenshot:

``` text
GLOBAL_ACTION_TAKE_SCREENSHOT
```

Current code checks API R/API 30+ for screenshot.

------------------------------------------------------------------------

# 17. ActionExecutor.kt

Package:

``` text
com.bracketzone.touchassistant.core
```

Class:

``` kotlin
class ActionExecutor(
    private val context: Context
)
```

It first checks whether the requested action is an Accessibility Service
action.

If so:

``` text
TouchAccessibilityService.getInstance()
```

is used.

If the service is unavailable, a Toast asks the user to enable the
Accessibility Service.

------------------------------------------------------------------------

# 18. Audio Actions

Implemented using `AudioManager`.

### Volume up

``` kotlin
audioManager.adjustVolume(
    AudioManager.ADJUST_RAISE,
    AudioManager.FLAG_SHOW_UI
)
```

### Volume down

Uses:

``` text
ADJUST_LOWER
```

### Mute/unmute

Uses:

``` text
adjustStreamVolume()
ADJUST_TOGGLE_MUTE
```

with a legacy implementation for older Android versions.

### Ringer

Toggles between normal and silent/normal states.

------------------------------------------------------------------------

# 19. Flashlight

Uses:

``` text
CameraManager
```

The implementation: - Finds a camera with flash support. - Reads the
current flashlight state from SharedPreferences. - Calls
`setTorchMode()`. - Saves the new flashlight state.

Flashlight preference storage:

``` text
touch_assistant_hardware
```

Key:

``` text
flashlight_enabled
```

`CAMERA` permission is required.

------------------------------------------------------------------------

# 20. Display Actions

### Auto rotate

Checks:

``` kotlin
Settings.System.canWrite(context)
```

If permission is unavailable, it opens:

``` text
ACTION_MANAGE_WRITE_SETTINGS
```

Otherwise it toggles:

``` text
Settings.System.ACCELEROMETER_ROTATION
```

### Maximum brightness

Also checks `canWrite`.

Then sets:

``` text
Settings.System.SCREEN_BRIGHTNESS = 255
```

------------------------------------------------------------------------

# 21. Master Sync

Uses:

``` kotlin
ContentResolver.getMasterSyncAutomatically()
ContentResolver.setMasterSyncAutomatically(...)
```

The implementation is marked deprecated because this API is deprecated
on newer Android versions.

This feature may require additional consideration for modern Android
behavior and device restrictions.

------------------------------------------------------------------------

# 22. Connectivity Actions

These actions open Android settings screens.

### Wi-Fi

``` text
Settings.ACTION_WIFI_SETTINGS
```

### Bluetooth

``` text
Settings.ACTION_BLUETOOTH_SETTINGS
```

### Location

``` text
Settings.ACTION_LOCATION_SOURCE_SETTINGS
```

### Airplane mode

``` text
Settings.ACTION_AIRPLANE_MODE_SETTINGS
```

### Mobile data

Uses:

``` text
Settings.ACTION_NETWORK_OPERATOR_SETTINGS
```

with fallback to:

``` text
Settings.ACTION_WIRELESS_SETTINGS
```

------------------------------------------------------------------------

# 23. AppLauncher.kt --- Planned

Purpose:

Launch installed applications without unnecessarily creating a new
instance.

Use:

``` kotlin
packageManager.getLaunchIntentForPackage(packageName)
```

with flags:

``` kotlin
Intent.FLAG_ACTIVITY_NEW_TASK
Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
Intent.FLAG_ACTIVITY_SINGLE_TOP
```

Goal:

``` text
Existing app instance
        ↓
Bring it to foreground
        ↓
Avoid unnecessary lifecycle recreation
```

------------------------------------------------------------------------

# 24. RelativeMenuManager.kt --- Planned

The main menu must NOT be centered on screen.

It must appear anchored to the current floating button position.

### If icon is on left half:

``` text
[ICON] [MENU]
```

Menu opens to the right:

``` text
iconX + iconWidth + margin
```

### If icon is on right half:

``` text
[MENU] [ICON]
```

Menu opens to the left:

``` text
iconX - menuWidth - margin
```

### Vertical positioning

The menu's vertical center should align with the icon's center.

Then clamp the menu position using `coerceIn()` to prevent it from going
off-screen.

### Outside touch

Menu window should use:

``` text
FLAG_WATCH_OUTSIDE_TOUCH
```

so touching outside dismisses the menu.

------------------------------------------------------------------------

# 25. Planned Main Gesture Behavior

After the gesture system is fixed:

``` text
Single tap
    ↓
GesturePreferences
    ↓
MAIN
    ↓
showMainMenu()
    ↓
RelativeMenuManager
    ↓
Anchored menu beside floating button
```

Double tap:

``` text
SCREENSHOT
```

Long press:

``` text
NOTIFICATIONS
```

These are the current default mappings.

------------------------------------------------------------------------

# 26. Theme and Transparency --- Planned

The floating button should read its configured alpha from:

``` kotlin
GesturePreferences.getBaseAlpha()
```

Allowed range:

``` text
0.1f – 1.0f
```

On wake, restore the configured alpha.

Light/dark mode should be detected using:

``` kotlin
Configuration.UI_MODE_NIGHT_MASK
```

and used to select appropriate popup/icon colors.

------------------------------------------------------------------------

# 27. Material You Dynamic Colors --- Planned

For Android 12/API 31+:

Use system dynamic colors such as:

``` text
android.R.color.system_accent1_500
android.R.color.system_neutral1_500
```

with:

``` kotlin
ContextCompat.getColor()
```

These should style floating UI elements to match the device
wallpaper/system theme.

------------------------------------------------------------------------

# 28. Current FloatingWidgetService Structure

Current major responsibilities:

``` text
FloatingWidgetService
├── WindowManager
├── Floating ImageView
├── Dragging
├── Edge snapping
├── Idle auto-hide
├── GestureDetector
├── GesturePreferences
├── Gesture action dispatch
└── Foreground notification
```

Current imports include Android animation, WindowManager,
GestureDetector, MotionEvent, ImageView, Toast, Handler, and related
classes.

------------------------------------------------------------------------

# 29. Current Critical Debugging Status

The most recent issue was:

> All gestures appeared to register as long press.

Root cause found:

> There were two `ACTION_UP` branches in `handleTouch()`.

The first branch returned before:

``` kotlin
gestureDetector.onTouchEvent(event)
```

could receive `ACTION_UP`.

The fix is to keep only one `ACTION_UP` branch and make sure it sends
the event to `GestureDetector` before returning.

------------------------------------------------------------------------

# 30. Immediate Next Step

Do NOT continue to `RelativeMenuManager` yet.

First:

1.  Replace `handleTouch()` with the corrected version.
2.  Build the app.
3.  Test single tap.
4.  Test double tap.
5.  Test long press.
6.  Test drag.
7.  Confirm the Toasts are correctly differentiated.
8.  Only then restore `executeGestureAction()`.

Expected:

``` text
Single tap  → SINGLE TAP
Double tap  → DOUBLE TAP
Long press  → LONG PRESS
Drag        → moves/snaps, no gesture action
```

After that, reconnect:

``` text
Single tap  → MAIN
Double tap  → SCREENSHOT
Long press  → NOTIFICATIONS
```

Then proceed to:

``` text
RelativeMenuManager.kt
```

for the anchored floating menu.

------------------------------------------------------------------------

# 31. Important Existing Files

Current/expected source structure:

``` text
app/
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── bracketzone/
        │           └── touchassistant/
        │               ├── core/
        │               │   ├── AssistantAction.kt
        │               │   └── ActionExecutor.kt
        │               │
        │               ├── preferences/
        │               │   └── GesturePreferences.kt
        │               │
        │               └── service/
        │                   ├── FloatingWidgetService.kt
        │                   └── TouchAccessibilityService.kt
        │
        └── res/
            └── xml/
                └── accessibility_service_config.xml
```

Planned:

``` text
core/
└── AppLauncher.kt

service/
└── RelativeMenuManager.kt
```

------------------------------------------------------------------------

# 32. Production Architecture

Final intended flow:

``` text
                        Android System
                              │
             ┌────────────────┴────────────────┐
             │                                 │
             ▼                                 ▼
 FloatingWidgetService              TouchAccessibilityService
             │                                 │
             │                                 │
       GestureDetector                         │
             │                                 │
             ▼                                 │
    GesturePreferences                         │
             │                                 │
             ▼                                 │
      AssistantAction                          │
             │                                 │
             ▼                                 │
      ActionExecutor ──────────────────────────┘
             │
      ┌──────┴────────┐
      │               │
      ▼               ▼
System Services    Settings
      │               │
      ├─ Audio        ├─ Wi-Fi
      ├─ Camera       ├─ Bluetooth
      ├─ Display      ├─ Location
      └─ Sync         ├─ Airplane
                      └─ Mobile Data
```

For the main menu:

``` text
Floating button
      ↓
MAIN
      ↓
RelativeMenuManager
      ↓
Menu anchored beside button
      ↓
AssistantAction
      ↓
ActionExecutor / AppLauncher
```

------------------------------------------------------------------------

# 33. Development Rule Going Forward

The app is being built incrementally.

Do not replace unrelated working code when debugging one component.

Preferred order:

``` text
1. Gesture recognition
2. Gesture → AssistantAction mapping
3. Relative menu
4. Accessibility actions
5. Hardware/system actions
6. App launcher
7. Theme/dynamic colors
8. Settings/customization UI
9. Final cleanup
10. R8/release optimization
```

At each stage: - Build. - Install. - Test. - Fix errors before moving to
the next component.

This prevents a working overlay/drag system from being broken while
adding new features.
