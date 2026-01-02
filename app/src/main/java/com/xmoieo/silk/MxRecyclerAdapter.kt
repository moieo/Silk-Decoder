package com.xmoieo.silk;

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.xmoieo.silk.databinding.ListItemBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class MxRecyclerAdapter(var context_: Context, var dataList: MutableList<DocumentFile>): RecyclerView.Adapter<MxRecyclerAdapter.ViewHolder>() {
    
    private lateinit var binding: ListItemBinding
    var context = context_
    
    @Suppress("DEPRECATION")
    var progress: ProgressDialog? = null
    
    @Suppress("DEPRECATION")
    private val app_cache: File = File(Environment.getExternalStorageDirectory(), "Silk解码器/解码")
    
    @SuppressLint("SimpleDateFormat")
    private var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    @Suppress("UNUSED", "DEPRECATION")
    val handler: Handler = Handler {
        when(it.what){
            0 -> {
                progress?.dismiss()
                Toast.makeText(context, "解码失败", Toast.LENGTH_SHORT).show()
            }
            201 -> {
                progress?.dismiss()
                val path = it.obj as String
                Toast.makeText(context, "解码成功：已保存在 $path", Toast.LENGTH_LONG).show()
            }
            202 -> {
                progress?.dismiss()
                PlayerUtils(context, it.obj as File)
            }
        }
        true
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ListItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //Log.d("wang","position,:$position,${dataList[position]}")
        val path = dataList[holder.bindingAdapterPosition].uri.path
        
        /* 获取文件大小 */
        val slk = context.contentResolver?.openFileDescriptor(dataList[holder.bindingAdapterPosition].uri, "rw")!! //通过uri获取ParcelFileDescriptor对象
        val fileDescriptor = slk.fileDescriptor//得到文件描述
        val fis_size = FileInputStream(fileDescriptor)
        val size_text = getFileSize(fis_size.channel.size().toFloat())
        holder.size?.text = size_text
        fis_size.close()
        
        holder.date?.text = sdf.format(Date(dataList[holder.bindingAdapterPosition].lastModified()))
        
        holder.title?.text = path?.substring(path.lastIndexOf("/")+1, path.length)!!
        
        holder.root?.setOnClickListener {
            val items = arrayOf("播放", "解码", "删除")
            //var path = dataList[holder].getUri().getPath()
            val dialog_ = AlertDialog.Builder(context)
            dialog_.setItems(items) { _, which ->
                when(which) {
                    0 -> {
                        decoder_to_file(dataList[holder.bindingAdapterPosition].uri, context.cacheDir, OnDecode { mp3 ->
                            val msg = Message()
                            msg.what = 202
                            msg.obj = mp3
                            handler.sendMessage(msg)
                        })
                    }
                    1 -> {
                        if (File(context.filesDir, "ffmpeg").exists()) {
                            ffmpeg_decoder(dataList[holder.bindingAdapterPosition].uri, app_cache, OnDecode { mp3 ->
                                val msg = Message()
                                msg.what = 201
                                msg.obj = mp3.absolutePath
                                handler.sendMessage(msg)
                            })
                        } else {
                            decoder_to_file(dataList[holder.bindingAdapterPosition].uri, app_cache, OnDecode { mp3 ->
                                val msg = Message()
                                msg.what = 201
                                msg.obj = mp3.absolutePath
                                handler.sendMessage(msg)
                            })
                        }
                    }
                    2 -> {
                        val file: DocumentFile = dataList[holder.bindingAdapterPosition]
                        file.delete()
                        dataList.remove(file)
                        notifyDataSetChanged()
                    }
                }
            }
            dialog_.show()
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        var title:TextView? = null
        var date: TextView? = null
        var size: TextView? = null
        var root: View? = null
        init {
            var binding = ListItemBinding.bind(item)
            title = binding.title
            date = binding.date
            size = binding.size
            root = item
        }
    }
    
    fun ffmpeg_decoder(source: Uri, cache: File, de: OnDecode?) {
        val items = arrayOf("普通解码", "Mp3", "Wav", "Amr", "Flac", "Mp4")
        //var path = dataList[holder].getUri().getPath()
        val dialog_ = AlertDialog.Builder(context)
        dialog_.setItems(items) { _, which ->
            when(which) {
                0 -> {
                    decoder_to_file(source, cache, de!!)
                }
            }
        }
        dialog_.show()
    }
    
    fun decoder(uri: Uri?): File {
        var cache_dir = context.getCacheDir()
        var cache_file = File(cache_dir, "cache.slk");
        
        var slk = context.getContentResolver().openFileDescriptor(uri!!, "rw")//通过uri获取ParcelFileDescriptor对象
        var fileDescriptor = slk?.getFileDescriptor()//得到文件描述
        var fis = FileInputStream(fileDescriptor!!)//文件输入流
        var fos = FileOutputStream(cache_file)
        var read: Int
        do {
            read = fis.read()
            if(read != -1){
                fos.write(read)
            } else {
                break
            }
        } while(true)
        fis.close()
        fos.close()
        slk?.close()
        return cache_file
    }
    
    /* 解码 */
    @Suppress("DEPRECATION")
    fun decoder_to_file(source: Uri, cache: File, de: OnDecode?) {
        val path = source.path
        progress = ProgressDialog.show(context, "正在解码，请稍候...", null, true, false);
        if(!cache.exists()) {
            cache.mkdirs()
        }
        Thread {
            val cache_file = decoder(source)
            val pcm = File(cache_file.parentFile, "cache.pcm")
            val fileName = path?.let { it.substring(it.lastIndexOf("/") + 1, it.lastIndexOf(".")) } ?: "output"
            val mp3 = File(cache, "$fileName.mp3")
            SilkCoder.decode(cache_file.path, pcm.path)
            //exec("decoder " + cache_file.path + " " + pcm.path)
            //var is_ = process.getInputStream()
            //var reader = BufferedReader(InputStreamReader(is_))
            if(!pcm.exists()){
                handler.sendEmptyMessage(0)
                return@Thread
            } else {
                val fis = FileInputStream(pcm)
                val _fis = FileInputStream(pcm)
                val fos = FileOutputStream(mp3)
                PcmToMp3().convertAudioFiles(fis, _fis, fos)
                de?.onFinish(mp3)
            }
        }.start()
    }
    
    fun interface OnDecode{
        fun onFinish(mp3: File)
    }
    
    companion object {
        fun getFileSize(sizeBytes: Float): String{
            var result = Math.abs(sizeBytes)
            var suffix = "B"
            if(result > 1024){
                suffix = "KB"
                result = result / 1024
            }
            if(result > 1024){
                suffix = "MB"
                result = result / 1024
            }
            if(result > 1024){
                suffix = "GB"
                result = result / 1024
            }
            if(result > 1024){
                suffix = "TB"
                result = result / 1024
            }
            if (result > 1024){
                suffix = "PB"
                result = result / 1024
            }
            return "%.2f".format(result) + suffix
        }
    }
}
