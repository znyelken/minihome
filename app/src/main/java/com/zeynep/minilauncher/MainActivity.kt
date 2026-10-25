package com.zeynep.minilauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
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
import android.widget.Toast
import java.io.File

/** Simple vector-ish icons drawn in code (no font symbols, no image files). */
private class Glyph(ctx: Context, var kind: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2; val cy = h / 2; val s = minOf(w, h)
        p.style = Paint.Style.FILL
        when (kind) {
            0 -> { // all apps: 2x2 squares
                val a = s * 0.2f; val g = s * 0.08f
                for (i in 0..1) for (j in 0..1) {
                    val x = if (i == 0) cx - a - g / 2 else cx + g / 2
                    val y = if (j == 0) cy - a - g / 2 else cy + g / 2
                    c.drawRoundRect(x, y, x + a, y + a, a * 0.25f, a * 0.25f, p)
                }
            }
            1 -> { // settings: gear
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.1f
                c.drawCircle(cx, cy, s * 0.17f, p)
                p.style = Paint.Style.FILL
                for (i in 0 until 8) {
                    c.save(); c.rotate(i * 45f, cx, cy)
                    c.drawRoundRect(cx - s * 0.05f, cy - s * 0.33f, cx + s * 0.05f, cy - s * 0.2f, s * 0.02f, s * 0.02f, p)
                    c.restore()
                }
            }
            2 -> { // previous
                c.drawRect(cx - s * 0.22f, cy - s * 0.2f, cx - s * 0.16f, cy + s * 0.2f, p)
                val t = Path().apply { moveTo(cx + s * 0.22f, cy - s * 0.2f); lineTo(cx + s * 0.22f, cy + s * 0.2f); lineTo(cx - s * 0.12f, cy); close() }
                c.drawPath(t, p)
            }
            3 -> { // play
                val t = Path().apply { moveTo(cx - s * 0.14f, cy - s * 0.22f); lineTo(cx - s * 0.14f, cy + s * 0.22f); lineTo(cx + s * 0.22f, cy); close() }
                c.drawPath(t, p)
            }
            4 -> { // pause
                c.drawRoundRect(cx - s * 0.18f, cy - s * 0.2f, cx - s * 0.06f, cy + s * 0.2f, s * 0.02f, s * 0.02f, p)
                c.drawRoundRect(cx + s * 0.06f, cy - s * 0.2f, cx + s * 0.18f, cy + s * 0.2f, s * 0.02f, s * 0.02f, p)
            }
            6 -> { // search
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.075f; p.strokeCap = Paint.Cap.ROUND
                c.drawCircle(cx - s * 0.04f, cy - s * 0.04f, s * 0.17f, p)
                c.drawLine(cx + s * 0.09f, cy + s * 0.09f, cx + s * 0.24f, cy + s * 0.24f, p)
            }
            7 -> { // home
                val t = Path().apply {
                    moveTo(cx, cy - s * 0.26f); lineTo(cx + s * 0.28f, cy - s * 0.02f); lineTo(cx + s * 0.2f, cy - s * 0.02f)
                    lineTo(cx + s * 0.2f, cy + s * 0.24f); lineTo(cx - s * 0.2f, cy + s * 0.24f); lineTo(cx - s * 0.2f, cy - s * 0.02f)
                    lineTo(cx - s * 0.28f, cy - s * 0.02f); close()
                }
                c.drawPath(t, p)
            }
            8 -> { // for you: four-point sparkle
                val t = Path().apply {
                    moveTo(cx, cy - s * 0.28f); lineTo(cx + s * 0.07f, cy - s * 0.07f); lineTo(cx + s * 0.28f, cy)
                    lineTo(cx + s * 0.07f, cy + s * 0.07f); lineTo(cx, cy + s * 0.28f); lineTo(cx - s * 0.07f, cy + s * 0.07f)
                    lineTo(cx - s * 0.28f, cy); lineTo(cx - s * 0.07f, cy - s * 0.07f); close()
                }
                c.drawPath(t, p)
            }
            9 -> { // inputs: little TV
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.07f; p.strokeCap = Paint.Cap.ROUND
                c.drawRoundRect(cx - s * 0.26f, cy - s * 0.2f, cx + s * 0.26f, cy + s * 0.1f, s * 0.04f, s * 0.04f, p)
                c.drawLine(cx - s * 0.12f, cy + s * 0.22f, cx + s * 0.12f, cy + s * 0.22f, p)
            }
            10 -> { // account: person
                c.drawCircle(cx, cy - s * 0.1f, s * 0.1f, p)
                c.drawArc(RectF(cx - s * 0.2f, cy + s * 0.04f, cx + s * 0.2f, cy + s * 0.44f), 180f, 180f, true, p)
            }
            11 -> { // switch: two opposite arrows
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.07f; p.strokeCap = Paint.Cap.ROUND
                c.drawLine(cx - s * 0.22f, cy - s * 0.1f, cx + s * 0.1f, cy - s * 0.1f, p)
                c.drawLine(cx + s * 0.22f, cy + s * 0.1f, cx - s * 0.1f, cy + s * 0.1f, p)
                p.style = Paint.Style.FILL
                c.drawPath(Path().apply {
                    moveTo(cx + s * 0.27f, cy - s * 0.1f); lineTo(cx + s * 0.13f, cy - s * 0.21f); lineTo(cx + s * 0.13f, cy + s * 0.01f); close()
                }, p)
                c.drawPath(Path().apply {
                    moveTo(cx - s * 0.27f, cy + s * 0.1f); lineTo(cx - s * 0.13f, cy - s * 0.01f); lineTo(cx - s * 0.13f, cy + s * 0.21f); close()
                }, p)
            }
            5 -> { // next
                c.drawRect(cx + s * 0.16f, cy - s * 0.2f, cx + s * 0.22f, cy + s * 0.2f, p)
                val t = Path().apply { moveTo(cx - s * 0.22f, cy - s * 0.2f); lineTo(cx - s * 0.22f, cy + s * 0.2f); lineTo(cx + s * 0.12f, cy); close() }
                c.drawPath(t, p)
            }
        }
    }
}

