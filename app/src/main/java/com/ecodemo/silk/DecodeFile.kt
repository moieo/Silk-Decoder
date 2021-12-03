package com.ecodemo.silk;
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
import java.lang.Character
import android.os.Message
import java.util.Collections
import android.view.Menu
import android.app.Activity
import android.widget.Toast
import java.util.regex.Pattern
import android.content.Intent
import android.view.MenuItem
import android.app.AlertDialog
import android.os.Environment
import android.content.Context
import android.provider.Settings
import android.app.ProgressDialog
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import android.content.DialogInterface
import androidx.core.app.ActivityCompat
import java.util.concurrent.ExecutorService
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.documentfile.provider.DocumentFile
import com.ecodemo.silk.databinding.DecodeFileBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration

class DecodeFile: Activity() {

    private lateinit var binding: DecodeFileBinding
    private var list = mutableListOf<File>()
    private var adapter: DecodeAdapter? = null
    @Suppress("DEPRECATION")
    private val sd_path: File = Environment.getExternalStorageDirectory()
    @Suppress("UNUSED", "DEPRECATION")
    private var handler: Handler = Handler {
        if(it.what == 0) {
            list.add(it.obj as File)
            adapter?.notifyDataSetChanged()
            false
        }
        false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
        binding = DecodeFileBinding.inflate(layoutInflater)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(binding.root)
        
        adapter = DecodeAdapter(this, list)
        var decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.recycle.addItemDecoration(decoration)
        var layoutManager = LinearLayoutManager(this)
        layoutManager.setRecycleChildrenOnDetach(true)
        binding.recycle.layoutManager = layoutManager
        binding.recycle.adapter = adapter
        binding.recycle.setHasFixedSize(true)
        binding.recycle.setItemViewCacheSize(60)
        binding.recycle.setDrawingCacheEnabled(true)
        
        var files = File(sd_path, "Silk解码器/解码")
        if(!files.exists()) {
            files.mkdirs()
        }
        if(files.listFiles().size == 0) {
            binding.tisp.text = "啥也没有"
            binding.tisp.visibility = View.VISIBLE
            return;
        }
        for(file in files.listFiles()) {
            if(file.isFile){
                var msg = Message()
                msg.what = 0
                msg.obj = file
                handler.sendMessage(msg)
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.getItemId()) {
            android.R.id.home -> {
                finish()
            }
        }
        return false
    }
}