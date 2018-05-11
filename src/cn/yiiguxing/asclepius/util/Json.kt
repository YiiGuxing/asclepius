/*
 * Json
 * 
 * Created by Yii.Guxing on 2018/05/11.
 */

@file:Suppress("NOTHING_TO_INLINE")

package cn.yiiguxing.asclepius.util

import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type

inline fun <reified T> File.readJson(gson: Gson = Gson()): T {
    return FileReader(this).use { gson.fromJson(it, T::class.java) }
}

inline fun <T> File.readJson(gson: Gson = Gson(), typeOfT: Type): T {
    return FileReader(this).use { gson.fromJson(it, typeOfT) }
}