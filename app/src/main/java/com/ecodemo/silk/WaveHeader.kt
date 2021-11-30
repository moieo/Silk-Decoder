package com.ecodemo.silk;
import java.io.ByteArrayOutputStream
import java.io.IOException

class WaveHeader {

    public var fileID: CharArray = charArrayOf('R', 'I', 'F', 'F')
    public var fileLength: Int = 0
    public var wavTag: CharArray = charArrayOf('W', 'A', 'V', 'E')
    public var FmtHdrID: CharArray= charArrayOf('f', 'm', 't', ' ')
    public var FmtHdrLeth: Int = 0
    public var FormatTag: Short = 0
    public var Channels: Short = 0
    public var SamplesPerSec: Int = 0
    public var AvgBytesPerSec: Int = 0
    public var BlockAlign: Short = 0
    public var BitsPerSample: Short = 0
    public var DataHdrID: CharArray = charArrayOf('d','a','t','a')
    public var DataHdrLeth: Int = 0

    fun getHeader(): ByteArray {
        var bos: ByteArrayOutputStream = ByteArrayOutputStream()
        WriteChar(bos, fileID)
        WriteInt(bos, fileLength)
        WriteChar(bos, wavTag)
        WriteChar(bos, FmtHdrID)
        WriteInt(bos,FmtHdrLeth)
        WriteShort(bos,FormatTag)
        WriteShort(bos,Channels)
        WriteInt(bos,SamplesPerSec)
        WriteInt(bos,AvgBytesPerSec)
        WriteShort(bos,BlockAlign)
        WriteShort(bos,BitsPerSample)
        WriteChar(bos,DataHdrID)
        WriteInt(bos,DataHdrLeth)
        bos.flush()
        var r = bos.toByteArray()
        bos.close()
        return r
    }

    fun WriteShort(bos: ByteArrayOutputStream, s: Short) {
        var i: Int = s.toInt()
        var mybyte = ByteArray(2)
        mybyte[1] = ((i shl 16) shr 24).toByte()
        mybyte[0] = ((i shl 24) shr 24).toByte()
        bos.write(mybyte)
    }


    fun WriteInt(bos: ByteArrayOutputStream, n: Int) {
        var buf = ByteArray(4)
        buf[3] = (n shr 24).toByte()
        buf[2] = ((n shl 8) shr 24).toByte()
        buf[1] = ((n shl 16) shr 24).toByte()
        buf[0] = ((n shl 24) shr 24).toByte()
        bos.write(buf)
    }

    fun WriteChar(bos: ByteArrayOutputStream, id: CharArray) {
        id.forEach {
            bos.write(it.toInt())
        }
    }
}
