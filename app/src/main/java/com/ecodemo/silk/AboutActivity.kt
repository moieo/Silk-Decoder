package com.ecodemo.silk;

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient

class AboutActivity : Activity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
        actionBar?.setDisplayHomeAsUpEnabled(true)
        var web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.loadUrl("file:///android_asset/about.html")
        web.setWebViewClient(object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
				startActivity(intent)
				return true
            }
        })
        setContentView(web)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.getItemId()) {
            android.R.id.home -> {
                finish()
            }
        }
        return false;
    }
}