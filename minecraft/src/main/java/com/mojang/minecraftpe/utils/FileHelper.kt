package com.mojang.minecraftpe.utils

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

object FileHelper {
    @JvmStatic
    fun readFile(path: String): String {
        return File(path).inputStream().readBytes().toString(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun writeToFile(file: File, content: InputStream): Long {
        return content.copyTo(FileOutputStream(file), 1024 * 4)
    }
}
