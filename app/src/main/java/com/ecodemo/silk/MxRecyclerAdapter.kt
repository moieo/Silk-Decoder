package com.ecodemo.silk;

import java.io.File
import java.net.URI
import java.util.Date
import java.lang.Math
import android.net.Uri
import android.util.Log
import java.lang.Process
import java.lang.Runtime
import android.view.View
import android.os.Handler
import java.io.InputStream
import android.os.Message
import java.io.FileDescriptor
import android.widget.Toast
import java.io.FileInputStream
import android.content.Intent
import java.io.BufferedReader
import android.app.AlertDialog
import android.os.Environment
import android.view.ViewGroup
import android.widget.TextView
import android.content.Context
import java.io.FileOutputStream
import java.io.InputStreamReader
import android.widget.ImageView
import android.view.LayoutInflater
import android.app.ProgressDialog
import java.text.SimpleDateFormat
import android.os.ParcelFileDescriptor
import android.content.DialogInterface
import android.content.ComponentName
import androidx.recyclerview.widget.RecyclerView
import androidx.documentfile.provider.DocumentFile
import com.ecodemo.silk.databinding.ListItemBinding

class MxRecyclerAdapter(var context_: Context, var dataList: MutableList<DocumentFile>): RecyclerView.Adapter<MxRecyclerAdapter.ViewHolder>() {
    
    private lateinit var binding: ListItemBinding
    var context = context_
    
    @Suppress("DEPRECATION")
    var progress: ProgressDialog? = null
    
    @Suppress("DEPRECATION")
    private val app_cache: File = File(Environment.getExternalStorageDirectory(), "Silk解码器/解码")
    
    private var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    @Suppress("UNUSED", "DEPRECATION")
    var handler: Handler = Handler {
        when(it.what){
            0 -> {
                progress?.dismiss()
                Toast.makeText(context, "解码失败", Toast.LENGTH_SHORT).show()
            }
            201 -> {
                progress?.dismiss()
                var path = it.obj as String
                Toast.makeText(context, "解码成功：已保存在 $path", Toast.LENGTH_LONG).show()
            }
            202 -> {
                progress?.dismiss()
                PlayerUtils(context, it.obj as File)
            }
        }
        false
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
        var path = dataList[holder.getAdapterPosition()].getUri().getPath()
        
        /* 获取文件大小 */
        var slk = context.getContentResolver()?.openFileDescriptor(dataList[holder.getAdapterPosition()].getUri(), "rw")!!//通过uri获取ParcelFileDescriptor对象
        var fileDescriptor = slk.getFileDescriptor()//得到文件描述
        var fis_size = FileInputStream(fileDescriptor)
        var size_text = getFileSize(fis_size.getChannel().size().toFloat())
        holder.size?.text=size_text
        fis_size.close()
        
        holder.date?.text = sdf.format(Date(dataList[holder.getAdapterPosition()].lastModified()))
        
        holder.title?.text = path?.substring(path.lastIndexOf("/")+1, path.length)!!
        
        holder.root?.setOnClickListener {
            var items = arrayOf("播放", "解码"/*,"删除"*/)
            //var path = dataList[holder].getUri().getPath()
            var dialog_ = AlertDialog.Builder(context)
            dialog_.setItems(items, DialogInterface.OnClickListener(){_, which ->
                when(which) {
                    0 -> {
                        decoder_to_file(dataList[holder.getAdapterPosition()].getUri(), context.getCacheDir(), OnDecode { mp3 ->
                            var msg = Message()
                            msg.what = 202
                            msg.obj = mp3
                            handler.sendMessage(msg)
                        })
                    }
                    1 -> {
                        decoder_to_file(dataList[holder.getAdapterPosition()].getUri(), app_cache, OnDecode { mp3 ->
                            var msg = Message()
                            msg.what = 201
                            msg.obj = mp3.getAbsolutePath()
                            handler.sendMessage(msg)
                        })
                    }
                }
            })
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
    fun decoder_to_file(source: Uri, cache: File, de: OnDecode?) {
        var path = source.getPath()
        progress = ProgressDialog.show(context, "正在解码，请稍候...", null, true, false);
        if(!cache.exists()) {
            cache.mkdirs()
        }
        Thread {
            var cache_file = decoder(source)
            var pcm = File(cache_file.parentFile, "cache.pcm")
            var mp3 = File(cache, path?.substring(path?.lastIndexOf("/")+1, path?.lastIndexOf(".")) + ".mp3")
            SilkCoder.decode(cache_file.path, pcm.path)
            //exec("decoder " + cache_file.path + " " + pcm.path)
            //var is_ = process.getInputStream()
            //var reader = BufferedReader(InputStreamReader(is_))
            if(!pcm.exists()){
                handler.sendEmptyMessage(0)
                false
            } else {
                var fis: FileInputStream? = FileInputStream(pcm)
                var _fis: FileInputStream? = FileInputStream(pcm)
                var fos: FileOutputStream? = FileOutputStream(mp3)
                PcmToMp3().convertAudioFiles(fis!!, _fis!!, fos!!)
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
