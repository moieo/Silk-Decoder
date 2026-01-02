package com.xmoieo.silk;
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
 
/**
 * PCM 转 MP3 for Kotlin
 *
 * @author Moieo
 * @since 2021-7-30
 */
class PcmToMp3 {
    /**
     * @param src    待转换文件路径
     * @param target 目标文件路径
     */
    fun convertAudioFiles(fis: FileInputStream, _fis: FileInputStream, fos: FileOutputStream) {
        //计算长度
        var buf = ByteArray(1024 *4)
        var size = fis.read(buf)
        var PCMSize = 0
        while (size != -1) {
            PCMSize += size
            size = fis.read(buf)
        }
        fis.close()
        
        //填入参数，比特率等等。这里用的是16位单声道 8000 hz
        var header: WaveHeader = WaveHeader()
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = PCMSize + (44 - 8)
        header.FmtHdrLeth = 16
        header.BitsPerSample = 16
        header.Channels = 1
        header.FormatTag = 0x0001
        header.SamplesPerSec = 25000
        header.BlockAlign = (header.Channels * header.BitsPerSample / 8).toShort()
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec
        header.DataHdrLeth = PCMSize

        var h = header.getHeader()
        assert(h.size == 44) //WAV标准，头部应该是44字节
        //write header
        fos.write(h, 0, h.size)
        //write data stream
        size = _fis.read(buf)
        while (size != -1) {
            fos.write(buf, 0, size)
            size = _fis.read(buf)
        }
        fis.close()
        fos.close()
    }
}
