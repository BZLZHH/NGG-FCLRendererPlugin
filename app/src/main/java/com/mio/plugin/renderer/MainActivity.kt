package com.bzlzhh.plugin.ngg

import NGGConfigEditor
import android.Manifest
import android.animation.*
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.*
import android.provider.Settings
import android.view.*
import android.util.TypedValue
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
//import com.bzlzhh.plugin.ngg.BuildConfig.useANGLE
import io.noties.markwon.Markwon
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import com.google.android.material.button.MaterialButton
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils

class MainActivity : Activity() {
    private val websiteUrl = "https://ng-gl4es.bzlzhh.top"
    private val markdownFileUrl = "https://raw.githubusercontent.com/BZLZHH/NG-GL4ES/main/README.md"
    
    private val REQUEST_CODE_PERMISSION = 0x00099
    private val REQUEST_CODE = 12

    private var ANGLESwitcher: MaterialSwitch? = null
    
    private val client = OkHttpClient()
    private var isMarkdownVisible = false
    private var markdownView: TextView? = null
    private var markdownViewAnimFinished = true
    
    private var hasAllFilesPermission = false
        set(value) {
            if (ANGLESwitcher != null) {
                // ANGLESwitcher!!.isEnabled = value
            }
            field = value
        }
    private var isNoticedAllFilesPermissionMissing = true

