package com.xmoieo.silk

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Environment
import android.view.View
import android.view.MenuItem
import android.app.Activity
import android.app.ProgressDialog
import android.widget.Toast
import android.content.Intent
import android.provider.OpenableColumns
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.xmoieo.silk.databinding.ActivityDecoderBinding

/**
 * Silk 解码器 Activity
 * 支持选择 Silk 文件并解码为 MP3 格式
 */
class SilkDecoder : Activity() {

    private lateinit var binding: ActivityDecoderBinding
    
    @Suppress("DEPRECATION")
    private val decodePath: File = File(Environment.getExternalStorageDirectory(), "Silk解码器/解码")
    
    private var selectedFileUri: Uri? = null
    
    private var decodedFiles = mutableListOf<File>()
    private var adapter: DecodeAdapter? = null
    
    @Suppress("DEPRECATION")
    private var progress: ProgressDialog? = null
    
    @Suppress("UNUSED", "DEPRECATION")
    private val handler: Handler = Handler {
        when (it.what) {
            MSG_DECODE_SUCCESS -> {
                progress?.dismiss()
                val path = it.obj as String
                Toast.makeText(this, "解码成功：$path", Toast.LENGTH_LONG).show()
                binding.textStatus.text = "解码完成：$path"
                loadDecodedFiles()
            }
            MSG_DECODE_FAILED -> {
                progress?.dismiss()
                val error = it.obj as? String ?: "未知错误"
                Toast.makeText(this, "解码失败：$error", Toast.LENGTH_SHORT).show()
                binding.textStatus.text = "解码失败：$error"
            }
            MSG_DECODING -> {
                progress?.setMessage("正在解码...")
            }
            MSG_CONVERTING -> {
                progress?.setMessage("正在转换为MP3...")
            }
        }
        true
    }
    
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        binding = ActivityDecoderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 确保解码输出目录存在
        if (!decodePath.exists()) {
            decodePath.mkdirs()
        }
        
        setupButtons()
        setupRecyclerView()
        loadDecodedFiles()
    }
    
    private fun setupButtons() {
        // 选择文件按钮
        binding.btnSelectFile.setOnClickListener {
            selectSilkFile()
        }
        
        // 文件路径点击也可以选择文件
        binding.editFilePath.setOnClickListener {
            selectSilkFile()
        }
        
        // 解码按钮
        binding.btnDecode.setOnClickListener {
            startDecode()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DecodeAdapter(this, decodedFiles)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.recycleDecoded.addItemDecoration(decoration)
        binding.recycleDecoded.layoutManager = LinearLayoutManager(this)
        binding.recycleDecoded.adapter = adapter
    }
    
    private fun loadDecodedFiles() {
        decodedFiles.clear()
        val files = decodePath.listFiles()
        if (files != null && files.isNotEmpty()) {
            files.filter { it.isFile && it.name.endsWith(".mp3") }
                .sortedByDescending { it.lastModified() }
                .forEach { decodedFiles.add(it) }
        }
        
        adapter?.notifyDataSetChanged()
        
        if (decodedFiles.isEmpty()) {
            binding.textEmpty.visibility = View.VISIBLE
            binding.recycleDecoded.visibility = View.GONE
        } else {
            binding.textEmpty.visibility = View.GONE
            binding.recycleDecoded.visibility = View.VISIBLE
        }
    }
    
    @Suppress("DEPRECATION")
    private fun selectSilkFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        
        try {
            startActivityForResult(Intent.createChooser(intent, "选择 Silk 文件"), REQUEST_SELECT_FILE)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_SELECT_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileUri = uri
                val fileName = getFileName(uri)
                binding.editFilePath.setText(fileName)
                binding.btnDecode.isEnabled = true
                binding.textStatus.text = "已选择：$fileName"
            }
        }
    }
    
    private fun getFileName(uri: Uri): String {
        var name = "未知文件"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name
    }
    
    @Suppress("DEPRECATION")
    private fun startDecode() {
        val uri = selectedFileUri ?: return
        
        progress = ProgressDialog.show(this, "正在处理", "准备中...", true, false)
        
        Thread {
            try {
                // 1. 将输入文件复制到缓存
                val cacheDir = cacheDir
                val inputFile = File(cacheDir, "input.slk")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // 2. 解码 Silk 到 PCM
                handler.sendEmptyMessage(MSG_DECODING)
                val pcmFile = File(cacheDir, "output.pcm")
                SilkCoder.decode(inputFile.absolutePath, pcmFile.absolutePath)
                
                if (!pcmFile.exists() || pcmFile.length() == 0L) {
                    sendError("解码失败，请确保输入的是有效的 Silk 文件")
                    return@Thread
                }
                
                // 3. 转换 PCM 到 MP3
                handler.sendEmptyMessage(MSG_CONVERTING)
                val fileName = getFileName(uri)
                val baseName = fileName.substringBeforeLast(".")
                val outputFile = File(decodePath, "$baseName.mp3")
                
                val fis = FileInputStream(pcmFile)
                val fis2 = FileInputStream(pcmFile)
                val fos = FileOutputStream(outputFile)
                PcmToMp3().convertAudioFiles(fis, fis2, fos)
                
                // 检查输出文件是否生成
                if (outputFile.exists() && outputFile.length() > 0) {
                    val msg = Message()
                    msg.what = MSG_DECODE_SUCCESS
                    msg.obj = outputFile.absolutePath
                    handler.sendMessage(msg)
                } else {
                    sendError("转换MP3失败")
                }
                
                // 清理临时文件
                inputFile.delete()
                pcmFile.delete()
                
            } catch (e: Exception) {
                e.printStackTrace()
                sendError(e.message ?: "未知错误")
            }
        }.start()
    }
    
    private fun sendError(error: String) {
        val msg = Message()
        msg.what = MSG_DECODE_FAILED
        msg.obj = error
        handler.sendMessage(msg)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        private const val REQUEST_SELECT_FILE = 100
        private const val MSG_DECODE_SUCCESS = 1
        private const val MSG_DECODE_FAILED = 2
        private const val MSG_DECODING = 3
        private const val MSG_CONVERTING = 4
    }
}
