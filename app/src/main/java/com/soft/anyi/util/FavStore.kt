package com.soft.anyi.util

import android.content.Context
import com.soft.anyi.SoftApp

/**
 * 收藏 / 历史本地存储（对应 myfav 模块，SharedPreferences JSON 实现）。
 */
object FavStore {
    private const val SP = "myfav"
    private const val KEY_FAV = "favs"
    private const val KEY_HIS = "history"

    private fun sp() = SoftApp.ctx().getSharedPreferences(SP, Context.MODE_PRIVATE)

    private fun read(key: String): MutableList<String> {
        val raw = sp().getString(key, "[]") ?: "[]"
        return try {
            val a = org.json.JSONArray(raw)
            (0 until a.length()).mapTo(mutableListOf()) { a.getString(it) }
        } catch (_: Exception) { mutableListOf() }
    }

    private fun write(key: String, list: List<String>) {
        val a = org.json.JSONArray()
        list.forEach { a.put(it) }
        sp().edit().putString(key, a.toString()).apply()
    }

    fun isFav(id: Int): Boolean = read(KEY_FAV).contains(id.toString())

    /** 返回切换后的状态 */
    fun toggleFav(id: Int): Boolean {
        val list = read(KEY_FAV)
        val s = id.toString()
        return if (list.contains(s)) { list.remove(s); write(KEY_FAV, list); false }
        else { list.add(0, s); write(KEY_FAV, list); true }
    }

    fun favIds(): List<Int> = read(KEY_FAV).mapNotNull { it.toIntOrNull() }

    fun addHistory(id: Int) {
        val list = read(KEY_HIS)
        val s = id.toString()
        list.remove(s)
        list.add(0, s)
        write(KEY_HIS, list.take(100))
    }

    fun historyIds(): List<Int> = read(KEY_HIS).mapNotNull { it.toIntOrNull() }

    fun clearHistory() = write(KEY_HIS, emptyList())
}

/**
 * 简单 KV 偏好（对应 SharedPreferencesAsync keyValue）。
 */
object Prefs {
    private fun sp() = SoftApp.ctx().getSharedPreferences("kv", Context.MODE_PRIVATE)

    fun bool(key: String, def: Boolean = false) = sp().getBoolean(key, def)
    fun setBool(key: String, v: Boolean) = sp().edit().putBoolean(key, v).apply()
    fun str(key: String, def: String = "") = sp().getString(key, def) ?: def
    fun setStr(key: String, v: String) = sp().edit().putString(key, v).apply()
    fun int(key: String, def: Int = 0) = sp().getInt(key, def)
    fun setInt(key: String, v: Int) = sp().edit().putInt(key, v).apply()
}
