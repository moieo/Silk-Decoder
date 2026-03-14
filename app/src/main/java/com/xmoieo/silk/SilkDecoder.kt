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
import android.app.AlertDialog
import android.app.ProgressDialog
import android.widget.Toast
import android.content.Intent
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.documentfile.provider.DocumentFile
import com.xmoieo.silk.databinding.ActivityDecoderBinding

class SilkDecoder : Activity() {

    private lateinit var binding: ActivityDecoderBinding

    @Suppress("DEPRECATION")
    private val decodePath: File = File(Environment.getExternalStorageDirectory(), "Silk解码器/解码")

    private var selectedFileUri: Uri? = null
    private var selectedTreeUri: Uri? = null

    private var decodedFiles = mutableListOf<File>()
    private var adapter: DecodeAdapter? = null

    @Suppress("UNUSED", "DEPRECATION")
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

        // 设置状态栏为浅色模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Android 16 (API 36) 使用新的状态栏处理方式
        if (Build.VERSION.SDK_INT >= 36) {
            window.decorView.setFitsSystemWindows(true)
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
        // Android 16 (API 36) 需要使用 ACTION_OPEN_DOCUMENT_TREE
        if (Build.VERSION.SDK_INT >= 36) {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                
                // 设置初始目录到外部存储根目录
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A")
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                }
                
                startActivityForResult(intent, REQUEST_SELECT_TREE)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开文件选择器: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Android 15 及以下使用 ACTION_GET_CONTENT
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            try {
                startActivityForResult(Intent.createChooser(intent, "选择 Silk 文件"), REQUEST_SELECT_FILE)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFileSelectionDialog(treeUri: Uri) {
        selectedTreeUri = treeUri
        val tree = DocumentFile.fromTreeUri(this, treeUri) ?: return
        
        // 获取目录下的所有文件
        val files = tree.listFiles()
        val silkFiles = files?.filter { 
            it.name?.lowercase()?.endsWith(".slk") == true || 
            it.name?.lowercase()?.endsWith(".silk") == true 
        } ?: emptyList()
        
        if (silkFiles.isEmpty()) {
            Toast.makeText(this, "所选目录中没有找到Silk文件", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 显示文件列表对话框
        val fileNames = silkFiles.map { it.name ?: "未知文件" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择Silk文件")
            .setItems(fileNames) { _, which ->
                val selectedFile = silkFiles[which]
                selectedFileUri = selectedFile.uri
                binding.editFilePath.setText(selectedFile.name)
                binding.btnDecode.isEnabled = true
                binding.textStatus.text = "已选择：${selectedFile.name}"
            }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SELECT_TREE && resultCode == RESULT_OK) {
            // Android 16: 处理目录选择
            data?.data?.let { treeUri ->
                // 持久化权限
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, data.flags and flags)
                
                // 显示文件选择对话框
                showFileSelectionDialog(treeUri)
            }
        } else if (requestCode == REQUEST_SELECT_FILE && resultCode == RESULT_OK) {
            // Android 15 及以下: 处理文件选择
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
        private const val REQUEST_SELECT_TREE = 101
        private const val MSG_DECODE_SUCCESS = 1
        private const val MSG_DECODE_FAILED = 2
        private const val MSG_DECODING = 3
        private const val MSG_CONVERTING = 4
    }
}
