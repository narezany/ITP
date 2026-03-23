package cat.narezany.itp

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // ──────── TV cursor ────────
    private lateinit var cursorView: CursorView
    private var cursorX = 0f
    private var cursorY = 0f
    private val cursorHandler = Handler(Looper.getMainLooper())
    private var cursorVisible = false
    private var okLongPressPending = false

    // Smooth movement state
    private var isUpPressed = false
    private var isDownPressed = false
    private var isLeftPressed = false
    private var isRightPressed = false
    private val cursorSpeed = 24f
    private val scrollSpeed = 30

    private val cursorUpdateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!cursorVisible) return
            var moved = false
            var scrolled = false
            
            if (isUpPressed) {
                if (cursorY > 0f) { cursorY = (cursorY - cursorSpeed).coerceAtLeast(0f); moved = true }
                else { webView.scrollBy(0, -scrollSpeed); scrolled = true }
            }
            if (isDownPressed) {
                if (cursorY < cursorView.height.toFloat()) { cursorY = (cursorY + cursorSpeed).coerceAtMost(cursorView.height.toFloat()); moved = true }
                else { webView.scrollBy(0, scrollSpeed); scrolled = true }
            }
            if (isLeftPressed) {
                if (cursorX > 0f) { cursorX = (cursorX - cursorSpeed).coerceAtLeast(0f); moved = true }
                else { webView.scrollBy(-scrollSpeed, 0); scrolled = true }
            }
            if (isRightPressed) {
                if (cursorX < cursorView.width.toFloat()) { cursorX = (cursorX + cursorSpeed).coerceAtMost(cursorView.width.toFloat()); moved = true }
                else { webView.scrollBy(scrollSpeed, 0); scrolled = true }
            }
            
            if (moved || scrolled) {
                if (moved) cursorView.invalidate()
                cursorHandler.removeCallbacks(hideCursorRunnable)
                cursorHandler.postDelayed(hideCursorRunnable, 4000)
            }
            if (isUpPressed || isDownPressed || isLeftPressed || isRightPressed) {
                cursorHandler.postDelayed(this, 16)
            }
        }
    }

    private val hideCursorRunnable = Runnable {
        cursorVisible = false
        cursorView.invalidate()
    }
    private val okLongPressRunnable = Runnable {
        startActivity(Intent(this, TweaksActivity::class.java))
    }

    inner class CursorView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = 220
        }
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            alpha = 80
        }
        override fun onDraw(canvas: Canvas) {
            if (!cursorVisible) return
            canvas.drawCircle(cursorX + 2f, cursorY + 2f, 9f, shadowPaint)
            canvas.drawCircle(cursorX, cursorY, 8f, paint)
        }
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        var pinUnlocked = false
    }

    // ──────── JS: blur profanity ────────
    private val blurProfanityJs = """
(function() {
    const words = ['блять','бля','блядь','блядский','бляд','пизд','пизда','пиздец','пиздатый','пиздануть','пиздить','пизде','хуй','хуя','хуёв','хуйня','хуево','хуйло','хуевый','нахуй','похуй','ёбан','ебан','ёбаный','ебаный','ёб твою','ёбать','ебать','ебу','мудак','мудаки','мудила','сука','суки','сучка','сучары','пиздёж','пиздеж','залупа','залупон','долбоёб','долбоеб','долбаеб','ёбт','ёпт','ёбст','пидор','пидар','педик','ссать','ссанина','ссаный','говно','говна','говняный','шлюха','шлюхи','выблядок','пиздабольство'];
    const regex = new RegExp(words.join('|'), 'i');
    function applyBlur(article) {
        const t = article.querySelector('.YmQiahvA, ._2QopExez');
        if (!t) return;
        if (regex.test(t.innerText || '')) {
            t.style.filter = 'blur(6px)'; t.style.transition = 'filter 0.3s ease';
            t.style.cursor = 'pointer'; t.setAttribute('data-blurred', '1');
            if (!t.__bh) { t.__bh = function(e) { e.stopPropagation(); e.preventDefault();
                if (t.getAttribute('data-blurred')==='1') { t.style.filter='blur(0px)'; t.setAttribute('data-blurred','0'); }
                else { t.style.filter='blur(6px)'; t.setAttribute('data-blurred','1'); }
            }; t.addEventListener('click', t.__bh); }
        }
    }
    document.querySelectorAll('article.FdYjPIR3').forEach(applyBlur);
    if (!window.__itpBO) { window.__itpBO = new MutationObserver(function(ms) { ms.forEach(function(m) {
        m.addedNodes.forEach(function(n) { if(n.nodeType!==1)return;
            if(n.matches&&n.matches('article.FdYjPIR3'))applyBlur(n);
            else if(n.querySelectorAll)n.querySelectorAll('article.FdYjPIR3').forEach(applyBlur);
        }); }); }); window.__itpBO.observe(document.body,{childList:true,subtree:true}); }
})();
""".trimIndent()

    private val removeBlurJs = """
(function() { document.querySelectorAll('[data-blurred]').forEach(function(el) {
    el.style.filter=''; el.style.cursor=''; el.removeAttribute('data-blurred');
    if(el.__bh){el.removeEventListener('click',el.__bh);el.__bh=null;}
}); if(window.__itpBO){window.__itpBO.disconnect();window.__itpBO=null;} })();
""".trimIndent()

    private val removeTapHighlightJs = """
(function() { if(document.getElementById('itp-nhl'))return; var s=document.createElement('style');
s.id='itp-nhl'; s.textContent='*,*::before,*::after{-webkit-tap-highlight-color:transparent!important;}';
document.head.appendChild(s); })();
""".trimIndent()

    private val trafficSaverJs = """
(function() { if(document.getElementById('itp-ts'))return; var s=document.createElement('style');
s.id='itp-ts'; s.textContent='article.FdYjPIR3 img,article.FdYjPIR3 video{display:none!important;}';
document.head.appendChild(s); })();
""".trimIndent()

    private val removeTrafficSaverJs = """
(function(){var el=document.getElementById('itp-ts');if(el)el.remove();})();
""".trimIndent()

    // ──────── JS: image download ────────
    private val imageDownloadJs = """
(function() {
    if (window.__itpDlInit) return;
    window.__itpDlInit = true;
    var style = document.createElement('style');
    style.id = 'itp-dl-style';
    style.textContent = '.itp-dl-wrap{position:relative;display:inline-block;width:100%;}.itp-dl-btn{position:absolute;top:8px;right:8px;width:36px;height:36px;border-radius:50%;background:rgba(0,0,0,0.55);display:flex;align-items:center;justify-content:center;cursor:pointer;z-index:999;border:none;padding:0;backdrop-filter:blur(4px);}.itp-dl-btn:active{background:rgba(0,0,0,0.8);}.itp-dl-btn svg{width:20px;height:20px;fill:white;}';
    document.head.appendChild(style);
    var svgIcon = '<svg viewBox="0 0 24 24"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>';
    function addBtn(img) {
        if (!img.src || img.getAttribute('data-itp-dl')) return;
        if (img.naturalWidth < 50 && img.width < 50) return;
        img.setAttribute('data-itp-dl', '1');
        var wrap = document.createElement('span');
        wrap.className = 'itp-dl-wrap';
        img.parentNode.insertBefore(wrap, img);
        wrap.appendChild(img);
        var btn = document.createElement('button');
        btn.className = 'itp-dl-btn';
        btn.innerHTML = svgIcon;
        btn.addEventListener('click', function(e) {
            e.stopPropagation(); e.preventDefault();
            if (window.Android) window.Android.downloadImage(img.src);
        });
        wrap.appendChild(btn);
    }
    function processAll() {
        document.querySelectorAll('article img:not([data-itp-dl])').forEach(function(img) {
            if (img.complete && img.src) addBtn(img);
        });
    }
    processAll();
    setInterval(processAll, 2000);
    if (!window.__itpDlObs) {
        window.__itpDlObs = new MutationObserver(function() { setTimeout(processAll, 500); });
        window.__itpDlObs.observe(document.body, {childList:true, subtree:true});
    }
})();
""".trimIndent()

    private val removeImageDownloadJs = """
(function(){
    document.querySelectorAll('.itp-dl-wrap').forEach(function(w){
        var img = w.querySelector('img');
        if(img){ img.removeAttribute('data-itp-dl'); w.parentNode.insertBefore(img, w); }
        w.remove();
    });
    document.querySelectorAll('.itp-dl-btn').forEach(function(b){b.remove();});
    var s=document.getElementById('itp-dl-style');if(s)s.remove();
    if(window.__itpDlObs){window.__itpDlObs.disconnect();window.__itpDlObs=null;}
    window.__itpDlInit=false;
})();
""".trimIndent()

    // ──────── JS: emoji clan filter ────────
    private fun emojiFilterJs(emojis: String): String {
        val chars = emojis.trim()
        if (chars.isEmpty()) return "(function(){})()"
        // Build array of individual emoji characters (handles multi-codepoint emoji)
        val emojiList = mutableListOf<String>()
        val codePoints = chars.codePoints().toArray()
        var i = 0
        while (i < codePoints.size) {
            val cp = codePoints[i]
            var emoji = String(Character.toChars(cp))
            // Consume variation selectors and ZWJ sequences
            while (i + 1 < codePoints.size) {
                val next = codePoints[i + 1]
                if (next == 0xFE0F || next == 0x200D || (next in 0x1F3FB..0x1F3FF)) {
                    i++
                    emoji += String(Character.toChars(codePoints[i]))
                    if (next == 0x200D && i + 1 < codePoints.size) {
                        i++
                        emoji += String(Character.toChars(codePoints[i]))
                    }
                } else break
            }
            emojiList.add(emoji)
            i++
        }
        val jsArray = emojiList.joinToString(",") { "'${it}'" }
        return """
(function() {
    var blocked = [$jsArray];
    function hidePost(article) {
        var emojiEl = article.querySelector('.pl3SNO9Y');
        if (!emojiEl) return;
        var txt = emojiEl.textContent.trim();
        for (var i = 0; i < blocked.length; i++) {
            if (txt.indexOf(blocked[i]) >= 0) {
                article.style.display = 'none';
                article.setAttribute('data-itp-hidden-clan', '1');
                return;
            }
        }
    }
    document.querySelectorAll('article.FdYjPIR3').forEach(hidePost);
    if (window.__itpClanObs) { window.__itpClanObs.disconnect(); }
    window.__itpClanObs = new MutationObserver(function(ms) { ms.forEach(function(m) {
        m.addedNodes.forEach(function(n) { if(n.nodeType!==1)return;
            if(n.matches&&n.matches('article.FdYjPIR3'))hidePost(n);
            else if(n.querySelectorAll)n.querySelectorAll('article.FdYjPIR3').forEach(hidePost);
        }); }); });
    window.__itpClanObs.observe(document.body, {childList:true, subtree:true});
})();
""".trimIndent()
    }

    private val removeEmojiFilterJs = """
(function(){
    document.querySelectorAll('[data-itp-hidden-clan]').forEach(function(a){
        a.style.display=''; a.removeAttribute('data-itp-hidden-clan');
    });
    if(window.__itpClanObs){window.__itpClanObs.disconnect();window.__itpClanObs=null;}
})();
""".trimIndent()

    // ──────── JS: translator ────────
    private fun translatorJs(skipLangs: String, targetLang: String, autoTranslate: Boolean): String {
        val skipArr = skipLangs.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val skipJs = skipArr.joinToString(",") { "'$it'" }
        return """
(function() {
    if (window.__itpTrInit) return;
    window.__itpTrInit = true;
    var skipLangs = [$skipJs];
    var targetLang = '$targetLang';
    var autoMode = $autoTranslate;
    var style = document.createElement('style');
    style.id = 'itp-tr-style';
    style.textContent = '.itp-tr-btn{background:none;border:none;padding:0;margin-left:4px;cursor:pointer;display:inline-flex;align-items:center;gap:4px;color:var(--text-secondary,#888);vertical-align:middle;}.itp-tr-btn:active{opacity:0.5;}.itp-tr-btn svg{width:20px;height:20px;}.itp-tr-btn.translated{color:var(--accent-primary,#3B82F6);}.itp-tr-btn.loading svg{animation:itpSpin 1s linear infinite;}@keyframes itpSpin{from{transform:rotate(0)}to{transform:rotate(360deg)}}';
    document.head.appendChild(style);
    var iconTranslate = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="none"><path stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" d="M3 4h7M6.5 4V2.5M5 4c.4 2 1.8 4 3.5 5.5M9.5 4c-.4 2-1.8 4-3.5 5.5"/><path stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" d="M11 18l2.5-6 2.5 6M11.8 16.5h4.4"/></svg>';
    var iconLoading = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5" opacity="0.3"/><path stroke="currentColor" stroke-width="1.5" stroke-linecap="round" d="M10 3a7 7 0 0 1 7 7"/></svg>';
    var iconDone = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="none"><path stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" d="M3 4h7M6.5 4V2.5M5 4c.4 2 1.8 4 3.5 5.5M9.5 4c-.4 2-1.8 4-3.5 5.5"/><path stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" d="M11 18l2.5-6 2.5 6M11.8 16.5h4.4"/><circle cx="16" cy="4" r="2.5" fill="currentColor" stroke="none"/></svg>';

    function processPost(article) {
        if (article.getAttribute('data-itp-tr')) return;
        article.setAttribute('data-itp-tr', '1');
        var textEl = article.querySelector('.YmQiahvA, ._2QopExez');
        if (!textEl) return;
        var originalText = textEl.innerText.trim();
        if (!originalText || originalText.length < 3) return;
        var actionsRow = article.querySelector('._4ZjoCms2');
        if (!actionsRow) return;
        var artId = 'itp-art-' + Math.random().toString(36).substr(2,9);
        article.id = artId;
        var btn = document.createElement('button');
        btn.className = 'itp-tr-btn aMkvCscU';
        btn.innerHTML = iconTranslate;
        btn.setAttribute('data-original', originalText);
        btn.setAttribute('data-translated', '');
        btn.setAttribute('data-is-translated', '0');
        btn.addEventListener('click', function(e) {
            e.stopPropagation(); e.preventDefault();
            if (btn.getAttribute('data-is-translated') === '1') {
                textEl.innerText = btn.getAttribute('data-original');
                btn.innerHTML = iconTranslate;
                btn.classList.remove('translated', 'loading');
                btn.setAttribute('data-is-translated', '0');
            } else {
                var cached = btn.getAttribute('data-translated');
                if (cached) {
                    textEl.innerText = cached;
                    btn.innerHTML = iconDone;
                    btn.classList.add('translated');
                    btn.classList.remove('loading');
                    btn.setAttribute('data-is-translated', '1');
                } else {
                    btn.innerHTML = iconLoading;
                    btn.classList.add('loading');
                    if (window.Android) {
                        window.Android.translate(originalText, targetLang, artId);
                    }
                }
            }
        });

        actionsRow.appendChild(btn);

        if (autoMode) {
            btn.innerHTML = iconLoading;
            btn.classList.add('loading');
            if (window.Android) {
                window.Android.translate(originalText, targetLang, artId);
            }
        }
    }

    window.__itpTrCallback = function(articleId, translated) {
        var article = document.getElementById(articleId);
        if (!article) return;
        var btn = article.querySelector('.itp-tr-btn');
        if (!btn) return;
        var textEl = article.querySelector('.YmQiahvA, ._2QopExez');
        if (!textEl) return;
        btn.setAttribute('data-translated', translated);
        textEl.innerText = translated;
        btn.innerHTML = iconDone;
        btn.classList.add('translated');
        btn.classList.remove('loading');
        btn.setAttribute('data-is-translated', '1');
    };

    document.querySelectorAll('article.FdYjPIR3').forEach(processPost);
    if (window.__itpTrObs) window.__itpTrObs.disconnect();
    window.__itpTrObs = new MutationObserver(function(ms) { ms.forEach(function(m) {
        m.addedNodes.forEach(function(n) { if(n.nodeType!==1)return;
            if(n.matches&&n.matches('article.FdYjPIR3')) processPost(n);
            else if(n.querySelectorAll) n.querySelectorAll('article.FdYjPIR3').forEach(processPost);
        }); }); });
    window.__itpTrObs.observe(document.body, {childList:true, subtree:true});
})();
""".trimIndent()
    }

    private val removeTranslatorJs = """
(function(){
    document.querySelectorAll('.itp-tr-btn').forEach(function(b){
        var art = b.closest('article');
        if(art){
            var txt = art.querySelector('.YmQiahvA, ._2QopExez');
            var orig = b.getAttribute('data-original');
            if(txt && orig) txt.innerText = orig;
            art.removeAttribute('data-itp-tr');
        }
        b.remove();
    });
    var s=document.getElementById('itp-tr-style');if(s)s.remove();
    if(window.__itpTrObs){window.__itpTrObs.disconnect();window.__itpTrObs=null;}
    window.__itpTrInit=false;
    window.__itpTrCallback=null;
})();
""".trimIndent()

    // ──────── Android Bridge ────────
    inner class AndroidBridge {
        @JavascriptInterface
        fun downloadImage(url: String) {
            runOnUiThread {
                try {
                    val uri = Uri.parse(url)
                    val fileName = (uri.lastPathSegment ?: "itp_${System.currentTimeMillis()}").let {
                        if (!it.contains('.')) "$it.jpg" else it
                    }
                    val request = DownloadManager.Request(uri)
                        .setTitle(fileName)
                        .setDescription("ИТП — скачивание фото")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "ITP/$fileName")
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    Toast.makeText(this@MainActivity, "Сохранено в Pictures/ITP", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Ошибка скачивания", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun translate(text: String, targetLang: String, articleId: String) {
            Thread {
                try {
                    val encoded = URLEncoder.encode(text, "UTF-8")
                    val apiUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
                    val connection = URL(apiUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = reader.readText()
                    reader.close()

                    // Parse Google Translate JSON response
                    val json = JSONArray(response)
                    val translations = json.getJSONArray(0)
                    val sb = StringBuilder()
                    for (i in 0 until translations.length()) {
                        val part = translations.getJSONArray(i)
                        sb.append(part.getString(0))
                    }
                    val translated = sb.toString()
                    val escapedTranslated = translated
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                    val escapedId = articleId.replace("'", "\\'")
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "if(window.__itpTrCallback) window.__itpTrCallback('$escapedId','$escapedTranslated');",
                            null
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    // ──────── Activity lifecycle ────────
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("tweaks_prefs", MODE_PRIVATE)

        // Применяем состояние аналитики при каждом запуске
        Firebase.analytics.setAnalyticsCollectionEnabled(
            prefs.getBoolean("analytics_enabled", true)
        )

        val pin = prefs.getString("pin_code", null)
        if (pin != null && !pinUnlocked) {
            startActivity(Intent(this, PinActivity::class.java))
        }

        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ic = WindowCompat.getInsetsController(window, window.decorView)
        ic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ic.hide(WindowInsetsCompat.Type.systemBars())



        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris = mutableListOf<Uri>()
            if (result.resultCode == RESULT_OK) {
                val d = result.data
                if (d?.clipData != null) { for (i in 0 until d.clipData!!.itemCount) uris.add(d.clipData!!.getItemAt(i).uri) }
                else if (d?.data != null) uris.add(d.data!!)
            }
            fileUploadCallback?.onReceiveValue(if (uris.isEmpty()) null else uris.toTypedArray())
            fileUploadCallback = null
        }

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.addJavascriptInterface(AndroidBridge(), "Android")

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                injectTweaksIfNeeded()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView?, cb: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                fileUploadCallback?.onReceiveValue(null); fileUploadCallback = cb
                filePickerLauncher.launch(p?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) })
                return true
            }
        }

        // TV Cursor overlay
        cursorView = CursorView(this)
        cursorView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        cursorView.isClickable = false
        cursorView.isFocusable = false

        swipeRefresh = SwipeRefreshLayout(this)
        swipeRefresh.addView(webView)
        swipeRefresh.setOnRefreshListener { webView.reload() }
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ -> swipeRefresh.isEnabled = scrollY == 0 }

        val rootFrame = FrameLayout(this)
        rootFrame.addView(swipeRefresh)
        rootFrame.addView(cursorView)
        setContentView(rootFrame)

        val extraUrl = intent.getStringExtra(EXTRA_URL)
        if (savedInstanceState == null) {
            webView.loadUrl(extraUrl ?: "https://итд.com/")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) injectTweaksIfNeeded()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    // ──────── TV D-pad + cursor ────────
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val isDown = event.action == KeyEvent.ACTION_DOWN

                // Show cursor if hidden
                if (isDown && !cursorVisible) {
                    cursorX = cursorView.width / 2f
                    cursorY = cursorView.height / 2f
                    cursorVisible = true
                    cursorView.invalidate()
                    cursorHandler.removeCallbacks(hideCursorRunnable)
                    cursorHandler.postDelayed(hideCursorRunnable, 4000)
                }

                // Update axis state
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> isUpPressed = isDown
                    KeyEvent.KEYCODE_DPAD_DOWN -> isDownPressed = isDown
                    KeyEvent.KEYCODE_DPAD_LEFT -> isLeftPressed = isDown
                    KeyEvent.KEYCODE_DPAD_RIGHT -> isRightPressed = isDown
                }

                if (isDown) {
                    // Start update loop if needed
                    cursorHandler.removeCallbacks(cursorUpdateRunnable)
                    cursorHandler.post(cursorUpdateRunnable)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!cursorVisible) return super.dispatchKeyEvent(event)
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        okLongPressPending = true
                        cursorHandler.postDelayed(okLongPressRunnable, 800)
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    if (okLongPressPending) {
                        okLongPressPending = false
                        cursorHandler.removeCallbacks(okLongPressRunnable)

                        // Simulate a real native touch event at cursor position
                        val time = SystemClock.uptimeMillis()
                        val motionEventDown = MotionEvent.obtain(
                            time, time, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0
                        )
                        val motionEventUp = MotionEvent.obtain(
                            time, time + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0
                        )
                        webView.dispatchTouchEvent(motionEventDown)
                        webView.dispatchTouchEvent(motionEventUp)
                        motionEventDown.recycle()
                        motionEventUp.recycle()

                        // Briefly flash cursor red/gray to indicate click
                        cursorView.invalidate()
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    startActivity(Intent(this, TweaksActivity::class.java))
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ──────── Analytics helper ────────
    private fun logTweak(name: String, enabled: Boolean) {
        val prefs = getSharedPreferences("tweaks_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("analytics_enabled", true)) return
        Firebase.analytics.logEvent(name) {
            param(FirebaseAnalytics.Param.VALUE, if (enabled) 1L else 0L)
        }
    }

    // ──────── Tweak injection ────────
    private fun injectTweaksIfNeeded() {
        val prefs = getSharedPreferences("tweaks_prefs", MODE_PRIVATE)
        webView.evaluateJavascript(removeTapHighlightJs, null)

        val blurEnabled = prefs.getBoolean("blur_profanity", false)
        webView.evaluateJavascript(if (blurEnabled) blurProfanityJs else removeBlurJs, null)
        logTweak("tweak_blur_profanity", blurEnabled)

        val trafficEnabled = prefs.getBoolean("traffic_saver", false)
        webView.evaluateJavascript(if (trafficEnabled) trafficSaverJs else removeTrafficSaverJs, null)
        logTweak("tweak_traffic_saver", trafficEnabled)

        val dlEnabled = prefs.getBoolean("image_download", true)
        webView.evaluateJavascript(if (dlEnabled) imageDownloadJs else removeImageDownloadJs, null)
        logTweak("tweak_image_download", dlEnabled)

        // Emoji clan filter
        val blockedEmojis = prefs.getString("blocked_emojis", "") ?: ""
        if (blockedEmojis.isNotEmpty()) {
            webView.evaluateJavascript(emojiFilterJs(blockedEmojis), null)
            logTweak("tweak_emoji_filter", true)
        } else {
            webView.evaluateJavascript(removeEmojiFilterJs, null)
            logTweak("tweak_emoji_filter", false)
        }

        // Translator
        val trEnabled = prefs.getBoolean("translator_enabled", false)
        if (trEnabled) {
            val skipLangs = prefs.getString("translator_skip_langs", "ru") ?: "ru"
            val targetLang = prefs.getString("translator_target_lang", "ru") ?: "ru"
            val autoTranslate = prefs.getBoolean("translator_auto", false)
            webView.evaluateJavascript(translatorJs(skipLangs, targetLang, autoTranslate), null)
        } else {
            webView.evaluateJavascript(removeTranslatorJs, null)
        }
        logTweak("tweak_translator", trEnabled)

        val myEnabled = prefs.getBoolean("material_you", false)
        if (myEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            injectMaterialYouColors()
        } else {
            webView.evaluateJavascript("(function(){var el=document.getElementById('itp-my');if(el)el.remove();})()", null)
        }
        logTweak("tweak_material_you", myEnabled)

        // PC Version: Desktop UA + landscape orientation + layout scaling
        val pcEnabled = prefs.getBoolean("pc_version", false)
        if (pcEnabled) {
            webView.settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            webView.setInitialScale(1)
            webView.evaluateJavascript(
                """
                (function() {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (meta) {
                        meta.setAttribute('content', 'width=1280');
                    } else {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=1280';
                        document.head.appendChild(meta);
                    }
                })();
                """.trimIndent(), null
            )
        } else {
            webView.settings.userAgentString = null
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            webView.settings.useWideViewPort = false
            webView.settings.loadWithOverviewMode = false
            webView.setInitialScale(0)
            webView.evaluateJavascript(
                """
                (function() {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (meta) {
                        meta.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0');
                    }
                })();
                """.trimIndent(), null
            )
        }
        logTweak("tweak_pc_version", pcEnabled)
    }

    @SuppressLint("DiscouragedApi")
    private fun injectMaterialYouColors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        fun hex(r: Int): String = String.format("#%06X", 0xFFFFFF and getColor(r))

        val lA1_300=hex(android.R.color.system_accent1_300); val lA1_500=hex(android.R.color.system_accent1_500)
        val lA1_600=hex(android.R.color.system_accent1_600); val lA1_700=hex(android.R.color.system_accent1_700)
        val lA2_300=hex(android.R.color.system_accent2_300); val lA3_500=hex(android.R.color.system_accent3_500)
        val lN1_10=hex(android.R.color.system_neutral1_10); val lN1_50=hex(android.R.color.system_neutral1_50)
        val lN1_100=hex(android.R.color.system_neutral1_100); val lN1_300=hex(android.R.color.system_neutral1_300)
        val lN1_500=hex(android.R.color.system_neutral1_500); val lN1_900=hex(android.R.color.system_neutral1_900)
        val dA1_100=hex(android.R.color.system_accent1_100); val dA1_200=hex(android.R.color.system_accent1_200)
        val dA1_300=hex(android.R.color.system_accent1_300); val dA3_200=hex(android.R.color.system_accent3_200)
        val dA2_200=hex(android.R.color.system_accent2_200)
        val dN1_50=hex(android.R.color.system_neutral1_50); val dN1_100=hex(android.R.color.system_neutral1_100)
        val dN1_200=hex(android.R.color.system_neutral1_200); val dN1_400=hex(android.R.color.system_neutral1_400)
        val dN1_600=hex(android.R.color.system_neutral1_600); val dN1_700=hex(android.R.color.system_neutral1_700)
        val dN1_800=hex(android.R.color.system_neutral1_800); val dN1_900=hex(android.R.color.system_neutral1_900)

        val css = ":root{--bg-primary:$lN1_50!important;--bg-secondary:$lN1_50!important;--bg-tertiary:$lN1_100!important;" +
            "--text-primary:$lN1_900!important;--text-secondary:$lN1_500!important;--text-tertiary:$lN1_300!important;--text-inverse:$lN1_10!important;" +
            "--accent-primary:$lA1_500!important;--accent-secondary:$lA1_300!important;--accent-hover:$lA1_700!important;" +
            "--accent-like:$lA3_500!important;--accent-repost:$lA2_300!important;--link-color:$lA1_600!important;" +
            "--border-color:${lN1_300}22!important;--block-bg:$lN1_10!important;--block-bg-secondary:$lN1_50!important;--block-hover-bg:$lN1_100!important;" +
            "--bg-hover:${lN1_900}0D!important;--bg-active:${lN1_900}1A!important;--tab-active-bg:$lN1_100!important;" +
            "--toggle-active-bg:$lN1_900!important;--glass-bg:${lN1_10}80!important;" +
            "--nav-gradient:linear-gradient(to bottom,transparent 0%,${lN1_50}4D 30%,${lN1_50}CC 100%)!important;" +
            "--btn-primary-bg:$lN1_900!important;--btn-primary-text:$lN1_10!important;--shadow-elevated:0 4px 16px ${lN1_900}14!important;}" +
            "[data-theme=dark]{--bg-primary:$dN1_900!important;--bg-secondary:$dN1_800!important;--bg-tertiary:$dN1_700!important;" +
            "--text-primary:$dN1_50!important;--text-secondary:$dN1_400!important;--text-tertiary:$dN1_600!important;--text-inverse:$dN1_900!important;" +
            "--accent-primary:$dA1_200!important;--accent-secondary:$dA1_300!important;--accent-hover:$dA1_100!important;" +
            "--accent-like:$dA3_200!important;--accent-repost:$dA2_200!important;--link-color:$dA1_200!important;" +
            "--border-color:${dN1_200}1A!important;--block-bg:$dN1_800!important;--block-bg-secondary:$dN1_700!important;--block-hover-bg:$dN1_700!important;" +
            "--bg-hover:${dN1_100}14!important;--bg-active:${dN1_100}1F!important;--tab-active-bg:${dN1_100}14!important;" +
            "--toggle-active-bg:$dN1_50!important;--glass-bg:${dN1_800}80!important;" +
            "--nav-gradient:linear-gradient(to bottom,transparent 0%,${dN1_900}4D 30%,${dN1_900}CC 100%)!important;" +
            "--btn-primary-bg:$dN1_50!important;--btn-primary-text:$dN1_900!important;--shadow-elevated:0 4px 16px ${dN1_900}33!important;}"
        val escaped = css.replace("'", "\\'")
        webView.evaluateJavascript("(function(){var el=document.getElementById('itp-my');if(!el){el=document.createElement('style');el.id='itp-my';document.head.appendChild(el);}el.textContent='$escaped';})()", null)
    }
}
