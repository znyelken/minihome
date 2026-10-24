package com.zeynep.minilauncher

import android.service.notification.NotificationListenerService

// Intentionally empty. Having this enabled is what lets the launcher read the active
// media session (Spotify now-playing). It never reads notification contents.
class MediaListener : NotificationListenerService()