class MainActivity : Activity() {

    // Default dock order. You can change it on the TV (hold OK on an icon); this is only the starting set.
    private val defaultFavs = listOf(
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
    private val spotifyPkgs = setOf("com.spotify.tv.android", "com.spotify.music")

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("mh", MODE_PRIVATE) }

    private lateinit var bgA: ImageView
    private lateinit var bgB: ImageView
    private lateinit var dock: LinearLayout
    private lateinit var scroll: HorizontalScrollView
    private lateinit var credit: TextView
    private var front = true
    private var files: List<File> = emptyList()
    private var idx = 0
    private var allDialog: Dialog? = null
    private var moving: View? = null

    // dock tile size in px, computed so every tile fits on screen
    private var iconPx = 0
    private var routed = false
    private lateinit var homeBg: GradientDrawable

    // now playing
    private lateinit var nowCard: LinearLayout
    private lateinit var nowArt: ImageView
    private lateinit var nowTitle: TextView
    private lateinit var nowArtist: TextView
    private lateinit var progFill: View
    private lateinit var progRest: View
    private lateinit var playGlyph: Glyph
    private var controller: MediaController? = null
    private var mediaMgr: MediaSessionManager? = null
    private var playing = false

    private val rotate = object : Runnable {
        override fun run() {
            nextWallpaper()
            handler.postDelayed(this, 60_000)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundOutline(r: Int) = object : ViewOutlineProvider() {
        override fun getOutline(v: View, o: Outline) {
            o.setRoundRect(0, 0, v.width, v.height, dp(r).toFloat())
        }
    }

    // ---------------------------------------------------------------- saved dock order
    private fun favList(): MutableList<String> {
        val s = prefs.getString("dock", null)
        return if (s == null) defaultFavs.toMutableList()
        else s.split(",").filter { it.isNotEmpty() }.toMutableList()
    }

    private fun saveFavs(l: List<String>) {
        prefs.edit().putString("dock", l.joinToString(",")).apply()
    }

    // ---------------------------------------------------------------- UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (routeIfClassic(intent)) { routed = true; finish(); return }

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
            intArrayOf(0x00000000, 0xCC000000.toInt())
        )
        root.addView(scrim, FrameLayout.LayoutParams(MATCH, dp(420), Gravity.BOTTOM))

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

        // Bottom stack: now-playing card above the dock
        nowCard = buildNowCard()
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
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(nowCard, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(12) })
            addView(scroll, LinearLayout.LayoutParams(WRAP, WRAP))
        }
        root.addView(bottom, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(40); leftMargin = dp(120); rightMargin = dp(32)
        })

        // Left rail
        val acc = railItem(10, "Account") { openAccounts() }
        val srch = railItem(6, "Search") { openSearch() }
        val home = railItem(7, "Home") { dock.getChildAt(0)?.requestFocus() }
        val inp = railItem(9, "Inputs") { showInputs() }
        val cls = railItem(11, "Classic") { switchToClassic() }
        val set = railItem(1, "Settings") { openSettings() }
        homeBg = home.second
        homeBg.setColor(0xFF1FBAA6.toInt())
        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(12), dp(8), dp(12))
            clipChildren = false
            background = GradientDrawable().apply { setColor(0x66101014); cornerRadius = dp(34).toFloat() }
            addView(acc.first); addView(srch.first); addView(home.first)
            addView(inp.first); addView(cls.first); addView(set.first)
        }
        root.addView(rail, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            leftMargin = dp(24)
        })

        setContentView(root)
    }

    private fun buildDock(focusIdx: Int = -1) {
        dock.removeAllViews()
        val pm = packageManager
        val shown = favList().filter {
            pm.getLeanbackLaunchIntentForPackage(it) != null || pm.getLaunchIntentForPackage(it) != null
        }
        val availPx = resources.displayMetrics.widthPixels - dp(120) - dp(32) - dp(48)
        val per = minOf(dp(84), availPx / (shown.size + 1))
        iconPx = maxOf(dp(40), per - dp(12))
        for (p in shown) {
            val intent = pm.getLeanbackLaunchIntentForPackage(p) ?: pm.getLaunchIntentForPackage(p) ?: continue
            val label = try { pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString() } catch (e: Exception) { p }
            val icon: Drawable? = try { pm.getApplicationIcon(p) } catch (e: Exception) { null }
            val t = tile(label, icon, null) { startActivity(intent) }
            t.tag = p
            t.setOnLongClickListener { editMenu(p, label); true }
            dock.addView(t)
        }
        val all = tile("All apps", null, 0) { allApps(null) }
        all.setOnLongClickListener { addPicker(); true }
        dock.addView(all)
        if (focusIdx >= 0) dock.post {
            dock.getChildAt(minOf(focusIdx, dock.childCount - 1))?.requestFocus()
        }
    }

    private fun tile(label: String, icon: Drawable?, glyphKind: Int?, go: () -> Unit): LinearLayout {
        val size = iconPx
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isFocusable = true
            isClickable = true
            isLongClickable = true
            clipChildren = false
        }
        val ringDrawable = GradientDrawable().apply {
            cornerRadius = dp(21).toFloat(); setStroke(dp(3), Color.TRANSPARENT)
        }
        val ring = FrameLayout(this).apply {
            background = ringDrawable
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        val img: View = if (icon != null) {
            ImageView(this).apply {
                setImageDrawable(icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(0x22FFFFFF)
                outlineProvider = roundOutline(18)
                clipToOutline = true
            }
        } else {
            FrameLayout(this).apply {
                background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(0x55FFFFFF) }
                setPadding(dp(8), dp(8), dp(8), dp(8))
                addView(Glyph(this@MainActivity, glyphKind ?: 0), FrameLayout.LayoutParams(MATCH, MATCH))
            }
        }
        ring.addView(img, FrameLayout.LayoutParams(size, size))
        box.addView(ring, LinearLayout.LayoutParams(size + dp(6), size + dp(6)))
        val name = TextView(this).apply {
            text = label; textSize = 12f; setTextColor(Color.WHITE)
            maxLines = 1; gravity = Gravity.CENTER; alpha = 0f
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, 0)
        }
        box.addView(name, LinearLayout.LayoutParams(size + dp(6), WRAP))
        box.setOnFocusChangeListener { v, focused ->
            val s = if (focused) 1.12f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            name.animate().alpha(if (focused) 1f else 0f).setDuration(120).start()
            ringDrawable.setStroke(dp(3), if (focused) Color.WHITE else Color.TRANSPARENT)
        }
        box.setOnClickListener { go() }
        box.layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply {
            leftMargin = dp(3); rightMargin = dp(3)
        }
        return box
    }

    // ---------------------------------------------------------------- edit dock (hold OK)
    private fun editMenu(pkg: String, label: String) {
        val items = arrayOf("Move", "Remove from dock", "Add an app\u2026", "Reset dock to default")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startMove(pkg)
                    1 -> removeFav(pkg)
                    2 -> addPicker()
                    3 -> { prefs.edit().remove("dock").apply(); buildDock(0) }
                }
            }.show()
    }

    private fun startMove(pkg: String) {
        for (i in 0 until dock.childCount) {
            val v = dock.getChildAt(i)
            if (v.tag == pkg) { moving = v; v.requestFocus() }
        }
        Toast.makeText(this, "Move with \u2190 \u2192, press OK to drop", Toast.LENGTH_LONG).show()
    }

    private fun shift(v: View, d: Int) {
        val i = dock.indexOfChild(v)
        val j = i + d
        var userCount = 0
        for (k in 0 until dock.childCount) if (dock.getChildAt(k).tag != null) userCount++
        if (j < 0 || j >= userCount) return
        dock.removeViewAt(i)
        dock.addView(v, j)
        v.requestFocus()
    }

    private fun endMove() {
        moving = null
        val order = ArrayList<String>()
        for (k in 0 until dock.childCount) (dock.getChildAt(k).tag as? String)?.let { order.add(it) }
        saveFavs(order)
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val m = moving
        if (m != null) {
            if (e.action == KeyEvent.ACTION_DOWN) {
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> shift(m, -1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> shift(m, 1)
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> endMove()
                }
            }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun removeFav(pkg: String) {
        val l = favList()
        val i = l.indexOf(pkg)
        l.remove(pkg)
        saveFavs(l)
        buildDock(i)
    }

    private fun addPicker() {
        allApps { pkg ->
            val l = favList()
            if (pkg !in l) l.add(pkg)
            saveFavs(l)
            buildDock(l.size - 1)
        }
    }

    // ---------------------------------------------------------------- all apps / picker
    private fun allApps(pick: ((String) -> Unit)?) {
        val pm = packageManager
        val lean = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val norm = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val inDock = favList()
        val apps = (pm.queryIntentActivities(lean, 0) + pm.queryIntentActivities(norm, 0))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .filter { pick == null || it.activityInfo.packageName !in inDock }
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
            if (pick != null) {
                pick(ai.packageName)
            } else {
                startActivity(Intent(Intent.ACTION_MAIN).setClassName(ai.packageName, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            dlg.dismiss()
        }
        dlg.setContentView(grid)
        dlg.window?.setBackgroundDrawable(ColorDrawable(0xEE0E1116.toInt()))
        dlg.window?.setLayout(MATCH, MATCH)
        allDialog = dlg
        if (pick != null) Toast.makeText(this, "Choose an app to add to the dock", Toast.LENGTH_SHORT).show()
        dlg.show()
    }

    // ---------------------------------------------------------------- now playing (Spotify)
    private fun controlButton(kind: Int, act: () -> Unit): FrameLayout {
        val g = Glyph(this, kind)
        if (kind == 3) playGlyph = g
        val bg = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x00FFFFFF) }
        val b = FrameLayout(this).apply {
            isFocusable = true; isClickable = true
            background = bg
            addView(g, FrameLayout.LayoutParams(MATCH, MATCH))
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setOnClickListener { act() }
            setOnFocusChangeListener { v, f ->
                bg.setColor(if (f) 0x55FFFFFF else 0x00FFFFFF)
                val s = if (f) 1.12f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
        }
        b.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { leftMargin = dp(2); rightMargin = dp(2) }
        return b
    }

    private fun buildNowCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(18), dp(12))
            visibility = View.GONE
            background = GradientDrawable().apply { setColor(0x66101014); cornerRadius = dp(32).toFloat() }
        }
        nowArt = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            isFocusable = true; isClickable = true
            background = GradientDrawable().apply { setColor(0xFF1DB954.toInt()); cornerRadius = dp(14).toFloat() }
            outlineProvider = roundOutline(14)
            clipToOutline = true
            setOnClickListener {
                val pkg = controller?.packageName
                if (pkg != null) {
                    val i = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                        ?: packageManager.getLaunchIntentForPackage(pkg)
                    if (i != null) startActivity(i)
                }
            }
            setOnFocusChangeListener { v, f ->
                val s = if (f) 1.06f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
        }
        card.addView(nowArt, LinearLayout.LayoutParams(dp(64), dp(64)))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(8), 0)
        }
        col.addView(TextView(this).apply {
            text = "SPOTIFY"; textSize = 11f; setTextColor(0xFF1ED760.toInt()); letterSpacing = 0.15f
        })
        nowTitle = TextView(this).apply {
            textSize = 18f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            setSingleLine(true); ellipsize = TextUtils.TruncateAt.END
        }
        nowArtist = TextView(this).apply {
            textSize = 14f; setTextColor(0xB3FFFFFF.toInt())
            setSingleLine(true); ellipsize = TextUtils.TruncateAt.END
        }
        col.addView(nowTitle)
        col.addView(nowArtist)
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply { setColor(0x33FFFFFF); cornerRadius = dp(2).toFloat() }
        }
        progFill = View(this).apply {
            background = GradientDrawable().apply { setColor(0xFF1ED760.toInt()); cornerRadius = dp(2).toFloat() }
        }
        progRest = View(this)
        bar.addView(progFill, LinearLayout.LayoutParams(0, dp(4), 0f))
        bar.addView(progRest, LinearLayout.LayoutParams(0, dp(4), 1f))
        col.addView(bar, LinearLayout.LayoutParams(MATCH, dp(4)).apply { topMargin = dp(8) })
        card.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))

        card.addView(controlButton(2) { controller?.transportControls?.skipToPrevious() })
        card.addView(controlButton(3) {
            val tc = controller?.transportControls
            if (playing) tc?.pause() else tc?.play()
        })
        card.addView(controlButton(5) { controller?.transportControls?.skipToNext() })
        return card
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pickController(list)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { updateNow() }
        override fun onMetadataChanged(metadata: MediaMetadata?) { updateNow() }
        override fun onSessionDestroyed() { updateNow() }
    }

    private val tick = object : Runnable {
        override fun run() {
            val st = controller?.playbackState
            val md = controller?.metadata
            if (st != null && md != null) {
                val dur = md.getLong(MediaMetadata.METADATA_KEY_DURATION)
                var pos = st.position
                if (st.state == PlaybackState.STATE_PLAYING) {
                    pos += ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
                }
                val f = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                (progFill.layoutParams as LinearLayout.LayoutParams).weight = f
                (progRest.layoutParams as LinearLayout.LayoutParams).weight = 1f - f
                progFill.requestLayout()
                progRest.requestLayout()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun startMedia() {
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, MediaListener::class.java)
            msm.addOnActiveSessionsChangedListener(sessionsListener, cn, handler)
            mediaMgr = msm
            pickController(msm.getActiveSessions(cn))
        } catch (e: SecurityException) {
            // notification-listener access not granted yet (03-install-launcher.ps1 grants it)
            nowCard.visibility = View.GONE
        } catch (e: Exception) {
            nowCard.visibility = View.GONE
        }
    }

    private fun stopMedia() {
        try { mediaMgr?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (e: Exception) { }
        controller?.unregisterCallback(mediaCallback)
        controller = null
        handler.removeCallbacks(tick)
    }

    private fun pickController(list: List<MediaController>?) {
        val c = list?.firstOrNull { it.packageName in spotifyPkgs }
        if (c?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(mediaCallback)
            controller = c
            c?.registerCallback(mediaCallback, handler)
        }
        updateNow()
    }

    private fun updateNow() {
        val c = controller
        val st = c?.playbackState
        val md = c?.metadata
        if (c == null || st == null || md == null ||
            st.state == PlaybackState.STATE_NONE || st.state == PlaybackState.STATE_STOPPED) {
            nowCard.visibility = View.GONE
            handler.removeCallbacks(tick)
            return
        }
        nowCard.visibility = View.VISIBLE
        nowTitle.text = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        nowArtist.text = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (art != null) nowArt.setImageBitmap(art) else nowArt.setImageDrawable(null)
        playing = st.state == PlaybackState.STATE_PLAYING
        playGlyph.kind = if (playing) 4 else 3
        playGlyph.invalidate()
        handler.removeCallbacks(tick)
        tick.run()
    }

    // ---------------------------------------------------------------- wallpapers (local + online)
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

    // ---------------------------------------------------------------- left rail
    private fun railItem(kind: Int, label: String, act: () -> Unit): Pair<View, GradientDrawable> {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(0x22FFFFFF); setStroke(dp(3), Color.TRANSPARENT)
        }
        val circle = FrameLayout(this).apply {
            isFocusable = true; isClickable = true
            background = bg
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(Glyph(this@MainActivity, kind), FrameLayout.LayoutParams(MATCH, MATCH))
            setOnClickListener { act() }
            setOnFocusChangeListener { v, f ->
                bg.setStroke(dp(3), if (f) Color.WHITE else Color.TRANSPARENT)
                val s = if (f) 1.12f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            addView(circle, LinearLayout.LayoutParams(dp(52), dp(52)))
            addView(TextView(this@MainActivity).apply {
                text = label; textSize = 10f; setTextColor(0xB3FFFFFF.toInt()); gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, 0)
            }, LinearLayout.LayoutParams(WRAP, WRAP))
        }
        box.layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(4); bottomMargin = dp(4) }
        return Pair(box, bg)
    }

    private fun launchPkg(pkg: String) {
        val i = packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) startActivity(i)
    }

    // ---- Classic (original) home <-> Mini Home --------------------------------------------
    // Classic mode: pressing Home on the remote still reaches this app (it is the default home),
    // which immediately hands over to the original home screen. Opening "Mini Home" from the
    // original interface's Apps row (not via the Home key) switches back.
    private fun classicHomeIntent(): Intent? {
        val homes = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
        val pick = homes.firstOrNull { it.packageName == "com.google.android.tvlauncher" }
            ?: homes.firstOrNull { it.packageName == "com.google.android.leanbacklauncher" }
            ?: homes.firstOrNull()
            ?: return null
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setClassName(pick.packageName, pick.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun launchClassic(): Boolean {
        val i = classicHomeIntent() ?: return false
        return try { startActivity(i); true } catch (e: Exception) { false }
    }

    private fun routeIfClassic(i: Intent?): Boolean {
        val fromHomeKey = i?.hasCategory(Intent.CATEGORY_HOME) == true
        if (!fromHomeKey) {
            prefs.edit().putBoolean("classic", false).apply()   // opened from an Apps row: back to Mini Home
            return false
        }
        if (prefs.getBoolean("classic", false)) {
            if (launchClassic()) return true
            prefs.edit().putBoolean("classic", false).apply()   // original home not available: stay here
        }
        return false
    }

    private fun switchToClassic() {
        if (classicHomeIntent() == null) {
            Toast.makeText(this, "Couldn't find the original home screen", Toast.LENGTH_LONG).show()
            return
        }
        prefs.edit().putBoolean("classic", true).apply()
        if (launchClassic()) {
            Toast.makeText(this, "Classic home. Open Mini Home from Apps to switch back.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            prefs.edit().putBoolean("classic", false).apply()
            Toast.makeText(this, "Couldn't open the original home screen", Toast.LENGTH_LONG).show()
        }
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openAccounts() {
        try {
            startActivity(Intent(Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { openSettings() }
    }

    private fun openSearch() {
        try {
            startActivity(Intent("android.search.action.GLOBAL_SEARCH").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try { launchPkg("com.google.android.katniss") } catch (e2: Exception) { }
        }
    }

    private fun showInputs() {
        val tim = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        val inputs = try { tim.tvInputList.filter { it.isPassthroughInput } } catch (e: Exception) { emptyList() }
        if (inputs.isEmpty()) {
            Toast.makeText(this, "No inputs found", Toast.LENGTH_SHORT).show()
            return
        }
        val names = inputs.map {
            val base = (it.loadCustomLabel(this) ?: it.loadLabel(this)).toString()
            val off = try { tim.getInputState(it.id) == TvInputManager.INPUT_STATE_DISCONNECTED } catch (e: Exception) { false }
            if (off) "$base  \u00B7  not connected" else base
        }.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Switch input")
            .setItems(names) { _, i ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputs[i].id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't switch to that input", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    // ---------------------------------------------------------------- lifecycle
    override fun onResume() {
        super.onResume()
        if (routed) return
        loadFiles()
        refreshOnline()
        handler.removeCallbacks(rotate)
        handler.post(rotate)
        startMedia()
        dock.post { if (currentFocus == null) dock.getChildAt(0)?.requestFocus() }
    }

    override fun onPause() {
        super.onPause()
        if (routed) return
        handler.removeCallbacks(rotate)
        stopMedia()
        moving = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (routeIfClassic(intent)) { routed = true; finish(); return }
        allDialog?.dismiss()
        scroll.smoothScrollTo(0, 0)
        dock.getChildAt(0)?.requestFocus()
    }

    override fun onBackPressed() { /* this is the home screen: Back does nothing */ }
}

/**
 * Intentionally empty. Having this enabled is what lets the launcher read the active media
 * session (Spotify now-playing). It never reads notification contents.
 */
class MediaListener : NotificationListenerService()
