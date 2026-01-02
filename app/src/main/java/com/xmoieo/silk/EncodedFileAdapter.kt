package com.xmoieo.silk

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import android.view.View
import android.app.AlertDialog
import android.app.ProgressDialog
import android.view.ViewGroup
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import android.os.Handler
import android.os.Message
import java.text.SimpleDateFormat
import androidx.recyclerview.widget.RecyclerView
import com.xmoieo.silk.databinding.DecodeItemBinding

/**
 * 编码文件适配器
 * 用于显示编码后的 Silk 文件列表，支持先解码再播放
 */
class EncodedFileAdapter(
    private val context: Context,
    private val dataList: MutableList<File>
) : RecyclerView.Adapter<EncodedFileAdapter.ViewHolder>() {

    @Suppress("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    @Suppress("DEPRECATION")
    private var progress: ProgressDialog? = null
    
    @Suppress("UNUSED", "DEPRECATION")
    private val handler: Handler = Handler {
        when (it.what) {
            MSG_PLAY_READY -> {
                progress?.dismiss()
                val file = it.obj as File
                PlayerUtils(context, file)
            }
            MSG_PLAY_FAILED -> {
                progress?.dismiss()
                Toast.makeText(context, "播放失败：${it.obj}", Toast.LENGTH_SHORT).show()
            }
        }
        true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DecodeItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = dataList[holder.bindingAdapterPosition]
        holder.title?.text = file.name
        holder.date?.text = sdf.format(Date(file.lastModified()))
        holder.size?.text = MxRecyclerAdapter.getFileSize(file.length().toFloat())
        
        holder.root?.setOnClickListener {
            val items = arrayOf("播放", "删除")
            AlertDialog.Builder(context)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> playFile(file)
                        1 -> deleteFile(holder.bindingAdapterPosition, file)
                    }
                }
                .show()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun playFile(file: File) {
        // Silk 文件需要先解码才能播放
        progress = ProgressDialog.show(context, "准备播放", "正在解码...", true, false)
        
        Thread {
            try {
                val cacheDir = context.cacheDir
                val pcmFile = File(cacheDir, "play_cache.pcm")
                val mp3File = File(cacheDir, "play_cache.mp3")
                
                // 解码 Silk 到 PCM
                SilkCoder.decode(file.absolutePath, pcmFile.absolutePath)
                
                if (!pcmFile.exists() || pcmFile.length() == 0L) {
                    sendError("解码失败")
                    return@Thread
                }
                
                // 转换 PCM 到 MP3
                val fis = FileInputStream(pcmFile)
                val fis2 = FileInputStream(pcmFile)
                val fos = FileOutputStream(mp3File)
                PcmToMp3().convertAudioFiles(fis, fis2, fos)
                
                if (mp3File.exists() && mp3File.length() > 0) {
                    val msg = Message()
                    msg.what = MSG_PLAY_READY
                    msg.obj = mp3File
                    handler.sendMessage(msg)
                } else {
                    sendError("转换失败")
                }
                
                // 清理 PCM 临时文件
                pcmFile.delete()
                
            } catch (e: Exception) {
                e.printStackTrace()
                sendError(e.message ?: "未知错误")
            }
        }.start()
    }
    
    private fun deleteFile(position: Int, file: File) {
        if (file.delete()) {
            dataList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, dataList.size)
        } else {
            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sendError(error: String) {
        val msg = Message()
        msg.what = MSG_PLAY_FAILED
        msg.obj = error
        handler.sendMessage(msg)
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        var title: TextView? = null
        var date: TextView? = null
        var size: TextView? = null
        var root: View? = null
        
        init {
            val binding = DecodeItemBinding.bind(item)
            title = binding.title
            date = binding.date
            size = binding.size
            root = item
        }
    }
    
    companion object {
        private const val MSG_PLAY_READY = 1
        private const val MSG_PLAY_FAILED = 2
    }
}
