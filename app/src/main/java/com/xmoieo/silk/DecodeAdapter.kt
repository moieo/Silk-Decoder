package com.xmoieo.silk;
import java.io.File
import java.util.Date
import android.view.View
import android.app.AlertDialog
import android.view.ViewGroup
import android.content.Context
import android.widget.TextView
import android.view.LayoutInflater
import java.text.SimpleDateFormat
import androidx.recyclerview.widget.RecyclerView
import com.xmoieo.silk.databinding.DecodeItemBinding

class DecodeAdapter(var context_: Context, var dataList: MutableList<File>): RecyclerView.Adapter<DecodeAdapter.ViewHolder>() {

    var context = context_
    
    @Suppress("SimpleDateFormat")
    private var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    private lateinit var binding: DecodeItemBinding
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = DecodeItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = dataList[holder.bindingAdapterPosition]
        holder.title?.text = file.name
        holder.date?.text = sdf.format(Date(file.lastModified()))
        holder.size?.text = MxRecyclerAdapter.getFileSize(file.length().toFloat())
        holder.root?.setOnClickListener {
            val items = arrayOf("播放", "删除")
            //var path = dataList[holder].getUri().getPath()
            val dialog_ = AlertDialog.Builder(context)
            dialog_.setItems(items) { _, witch ->
                when(witch) {
                    0 -> {
                        PlayerUtils(context, file)
                    }
                    1 -> {
                        file.delete()
                        if (file.exists()) {
                            dataList.remove(file)
                            notifyDataSetChanged()
                        }
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
            var binding = DecodeItemBinding.bind(item)
            title = binding.title
            date = binding.date
            size = binding.size
            root = item
        }
    }
}
