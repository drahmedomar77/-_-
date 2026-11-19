package com.example.nahwsimple

import android.content.Context
import org.json.JSONObject

data class Section(val heading: String, val text: String)

data class Example(val text: String, val parse: String)

data class Question(
    val text: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String
)

data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val sections: List<Section>,
    val examples: List<Example>,
    val questions: List<Question>
)

/** مفتاح ثابت للسؤال مبني على نصه، فلا يتأثر بإعادة ترتيب الأسئلة في الملف. */
fun questionKey(lessonId: String, question: Question): String =
    "$lessonId:${question.text.hashCode()}"

/** الأسئلة التي أُخطئ فيها، بترتيب الدروس، كأزواج (المفتاح، السؤال). */
fun reviewItems(lessons: List<Lesson>, store: ProgressStore): List<Pair<String, Question>> {
    val wrong = store.wrongKeys()
    return lessons.flatMap { lesson ->
        lesson.questions.mapNotNull { q ->
            val key = questionKey(lesson.id, q)
            if (key in wrong) key to q else null
        }
    }
}

/** يقرأ الدروس من assets/content.json — لإضافة دروس جديدة عدّل الملف فقط. */
object ContentLoader {

    fun load(context: Context): List<Lesson> {
        val raw = context.assets.open("content.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val lessons = JSONObject(raw).getJSONArray("lessons")
        return List(lessons.length()) { i -> parseLesson(lessons.getJSONObject(i)) }
    }

    private fun parseLesson(o: JSONObject): Lesson {
        val sections = o.getJSONArray("sections")
        val examples = o.getJSONArray("examples")
        val questions = o.getJSONArray("questions")
        return Lesson(
            id = o.getString("id"),
            title = o.getString("title"),
            subtitle = o.getString("subtitle"),
            sections = List(sections.length()) { i ->
                val s = sections.getJSONObject(i)
                Section(s.getString("heading"), s.getString("text"))
            },
            examples = List(examples.length()) { i ->
                val e = examples.getJSONObject(i)
                Example(e.getString("text"), e.getString("parse"))
            },
            questions = List(questions.length()) { i -> parseQuestion(questions.getJSONObject(i)) }
        )
    }

    private fun parseQuestion(o: JSONObject): Question {
        val options = o.getJSONArray("options")
        return Question(
            text = o.getString("text"),
            options = List(options.length()) { i -> options.getString(i) },
            answer = o.getInt("answer"),
            explanation = o.getString("explanation")
        )
    }
}
