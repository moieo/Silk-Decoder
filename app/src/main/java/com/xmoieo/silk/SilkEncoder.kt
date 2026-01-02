package com.xmoieo.silk

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
import android.widget.ArrayAdapter
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.provider.OpenableColumns
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.xmoieo.silk.databinding.ActivityEncoderBinding

/**
 * Silk 编码器 Activity
 * 支持将 PCM/WAV 音频文件编码为 Silk 格式
 */
class SilkEncoder : Activity() {

    private lateinit var binding: ActivityEncoderBinding
    
    @Suppress("DEPRECATION")
    private val encodePath: File = File(Environment.getExternalStorageDirectory(), "Silk解码器/编码")
    
    private var selectedFileUri: Uri? = null
    private var selectedFilePath: String? = null
    
    private var encodedFiles = mutableListOf<File>()
    private var adapter: EncodedFileAdapter? = null
    
    // 采样率选项 (Hz)
    private val sampleRates = arrayOf(8000, 12000, 16000, 24000, 48000)
    private val sampleRateLabels = arrayOf("8000 Hz", "12000 Hz", "16000 Hz", "24000 Hz", "48000 Hz")
    
    // 比特率选项 (bps)
    private val bitRates = arrayOf(10000, 15000, 20000, 25000, 30000, 40000)
    private val bitRateLabels = arrayOf("10 kbps", "15 kbps", "20 kbps", "25 kbps", "30 kbps", "40 kbps")
    
    @Suppress("DEPRECATION")
    private var progress: ProgressDialog? = null
    
    @Suppress("UNUSED", "DEPRECATION")
    private val handler: Handler = Handler {
        when (it.what) {
            MSG_ENCODE_SUCCESS -> {
                progress?.dismiss()
                val path = it.obj as String
                Toast.makeText(this, "编码成功：$path", Toast.LENGTH_LONG).show()
                binding.textStatus.text = "编码完成：$path"
                loadEncodedFiles()
            }
            MSG_ENCODE_FAILED -> {
                progress?.dismiss()
                val error = it.obj as? String ?: "未知错误"
                Toast.makeText(this, "编码失败：$error", Toast.LENGTH_SHORT).show()
                binding.textStatus.text = "编码失败：$error"
            }
            MSG_CONVERT_PCM -> {
                progress?.setMessage("正在转换为PCM格式...")
            }
            MSG_ENCODING -> {
                progress?.setMessage("正在编码为Silk格式...")
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
        
        binding = ActivityEncoderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 确保编码输出目录存在
        if (!encodePath.exists()) {
            encodePath.mkdirs()
        }
        
        setupSpinners()
        setupButtons()
        setupRecyclerView()
        loadEncodedFiles()
    }
    
    private fun setupSpinners() {
        // 采样率选择器
        val sampleRateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sampleRateLabels)
        sampleRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSampleRate.adapter = sampleRateAdapter
        binding.spinnerSampleRate.setSelection(3) // 默认 24000 Hz
        
        // 比特率选择器
        val bitRateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bitRateLabels)
        bitRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBitRate.adapter = bitRateAdapter
        binding.spinnerBitRate.setSelection(3) // 默认 25 kbps
    }
    
