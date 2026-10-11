package com.supanga.bros

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

/** Minimal PNG writer (the Android unit-test classpath has no java.awt / ImageIO). */
object Png {
    fun write(file: File, w: Int, h: Int, argb: IntArray, scale: Int = 1) {
        val sw = w * scale
        val sh = h * scale
        val raw = ByteArrayOutputStream()
        DeflaterOutputStream(raw).use { z ->
            val line = ByteArray(1 + sw * 3)
            for (y in 0 until sh) {
                line[0] = 0
                val sy = y / scale
                for (x in 0 until sw) {
                    val c = argb[sy * w + x / scale]
                    line[1 + x * 3] = (c shr 16).toByte()
                    line[2 + x * 3] = (c shr 8).toByte()
                    line[3 + x * 3] = c.toByte()
                }
                z.write(line)
            }
        }
        file.parentFile?.mkdirs()
        DataOutputStream(file.outputStream()).use { out ->
            out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
            val ihdr = ByteArrayOutputStream()
            DataOutputStream(ihdr).apply { writeInt(sw); writeInt(sh); writeByte(8); writeByte(2); writeByte(0); writeByte(0); writeByte(0) }
            chunk(out, "IHDR", ihdr.toByteArray())
            chunk(out, "IDAT", raw.toByteArray())
            chunk(out, "IEND", ByteArray(0))
        }
    }

    private fun chunk(out: DataOutputStream, type: String, data: ByteArray) {
        out.writeInt(data.size)
        val t = type.toByteArray()
        out.write(t)
        out.write(data)
        val crc = CRC32()
        crc.update(t)
        crc.update(data)
        out.writeInt(crc.value.toInt())
    }
}