    fun Int.dpToPx(): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()
        if (hasAllFilesPermission) {
            NGGConfigEditor.configRefresh()
        }

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(32, 32, 32, 32)
        }

        val kryptonTextView = MaterialTextView(this).apply {
            text = "Krypton Wrapper"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32.dpToPx()
                bottomMargin = 16.dpToPx()
            }
        }

        val releaseTextView = MaterialTextView(this).apply {
            text = getAppVersionName()
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY))
            gravity = Gravity.CENTER
        }

        val byTextView = MaterialTextView(this).apply {
            text = "By BZLZHH"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            gravity = Gravity.CENTER
        }

        val divider = MaterialDivider(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
                bottomMargin = 100
            }
        }

        val horizontalInnerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val settingText1 = MaterialTextView(this).apply {
            text = "禁用 ANGLE"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            gravity = Gravity.CENTER
        }

        val settingText2 = MaterialTextView(this).apply {
            text = "启用 ANGLE"
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            gravity = Gravity.CENTER
        }

        ANGLESwitcher = MaterialSwitch(this).apply {
            isChecked = false
            isEnabled = false
            // isEnabled = hasAllFilesPermission
            // setOnCheckedChangeListener { _, _ ->
            //     NGGConfigEditor.configSetInt("enableANGLE", if (isChecked) 1 else 0)
            //     NGGConfigEditor.configSaveToFile()
            //     refreshConfig()
            // }
        }

        horizontalInnerLayout.addView(settingText1.apply { setPadding(0,0,8.dpToPx(),0) })
        horizontalInnerLayout.addView(ANGLESwitcher?.apply { setPadding(8.dpToPx(),0,8.dpToPx(),0) })
        horizontalInnerLayout.addView(settingText2.apply { setPadding(8.dpToPx(),0,0,0) })

        val angleDisabledHint = MaterialTextView(this).apply {
            text = "ANGLE 在当前版本被禁用，后续可能修复 (x4)"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8.dpToPx()
            }
        }

        val divider2 = MaterialDivider(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
                bottomMargin = 100
            }
        }

        val goToWebsiteButton = MaterialButton(this).apply {
            text = "跳转至官网"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                startActivity(intent)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dpToPx()
            ).apply { bottomMargin = 16.dpToPx() }

        }

        val shareLogButton = MaterialButton(this).apply {
            text = "分享日志文件"
            setOnClickListener {
                val logFile = File(NGGConfigEditor.LOG_FILE_PATH)
                if (!logFile.exists()) {
                    Toast.makeText(context, "日志文件不存在！", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "$packageName.fileprovider",
                            logFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "分享日志文件"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "分享日志文件失败！", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dpToPx()
            ).apply { bottomMargin = 16.dpToPx() }

        }

        val showReadmeButton = MaterialButton(this).apply {
            text = "查看 README.md"
            setOnClickListener {
                if (markdownViewAnimFinished) {
                    markdownViewAnimFinished = false
                    isMarkdownVisible = !isMarkdownVisible
                    if (isMarkdownVisible) {
                        text = "隐藏 README.md"
                        fetchMarkdown(layout)
                    } else {
                        text = "查看 README.md"
                        markdownView?.let { hideMarkdownWithAnimation(it, layout) }
                    }
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dpToPx()
            ).apply { bottomMargin = 16.dpToPx() }

        }

        layout.apply {
            addView(kryptonTextView)
            addView(releaseTextView)
            addView(byTextView)
            addView(divider)
            addView(horizontalInnerLayout)
            addView(angleDisabledHint)
            addView(divider2)
            addView(goToWebsiteButton.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 50 }
            })
            addView(shareLogButton.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 50 }
            })
            addView(showReadmeButton)
        }

        scrollView.addView(layout)
        setContentView(scrollView)

        refreshConfig()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hasAllFilesPermission = true
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("权限请求")
                    .setMessage("程序需要获取访问所有文件权限才能正常使用 Krypton Wrapper 设置功能。是否授予？")
                    .setPositiveButton("是") { _: DialogInterface?, _: Int ->
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:" + this.packageName)
                        startActivityForResult(intent, REQUEST_CODE)
                        isNoticedAllFilesPermissionMissing = false
                    }
                    .setNegativeButton("否") { _: DialogInterface?, _: Int ->
                        isNoticedAllFilesPermissionMissing = true
                        Toast.makeText(
                            this,
                            "拒绝授权将导致设置功能无法正常工作",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setOnKeyListener { _, keyCode, _ ->
                        keyCode == KeyEvent.KEYCODE_BACK
                    }
                    .setCancelable(false)
                    .show()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_PERMISSION
                )
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                hasAllFilesPermission = true
            } else {
                Toast.makeText(this, "拒绝授权将导致设置功能无法正常工作", Toast.LENGTH_SHORT)
                    .show()
                hasAllFilesPermission = false
            }
        }

    }

    private fun isVulkanSupported(context: Context): Boolean {
        val packageManager = context.packageManager
        val hasVulkanLevel1 = packageManager.hasSystemFeature("android.hardware.vulkan.level")
        val hasVulkanBasic = packageManager.hasSystemFeature("android.hardware.vulkan.version")
        return hasVulkanLevel1 || hasVulkanBasic
    }
    
    private fun checkVulkanSupportability(context: Context) {
        if (!isVulkanSupported(context)) {
            AlertDialog.Builder(context)
                .setTitle("警告")
                .setMessage("检测到设备不支持 Vulkan! 这意味着此设备无法使用带有 ANGLE 的 Krypton Wrapper!\n\n您应当前往官网，下载\"NO-ANGLE\"版本。")
                .setPositiveButton("前往官网") { _: DialogInterface?, _: Int ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                    startActivity(intent)
                }
                .setNegativeButton("继续使用此版本") { _: DialogInterface?, _: Int ->  }
                .setOnKeyListener { _, keyCode, _ ->
                    keyCode == KeyEvent.KEYCODE_BACK
                }
                .setCancelable(false)
                .show()
        }
    }
    
    private fun getGPUName(): String? {
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            return null
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            return null
        }

        val configAttributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val eglConfigs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttributes, 0, eglConfigs, 0, eglConfigs.size, numConfigs, 0)) {
            EGL14.eglTerminate(eglDisplay)
            return null
        }
        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        val eglContext = EGL14.eglCreateContext(eglDisplay, eglConfigs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            EGL14.eglTerminate(eglDisplay)
            return null
        }
        val surfaceAttributes = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfigs[0], surfaceAttributes, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
            return null
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
            return null
        }
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
        return renderer
    }
    
    private fun checkAdreno740() {
        //val renderer = getGPUName()
        //if (renderer != null && renderer.contains("Adreno", ignoreCase = true) && renderer.contains("740", ignoreCase = true)) {
        //    // device is Adreno 740
        //    if (useANGLE)
        //        AlertDialog.Builder(this)
        //            .setTitle("警告")
        //            .setMessage("检测到设备的 GPU 是 Adreno 740! Adreno 740 可能在使用了 ANGLE 的 Krypton Wrapper 的中出现严重渲染错误!\n\n您应当前往官网，下载\"NO-ANGLE\"版本。")
        //            .setPositiveButton("前往官网") { _: DialogInterface?, _: Int ->
        //                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        //                startActivity(intent)
        //            }
        //            .setNegativeButton("继续使用此版本") { _: DialogInterface?, _: Int ->  }
        //            .setOnKeyListener { _, keyCode, _ ->
        //                keyCode == KeyEvent.KEYCODE_BACK
        //            }
        //            .setCancelable(false)
        //            .show()
        //} else {
        //    if (!useANGLE)
        //     AlertDialog.Builder(this)
        //         .setTitle("警告")
        //         .setMessage("检测到设备的 GPU 不是 Adreno 740! 非 Adreno 740 的设备在没有使用 ANGLE 的 Krypton Wrapper 的中出现少部分光影渲染错误，且效率更低!\n\n您应当前往官网，下载没有标注\"NO-ANGLE\"版本。")
        //         .setPositiveButton("前往官网") { _: DialogInterface?, _: Int ->
        //             val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        //             startActivity(intent)
        //         }
        //         .setNegativeButton("继续使用此版本") { _: DialogInterface?, _: Int ->  }
        //         .setOnKeyListener { _, keyCode, _ ->
        //             keyCode == KeyEvent.KEYCODE_BACK
        //         }
        //         .setCancelable(false)
        //         .show()
        //}
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchMarkdown(layout: LinearLayout) {
        GlobalScope.launch {
            val markdownContent = getMarkdownFromGitHub()
            withContext(Dispatchers.Main) {
                renderMarkdownWithAnimation(markdownContent, layout)
            }
        }
    }

    private fun getMarkdownFromGitHub(): String? {
        val request = Request.Builder().url(markdownFileUrl).build()
        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                "Failed to get text from $markdownFileUrl"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to get text from $markdownFileUrl"
        }
    }

    private fun renderMarkdownWithAnimation(markdownContent: String?, layout: LinearLayout) {
        if (markdownContent.isNullOrEmpty()) return

        val markwon = Markwon.create(this)

        val baseColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
        val backgroundColor = ColorUtils.setAlphaComponent(baseColor, (0.0618f * 255).toInt())

        markdownView = TextView(this).apply {
            textSize = 16f
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK))
            gravity = Gravity.START
            alpha = 0f
            translationY = resources.displayMetrics.heightPixels.toFloat() * 0.3f

            setPadding(60, 40, 60, 40)

            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = 100f
            shape.setColor(backgroundColor) // 设置主题色的淡色背景
            background = shape
        }
        markwon.setMarkdown(markdownView!!, markdownContent)
        layout.addView(markdownView)
        markdownView!!.layoutParams =
            (markdownView!!.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = 40
            }

        val alphaAnimator = ObjectAnimator.ofFloat(markdownView, View.ALPHA, 0f, 1f)
        val translationYAnimator = ObjectAnimator.ofFloat(
            markdownView,
            View.TRANSLATION_Y,
            resources.displayMetrics.heightPixels.toFloat() * 0.3f,
            0f
        )

        AnimatorSet().apply {
            playTogether(alphaAnimator, translationYAnimator)
            duration = 300
            interpolator = android.view.animation.DecelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    markdownViewAnimFinished = true
                }

                override fun onAnimationCancel(p0: Animator) {}
                override fun onAnimationRepeat(p0: Animator) {}
            })
            start()
        }
    }

    private fun hideMarkdownWithAnimation(view: View, layout: LinearLayout) {
        val alphaAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
        val translationYAnimator = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            0f,
            resources.displayMetrics.heightPixels.toFloat() * 0.3f
        )

        AnimatorSet().apply {
            playTogether(alphaAnimator, translationYAnimator)
            duration = 300
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    layout.removeView(view)
                    markdownView = null
                    markdownViewAnimFinished = true
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            packageInfo.versionName ?: "Unknown Version"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Unknown Version"
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hasAllFilesPermission = true
                isNoticedAllFilesPermissionMissing = false
            } else {
                if (!isNoticedAllFilesPermissionMissing)
                    Toast.makeText(this, "拒绝授权将导致设置功能无法正常工作", Toast.LENGTH_SHORT).show()
                isNoticedAllFilesPermissionMissing = true
                hasAllFilesPermission = false
            }
        }
        refreshConfig()
    }
    
    private fun refreshConfig() {
        if(hasAllFilesPermission) {
            NGGConfigEditor.configRefresh()
            var changed = false
            // val ANGLE = NGGConfigEditor.configGetInt("enableANGLE")
            // ANGLESwitcher?.isChecked = ANGLE == 1

            // if (ANGLE != 0 && ANGLE != 1) {
            //     changed = true
            //     NGGConfigEditor.configSetInt("enableANGLE", 1)
            // }
            // if (changed) {
            //     NGGConfigEditor.configSaveToFile()
            // }
        }
    }
}
