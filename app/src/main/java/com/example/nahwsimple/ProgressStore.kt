package com.example.nahwsimple

import android.content.Context
import java.time.LocalDate

/** يحفظ أفضل نتيجة لكل درس وعدد الأيام المتواصلة للدراسة، على الجهاز فقط. */
class ProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    /** أفضل نتيجة (عدد الإجابات الصحيحة)، أو -1 إن لم يُجرَّب الدرس بعد. */
    fun bestScore(lessonId: String): Int = prefs.getInt("best_$lessonId", -1)

    /** يُعدّ الدرس مكتملاً عند الوصول إلى 60% فأكثر. */
    fun isPassed(lessonId: String, total: Int): Boolean {
        val best = bestScore(lessonId)
        return best >= 0 && best * 100 >= total * 60
    }

    fun saveScore(lessonId: String, score: Int) {
        if (score > bestScore(lessonId)) {
            prefs.edit().putInt("best_$lessonId", score).apply()
        }
        registerStudyToday()
    }

    /** مفاتيح الأسئلة التي أُخطئ فيها ولم تُصحَّح بعد. */
    fun wrongKeys(): Set<String> = prefs.getStringSet("wrong", emptySet()) ?: emptySet()

    /** يسجّل الإجابة: الخاطئة تُضاف للمراجعة، والصحيحة تُحذف منها. */
    fun recordAnswer(key: String, correct: Boolean) {
        // نسخة جديدة دائماً: لا يجوز تعديل المجموعة المُرجَعة من SharedPreferences مباشرة
        val set = wrongKeys().toMutableSet()
        val changed = if (correct) set.remove(key) else set.add(key)
        if (changed) prefs.edit().putStringSet("wrong", set).apply()
    }

    fun streak(): Int {
        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong("last_day", -1L)
        val streak = prefs.getInt("streak", 0)
        return if (last == today || last == today - 1) streak else 0
    }

    fun registerStudyToday() {
        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong("last_day", -1L)
        val streak = prefs.getInt("streak", 0)
        val newStreak = when (last) {
            today -> maxOf(streak, 1)
            today - 1 -> streak + 1
            else -> 1
        }
        prefs.edit().putLong("last_day", today).putInt("streak", newStreak).apply()
    }
}