    private fun setupButtons() {
        // 选择文件按钮
        binding.btnSelectFile.setOnClickListener {
            selectAudioFile()
        }
        
        // 文件路径点击也可以选择文件
        binding.editFilePath.setOnClickListener {
            selectAudioFile()
        }
        
        // 编码按钮
        binding.btnEncode.setOnClickListener {
            startEncode()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = EncodedFileAdapter(this, encodedFiles)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.recycleEncoded.addItemDecoration(decoration)
        binding.recycleEncoded.layoutManager = LinearLayoutManager(this)
        binding.recycleEncoded.adapter = adapter
    }
    
    private fun loadEncodedFiles() {
        encodedFiles.clear()
        val files = encodePath.listFiles()
        if (files != null && files.isNotEmpty()) {
            files.filter { it.isFile && (it.name.endsWith(".slk") || it.name.endsWith(".silk")) }
                .sortedByDescending { it.lastModified() }
                .forEach { encodedFiles.add(it) }
        }
        
        adapter?.notifyDataSetChanged()
        
        if (encodedFiles.isEmpty()) {
            binding.textEmpty.visibility = View.VISIBLE
            binding.recycleEncoded.visibility = View.GONE
        } else {
            binding.textEmpty.visibility = View.GONE
            binding.recycleEncoded.visibility = View.VISIBLE
        }
    }
    
    @Suppress("DEPRECATION")
    private fun selectAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        
        // 也支持选择 pcm 文件
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "application/octet-stream"))
        
        try {
            startActivityForResult(Intent.createChooser(intent, "选择音频文件"), REQUEST_SELECT_FILE)
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
                binding.btnEncode.isEnabled = true
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
    private fun startEncode() {
        val uri = selectedFileUri ?: return
        
        val sampleRate = sampleRates[binding.spinnerSampleRate.selectedItemPosition]
        val bitRate = bitRates[binding.spinnerBitRate.selectedItemPosition]
        val isTencent = binding.checkboxTencent.isChecked
        
        progress = ProgressDialog.show(this, "正在处理", "准备中...", true, false)
        
        Thread {
            try {
                // 1. 将输入文件复制到缓存
                handler.sendEmptyMessage(MSG_CONVERT_PCM)
                val cacheDir = cacheDir
                val inputFile = File(cacheDir, "input_audio")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // 2. 检查文件格式并转换为 PCM（如果需要）
                val pcmFile = convertToPcm(inputFile, sampleRate)
                if (pcmFile == null) {
                    sendError("无法转换音频格式，请确保输入的是有效的音频文件")
                    return@Thread
                }
                
                // 3. 编码为 Silk
                handler.sendEmptyMessage(MSG_ENCODING)
                val fileName = getFileName(uri)
                val baseName = fileName.substringBeforeLast(".")
                val outputFile = File(encodePath, "$baseName.slk")
                
                SilkCoder.encode(
                    pcmFile.absolutePath,
                    outputFile.absolutePath,
                    isTencent,
                    sampleRate,
                    24000,
                    (20 * sampleRate) / 1000,
                    0,
                    0,
                    0,
                    2,
                    bitRate
                )
                
                // 检查输出文件是否生成
                if (outputFile.exists() && outputFile.length() > 0) {
                    val msg = Message()
                    msg.what = MSG_ENCODE_SUCCESS
                    msg.obj = outputFile.absolutePath
                    handler.sendMessage(msg)
                } else {
                    sendError("编码失败，输出文件为空")
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
    
    /**
     * 将输入文件转换为 PCM 格式
     * 使用 MediaExtractor + MediaCodec 支持 MP3, FLAC, WAV, AAC 等格式
     * 输出: 16位单声道 PCM，采样率与目标匹配
     */
    private fun convertToPcm(inputFile: File, targetSampleRate: Int): File? {
        val pcmFile = File(cacheDir, "temp.pcm")
        val tempDecodedFile = File(cacheDir, "temp_decoded.pcm")
        
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)
            
            // 找到音频轨道
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            
            if (audioTrackIndex < 0 || audioFormat == null) {
                extractor.release()
                return null
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val sourceSampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val sourceChannels = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            
            android.util.Log.d("SilkEncoder", "Source: mime=$mime, sampleRate=$sourceSampleRate, channels=$sourceChannels")
            
            // 解码音频到 PCM
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()
            
            val bufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10000L
            var isEOS = false
            var sawOutputEOS = false
            
            FileOutputStream(tempDecodedFile).use { fos ->
                while (!sawOutputEOS) {
                    // 填充输入缓冲区
                    if (!isEOS) {
                        val inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = decoder.getInputBuffer(inputBufferIndex) ?: continue
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEOS = true
                            } else {
                                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    
                    // 读取输出缓冲区
                    val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val pcmBytes = ByteArray(bufferInfo.size)
                            outputBuffer.get(pcmBytes)
                            fos.write(pcmBytes)
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                        
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true
                        }
                    }
                }
            }
            
            decoder.stop()
            decoder.release()
            extractor.release()
            
            // 现在处理声道和采样率转换
            val decodedData = tempDecodedFile.readBytes()
            val processedData = processAudioData(decodedData, sourceSampleRate, sourceChannels, targetSampleRate)
            
            FileOutputStream(pcmFile).use { fos ->
                fos.write(processedData)
            }
            
            tempDecodedFile.delete()
            return pcmFile
            
        } catch (e: Exception) {
            android.util.Log.e("SilkEncoder", "convertToPcm error", e)
            e.printStackTrace()
            
            // 回退到简单方法处理 WAV 文件
            return fallbackConvertWav(inputFile, targetSampleRate, pcmFile)
        }
    }
    
    /**
     * 处理 PCM 数据：立体声转单声道 + 重采样
     */
    private fun processAudioData(data: ByteArray, sourceSampleRate: Int, sourceChannels: Int, targetSampleRate: Int): ByteArray {
        // 首先转换为 short 数组 (16-bit PCM)
        val shortBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = ShortArray(shortBuffer.remaining())
        shortBuffer.get(samples)
        
        // 步骤1: 立体声转单声道
        val monoSamples = if (sourceChannels > 1) {
            val monoSize = samples.size / sourceChannels
            ShortArray(monoSize) { i ->
                // 取所有声道的平均值
                var sum = 0
                for (ch in 0 until sourceChannels) {
                    sum += samples[i * sourceChannels + ch]
                }
                (sum / sourceChannels).toShort()
            }
        } else {
            samples
        }
        
        // 步骤2: 重采样
        val resampledSamples = if (sourceSampleRate != targetSampleRate) {
            resample(monoSamples, sourceSampleRate, targetSampleRate)
        } else {
            monoSamples
        }
        
        // 转换回 byte 数组
        val result = ByteArray(resampledSamples.size * 2)
        val resultBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in resampledSamples) {
            resultBuffer.putShort(sample)
        }
        
        return result
    }
    
    /**
     * 线性插值重采样
     */
    private fun resample(input: ShortArray, inputRate: Int, outputRate: Int): ShortArray {
        if (inputRate == outputRate) return input
        
        val ratio = inputRate.toDouble() / outputRate.toDouble()
        val outputLength = (input.size / ratio).toInt()
        val output = ShortArray(outputLength)
        
        for (i in 0 until outputLength) {
            val srcIndex = i * ratio
            val index0 = srcIndex.toInt().coerceIn(0, input.size - 1)
            val index1 = (index0 + 1).coerceIn(0, input.size - 1)
            val frac = srcIndex - index0
            
            // 线性插值
            val sample0 = input[index0].toInt()
            val sample1 = input[index1].toInt()
            output[i] = (sample0 + (sample1 - sample0) * frac).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        return output
    }
    
    /**
     * 回退方法：简单处理 WAV 文件
     */
    private fun fallbackConvertWav(inputFile: File, targetSampleRate: Int, outputFile: File): File? {
        try {
            FileInputStream(inputFile).use { fis ->
                val header = ByteArray(44)
                if (fis.read(header) < 44) return null
                
                val riff = String(header, 0, 4)
                val wave = String(header, 8, 4)
                
                if (riff != "RIFF" || wave != "WAVE") return null
                
                // 解析 WAV 头
                val wavBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val channels = wavBuffer.getShort(22).toInt()
                val sampleRate = wavBuffer.getInt(24)
                val bitsPerSample = wavBuffer.getShort(34).toInt()
                
                android.util.Log.d("SilkEncoder", "WAV fallback: channels=$channels, sampleRate=$sampleRate, bits=$bitsPerSample")
                
                // 读取 PCM 数据
                val pcmData = fis.readBytes()
                
                // 处理声道和采样率
                val processedData = processAudioData(pcmData, sampleRate, channels, targetSampleRate)
                
                FileOutputStream(outputFile).use { fos ->
                    fos.write(processedData)
                }
                
                return outputFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun sendError(error: String) {
        val msg = Message()
        msg.what = MSG_ENCODE_FAILED
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
        private const val MSG_ENCODE_SUCCESS = 1
        private const val MSG_ENCODE_FAILED = 2
        private const val MSG_CONVERT_PCM = 3
        private const val MSG_ENCODING = 4
    }
}
