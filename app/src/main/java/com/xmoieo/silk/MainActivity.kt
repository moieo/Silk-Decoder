package com.xmoieo.silk;

import java.io.File
import android.net.Uri
import kotlin.Suppress
import android.os.Build
import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.LinkedList
import java.lang.Runnable
import android.os.Message
import java.util.Collections
import android.view.Menu
import android.app.Activity
import android.widget.Toast
import java.util.regex.Pattern
import android.content.Intent
import android.view.MenuItem
import android.widget.EditText
import android.app.AlertDialog
import android.os.Environment
import android.content.Context
import android.provider.Settings
import android.app.ProgressDialog
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors;
import android.content.DialogInterface
import androidx.core.app.ActivityCompat
import java.util.concurrent.ExecutorService
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.preference.PreferenceActivity

@Suppress("DEPRECATION")
class MainActivity: PreferenceActivity() {

  private val app_cache: File = File(Environment.getExternalStorageDirectory(), "Silk解码器")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /* 设置状态栏字体色 不然看不清字*/
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    addPreferencesFromResource(R.xml.main)

    /* 权限申请 */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)) {
      initPermission()
      return
    }

    if(!app_cache.exists() && app_cache.isDirectory()) {
      app_cache.mkdirs()
    }

    findPreference("install_ffmpeg")?.setOnPreferenceClickListener {
      /* val view = View.inflate(this, R.layout.install_ffmpeg_dialog, null)
      val edit: EditText = view.findViewById(R.id.path) */
      val builder = AlertDialog.Builder(this)
        .setTitle("安装 FFmpeg")
        .setMessage("由于本应用使用 Android 手机 开发，无法编译 FFmpeg，坐等有缘人协助...")
        .setPositiveButton("确定", null)
        // .setNeutralButton("取消", null)
      builder.create().show()
      true
    }

    findPreference("author_coolapk")?.setOnPreferenceClickListener {
      try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("coolmarket://u/1512829")))
      } catch (e: Exception) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://www.coolapk.com/u/1512829")))
      }
      true
    }

    findPreference("app_coolapk")?.setOnPreferenceClickListener {
      try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("coolmarket://apk/com.xmoieo.silk")))
      } catch (e: Exception) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/apk/com.xmoieo.silk")))
      }
      true
    }
    
    findPreference("update_log")?.setOnPreferenceClickListener {
      try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Moieo/Silk-Decoder/releases")))
      } catch (e: Exception) {
        Toast.makeText(this, "跳转失败：你可能没有安装浏览器", Toast.LENGTH_SHORT).show()
      }
      true
    }
    
    findPreference("issue")?.setOnPreferenceClickListener {
      try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Moieo/Silk-Decoder/issues")))
      } catch (e: Exception) {
        Toast.makeText(this, "跳转失败：你可能没有安装浏览器", Toast.LENGTH_SHORT).show()
      }
      true
    }
  }
  
  //返回授权状态
  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, Data: Intent?) {
    super.onActivityResult(requestCode, resultCode, Data)
    val uri: Uri? = Data?.data
    if (requestCode == 0xFC && uri != null) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        if(Environment.isExternalStorageManager()){
          Toast.makeText(this, "存储权限授权成功", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, "你拒绝了授权", Toast.LENGTH_SHORT).show()
          java.lang.System.exit(0)
        }
      }
      if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
        Toast.makeText(this, "你拒绝了授权", Toast.LENGTH_SHORT).show()
        java.lang.System.exit(0)
      }
    }
    reload()
  }

  fun initPermission() {
    AlertDialog.Builder(this)
    .setMessage("你正在使用 Silk解码器 App，本软件基于 Silkv3 开源库，我们也很不想让你给予软件 存储权限，由于 Android 6 以上 系统的一些限制 和 本应用的原理，你只能通过给予权限，才能够使用App，否则你将无法使用本 App，我们不会收集和使用你的任何个人信息，本软件无需联网")
    .setTitle("警告")
    .setPositiveButton("同意", DialogInterface.OnClickListener { _, _->
      if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
          var intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
          intent.data = Uri.parse("package:" + getPackageName())
          startActivityForResult(intent, 0xFC)
        }
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
          //没有权限则申请权限
          ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1);
        }
      }
    }).setNeutralButton("取消", DialogInterface.OnClickListener{ _, _ ->
      java.lang.System.exit(0)
    }).create().show()
  }

  /* 重启 Activity */
  fun reload() {
    var intent = getIntent()
    overridePendingTransition(0, 0)
    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    finish()
    overridePendingTransition(0, 0)
    startActivity(intent)
  } 
}
