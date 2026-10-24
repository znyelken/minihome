package com.zeynep.minilauncher

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    // Dock order. Apps that are not installed are skipped.
    private val favs = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.spotify.tv.android",
        "com.wbd.stream",
        "nl.pathe.thuis",
        "com.digiturk.iq.mobil",
        "com.valvesoftware.steamlink"
    )

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bgA: ImageView
    private lateinit var bgB: ImageView
    private lateinit var dock: LinearLayout
    private lateinit var scroll: HorizontalScrollView
    private lateinit var credit: TextView
    private var front = true
    private var files: List<File> = emptyList()
    private var idx = 0
    private var allDialog: Dialog? = null

    private val rotate = object : Runnable {
        override fun run() {
            nextWallpaper()
            handler.postDelayed(this, 60_000)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0B1F2A.toInt(), 0xFF1D4E5F.toInt())
        )

        bgA = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        bgB = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; alpha = 0f }
        root.addView(bgA, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(bgB, FrameLayout.LayoutParams(MATCH, MATCH))

        val scrim = View(this)
        scrim.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x00000000, 0xAA000000.toInt())
        )
        root.addView(scrim, FrameLayout.LayoutParams(MATCH, dp(360), Gravity.BOTTOM))

        credit = TextView(this).apply { textSize = 11f; setTextColor(0x99FFFFFF.toInt()); maxLines = 1 }
        root.addView(credit, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = dp(36); bottomMargin = dp(14)
        })

        // Clock + date, top right
        val clock = TextClock(this).apply {
            format12Hour = "HH:mm"; format24Hour = "HH:mm"
            textSize = 44f; setTextColor(Color.WHITE); gravity = Gravity.END
        }
        val date = TextClock(this).apply {
            format12Hour = "EEEE d MMMM"; format24Hour = "EEEE d MMMM"
            textSize = 18f; setTextColor(0xCCFFFFFF.toInt()); gravity = Gravity.END
        }
        date.setSingleLine(true)
        val clockBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(clock, LinearLayout.LayoutParams(WRAP, WRAP))
            addView(date, LinearLayout.LayoutParams(WRAP, WRAP))
        }
        root.addView(clockBox, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(40); rightMargin = dp(64)
        })

        // Dock
        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(22))
            clipChildren = false
            clipToPadding = false
            background = GradientDrawable().apply {
                setColor(0x66101014)
                cornerRadius = dp(40).toFloat()
            }
        }
        buildDock()
        scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(dock, FrameLayout.LayoutParams(WRAP, WRAP))
        }
        root.addView(scroll, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(48); leftMargin = dp(32); rightMargin = dp(32)
        })

        setContentView(root)
    }

    private fun buildDock() {
        val pm = packageManager
        for (p in favs) {
            val intent = pm.getLeanbackLaunchIntentForPackage(p) ?: pm.getLaunchIntentForPackage(p) ?: continue
            val label = try { pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString() } catch (e: Exception) { p }
            val icon: Drawable? = try { pm.getApplicationIcon(p) } catch (e: Exception) { null }
            dock.addView(tile(label, icon, null) { startActivity(intent) })
        }
        dock.addView(tile("All apps", null, "\u22EF") { allApps() })
        dock.addView(tile("Settings", null, "\u2699") {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
    }

    private fun tile(label: String, icon: Drawable?, glyph: String?, go: () -> Unit): View {
        val size = dp(66)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isFocusable = true
            isClickable = true
            clipChildren = false
        }
        val img: View = if (icon != null) {
            ImageView(this).apply {
                setImageDrawable(icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(0x22FFFFFF)
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, o: Outline) {
                        o.setRoundRect(0, 0, v.width, v.height, dp(18).toFloat())
                    }
                }
                clipToOutline = true
            }
        } else {
            TextView(this).apply {
                text = glyph; textSize = 34f; gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat(); setColor(0x55FFFFFF)
                }
            }
        }
        box.addView(img, LinearLayout.LayoutParams(size, size))
        val name = TextView(this).apply {
            text = label; textSize = 12f; setTextColor(Color.WHITE)
            maxLines = 1; gravity = Gravity.CENTER; alpha = 0f
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, 0)
        }
        box.addView(name, LinearLayout.LayoutParams(size, WRAP))
        box.setOnFocusChangeListener { v, focused ->
            val s = if (focused) 1.12f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            name.animate().alpha(if (focused) 1f else 0f).setDuration(120).start()
        }
        box.setOnClickListener { go() }
        box.layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply {
            leftMargin = dp(4); rightMargin = dp(4)
        }
        return box
    }

    private fun allApps() {
        val pm = packageManager
        val lean = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val norm = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = (pm.queryIntentActivities(lean, 0) + pm.queryIntentActivities(norm, 0))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val grid = GridView(this)
        grid.numColumns = 6
        grid.verticalSpacing = dp(16)
        grid.horizontalSpacing = dp(16)
        grid.setPadding(dp(48), dp(48), dp(48), dp(48))
        grid.setSelector(GradientDrawable().apply { setColor(0x44FFFFFF); cornerRadius = dp(16).toFloat() })
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(position: Int) = apps[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val ri = apps[position]
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                }
                val iv = ImageView(this@MainActivity).apply { setImageDrawable(ri.loadIcon(pm)) }
                row.addView(iv, LinearLayout.LayoutParams(dp(72), dp(72)))
                row.addView(TextView(this@MainActivity).apply {
                    text = ri.loadLabel(pm); setTextColor(Color.WHITE)
                    textSize = 14f; maxLines = 1; gravity = Gravity.CENTER
                })
                return row
            }
        }
        val dlg = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        grid.setOnItemClickListener { _, _, p, _ ->
            val ai = apps[p].activityInfo
            startActivity(Intent(Intent.ACTION_MAIN).setClassName(ai.packageName, ai.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            dlg.dismiss()
        }
        dlg.setContentView(grid)
        dlg.window?.setBackgroundDrawable(ColorDrawable(0xEE0E1116.toInt()))
        dlg.window?.setLayout(MATCH, MATCH)
        allDialog = dlg
        dlg.show()
    }

    // ---- wallpapers: JPG/PNG/WEBP in /sdcard/Android/data/com.zeynep.minilauncher/files/wallpapers ----
    private fun loadFiles() {
        val dir = getExternalFilesDir("wallpapers")
        files = dir?.listFiles { f -> f.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            ?.toList()?.shuffled() ?: emptyList()
        idx = 0
    }

    private fun decode(f: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var s = 1
        while (bounds.outWidth / (s * 2) >= 1920) s *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = s }
        return BitmapFactory.decodeFile(f.path, opts)
    }

    private fun nextWallpaper() {
        if (files.isEmpty()) return
        val f = files[idx++ % files.size]
        Thread {
            val bmp = decode(f)
            val t = File(f.path.removeSuffix("." + f.extension) + ".txt")
            val text = if (t.exists()) t.readText() else ""
            if (bmp != null) runOnUiThread { crossfade(bmp, text) }
        }.start()
    }

    private fun crossfade(bmp: Bitmap, text: String) {
        credit.text = text
        val inV = if (front) bgB else bgA
        val outV = if (front) bgA else bgB
        inV.setImageBitmap(bmp)
        inV.animate().alpha(1f).setDuration(1500).start()
        outV.animate().alpha(0f).setDuration(1500).withEndAction { outV.setImageDrawable(null) }.start()
        front = !front
    }

    // ---- online wallpapers: Wikimedia Commons "Featured pictures" (no account/key, ~once a day) ----
    private val ua = "MiniHome/1.0 (personal-use TV launcher; https://github.com/znyelken/minihome)"
    private val sources = listOf(
        "Featured pictures of landscapes" to 468,
        "Featured pictures of mountains" to 137,
        "Featured pictures of coasts" to 54
    )

    private fun httpGet(u: String): ByteArray {
        val c = java.net.URL(u).openConnection() as java.net.HttpURLConnection
        c.setRequestProperty("User-Agent", ua)
        c.connectTimeout = 15000
        c.readTimeout = 30000
        try { return c.inputStream.use { it.readBytes() } } finally { c.disconnect() }
    }

    private fun refreshOnline() {
        val dir = getExternalFilesDir("wallpapers") ?: return
        if (File(dir, "offline.flag").exists()) return
        val prefs = getSharedPreferences("mh", MODE_PRIVATE)
        val have = dir.listFiles { f -> f.name.startsWith("wm_") && f.extension == "jpg" }?.size ?: 0
        val due = System.currentTimeMillis() - prefs.getLong("last_fetch", 0) > 20L * 3600 * 1000
        if (!due && have >= 6) return
        Thread {
            try {
                val (cat, count) = sources.random()
                val offset = (0..(count / 50)).random() * 50
                val q = java.net.URLEncoder.encode("incategory:\"$cat\"", "UTF-8")
                val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
                    "&generator=search&gsrnamespace=6&gsrlimit=50&gsroffset=$offset&gsrsearch=$q" +
                    "&prop=imageinfo&iiprop=url|size|mime&iiurlwidth=1920"
                val json = org.json.JSONObject(String(httpGet(url), Charsets.UTF_8))
                val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return@Thread
                var got = 0
                for (k in pages.keys().asSequence().toList().shuffled()) {
                    if (got >= 8) break
                    val p = pages.getJSONObject(k)
                    val ii = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                    if (ii.optString("mime") != "image/jpeg") continue
                    val w = ii.optInt("width"); val h = ii.optInt("height")
                    if (h == 0 || w.toFloat() / h < 1.4f || w.toFloat() / h > 2.6f) continue
                    val thumb = ii.optString("thumburl")
                    if (thumb.isEmpty()) continue
                    val id = p.optInt("pageid")
                    val out = File(dir, "wm_$id.jpg")
                    if (out.exists()) continue
                    out.writeBytes(httpGet(thumb))
                    File(dir, "wm_$id.txt").writeText(
                        p.optString("title").removePrefix("File:").substringBeforeLast('.') +
                            " \u00B7 Wikimedia Commons")
                    got++
                }
                val all = dir.listFiles { f -> f.name.startsWith("wm_") && f.extension == "jpg" }
                    ?.sortedBy { it.lastModified() } ?: emptyList()
                all.dropLast(24).forEach {
                    it.delete(); File(it.path.removeSuffix(".jpg") + ".txt").delete()
                }
                if (got > 0) {
                    prefs.edit().putLong("last_fetch", System.currentTimeMillis()).apply()
                    runOnUiThread { loadFiles(); nextWallpaper() }
                }
            } catch (e: Exception) {
                // offline or API hiccup: keep using the photos we already have
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        loadFiles()
        refreshOnline()
        handler.removeCallbacks(rotate)
        handler.post(rotate)
        dock.post { if (currentFocus == null) dock.getChildAt(0)?.requestFocus() }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(rotate)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        allDialog?.dismiss()
        scroll.smoothScrollTo(0, 0)
        dock.getChildAt(0)?.requestFocus()
    }

    override fun onBackPressed() { /* this is the home screen: Back does nothing */ }
}
