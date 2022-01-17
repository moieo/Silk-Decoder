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
import java.util.concurrent.Executors;
import android.content.DialogInterface
import androidx.core.app.ActivityCompat
import java.util.concurrent.ExecutorService
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.documentfile.provider.DocumentFile
import com.ecodemo.silk.databinding.ActivityMainBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration


class OtherDecoder : Activity() {

    private lateinit var binding: ActivityMainBinding
    private var list = mutableListOf<DocumentFile>()
    private var adapter: MxRecyclerAdapter? = null
    private var document: DocumentFile? = null
    @Suppress("DEPRECATION")
    private var progress: ProgressDialog? = null
    @Suppress("DEPRECATION")
    private val sd_path: File = Environment.getExternalStorageDirectory()
    @Suppress("UNUSED", "DEPRECATION")
    private var handler: Handler = Handler {
        if(it.what == 0) {
            list.add(it.obj as DocumentFile)
            adapter?.notifyDataSetChanged()
            false
        }
        if(it.what == 1) { 
            Toast.makeText(this, it.obj as String, Toast.LENGTH_SHORT).show()
            false
        }
        if(it.what == 6) {
            adapter?.notifyDataSetChanged()
            progress?.dismiss()
            false
        }
        if(it.what == 7) {
            progress?.setTitle("正在排序，请稍候...")
        }
        if(it.what == 888) {
            if(list.size > 0) {
                binding.tisp.visibility = View.GONE
            }
        }
        if(it.what == 999) {
            if(list.size <= 0) {
                binding.tisp.text = "啥也没有"
                binding.tisp.visibility = View.VISIBLE
            }
        }
        false
    }
    
    //private var files: MutableList<File> = mutableListOf<File>()
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
        actionBar?.setDisplayHomeAsUpEnabled(true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MxRecyclerAdapter(this, list)
        var decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.recycle.addItemDecoration(decoration)
        var layoutManager = LinearLayoutManager(this)
        layoutManager.setRecycleChildrenOnDetach(true)
        binding.recycle.layoutManager = layoutManager
        binding.recycle.adapter = adapter
        binding.recycle.setHasFixedSize(true)
        binding.recycle.setItemViewCacheSize(60)
        binding.recycle.setDrawingCacheEnabled(true)
        
        
        /* if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){ //安卓11 以上
            if(!FileUriUtils.isGrant(this@OtherDecoder)){
                FileUriUtils.startForRoot(this@OtherDecoder, 200)
                return;
            }
            document = DocumentFile.fromTreeUri(this, Uri.parse(FileUriUtils.changeToUri("/storage/emulated/0/Silk解码器/")))
        } else {
            var file_: File = File(sd_path, "Silk解码器")
            document = DocumentFile.fromFile(file_)
        } */
        var file_: File = File(sd_path, "Silk解码器")
        document = DocumentFile.fromFile(file_)
        
        scanFiles()
    }
    
    //@Synchronized 
    fun scanSilk(document: DocumentFile?) {
        handler.sendEmptyMessage(888)
		var files: Array<DocumentFile>? = document?.listFiles()
		var list_tmp = mutableListOf<DocumentFile>()
		for (file in files!!) {
			if(file.isFile()) {
                if(file.getUri().getPath()!!.endsWith("slk")){
			        updateFileList(file)
			    }
			} else {
			    list_tmp.add(file)
			}
		}
		while(list_tmp.size > 0) {
		    var file_: DocumentFile? = list_tmp.removeAt(0)
		    var files_: Array<DocumentFile>? = file_?.listFiles()
		    for (file in files_!!) {
		        if(file.isFile()) {
			        if(file.getUri().getPath()!!.endsWith("slk")){
			            updateFileList(file)
			        }
			    } else {
			        list_tmp.add(file)
			    }
			}
		}
		handler.sendEmptyMessage(999)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 4, 0, "刷新")
        var order = menu?.addSubMenu(1, 1, 1, "排序")
        order?.add(2, 2, 1, "时间顺序")
        order?.add(2, 3, 2, "时间顺序（倒序）")
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
	    when(item.getItemId()){
	        2 -> {
	            progress = ProgressDialog.show(this, "正在排序，请稍候...", null, true, false)
	            Thread {
	                list.sortBy({ it.lastModified().toInt() })
	                handler.sendEmptyMessage(6)
	            }.start()
	        }
	        3 -> {
	            progress = ProgressDialog.show(this, "正在排序，请稍候...", null, true, false)
	            Thread {
	                list.sortByDescending({ it.lastModified().toInt() })
	                handler.sendEmptyMessage(6)
	            }.start()
	        }
	        4 -> {
	            scanFiles()
	        }
	        android.R.id.home -> {
                finish()
            }
	    }
        return super.onOptionsItemSelected(item);
    }
	
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, Data: Intent?) {
        super.onActivityResult(requestCode, resultCode, Data)
        var uri: Uri? = Data?.getData()
        if (requestCode == 200 && uri != null) {
            var flag = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            getContentResolver().takePersistableUriPermission(uri, Data!!.flags and flag)
            reload()
        }
    }
    
    fun reload() {
		var intent = getIntent()
		overridePendingTransition(0, 0)
		intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
		finish()
		overridePendingTransition(0, 0)
		startActivity(intent)
	}
	
	fun updateFileList(file: DocumentFile){
	    var msg = Message()
        msg.obj = file
        msg.what = 0
        handler.sendMessage(msg)
	}
	
    fun isQQNum(num: String?): Boolean {
        val compile = Pattern.compile("^\\d{5,10}$")
        val matcher = compile.matcher(num!!)
        return matcher.matches()
    }
    
    fun scanFiles() {
        list.clear()
        var files: Array<DocumentFile>? = document?.listFiles()
        progress = ProgressDialog.show(this, "正在扫描，请稍候...", null, true, false)
        
        var exe = Executors.newCachedThreadPool()
        files?.forEach {
            if(it.isFile()) {
                if(it.getUri().getPath()!!.endsWith("slk")){
    			    updateFileList(it)
    		    }
    		} else {
    		    if(isQQNum(it.getName())){
    		        exe.execute {
    		            scanSilk(it)
    		        }
    		    }
            }
        }
        exe.shutdown()
        
        Thread {
            while(true){
                if (exe.isTerminated()){
                    Thread.sleep(250)
                    /* handler.sendEmptyMessage(7)
                    list.sortByDescending({ it.lastModified().toInt() })*/
                    handler.sendEmptyMessage(6)
                    handler.sendEmptyMessage(999)
                    break
                }
                continue
            }
        }.start()
    }
}