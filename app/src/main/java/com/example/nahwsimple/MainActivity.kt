@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nahwsimple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lessons = ContentLoader.load(this)
        val store = ProgressStore(this)
        setContent {
            AppTheme {
                // واجهة عربية من اليمين إلى اليسار بغض النظر عن لغة الجهاز
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NahwApp(lessons, store)
                }
            }
        }
    }
}

@Composable
fun NahwApp(lessons: List<Lesson>, store: ProgressStore) {
    var lessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var inQuiz by rememberSaveable { mutableStateOf(false) }
    var inReview by rememberSaveable { mutableStateOf(false) }
    val lesson = lessons.firstOrNull { it.id == lessonId }

    BackHandler(enabled = lesson != null || inReview) {
        if (inReview) inReview = false
        else if (inQuiz) inQuiz = false
        else lessonId = null
    }

    if (inReview) {
        val items = remember { reviewItems(lessons, store) }
        QuizScreen(
            quizId = "review",
            topTitle = "مراجعة أخطائي",
            questions = items.map { it.second },
            nextLesson = null,
            onAnswered = { i, correct -> store.recordAnswer(items[i].first, correct) },
            onFinish = { store.registerStudyToday() },
            onNextLesson = {},
            onExit = { inReview = false },
            onBack = { inReview = false },
            passedText = "أحسنت! راجعت أخطاءك",
            failedText = "ما زالت بعض الأخطاء، أعد المراجعة"
        )
    } else if (lesson == null) {
        HomeScreen(lessons, store, onReview = { inReview = true }) { id ->
            lessonId = id
            inQuiz = false
        }
    } else if (!inQuiz) {
        LessonScreen(
            lesson = lesson,
            onBack = { lessonId = null },
            onStartQuiz = { inQuiz = true }
        )
    } else {
        val next = lessons.getOrNull(lessons.indexOf(lesson) + 1)
        QuizScreen(
            quizId = lesson.id,
            topTitle = "تدريب: ${lesson.title}",
            questions = lesson.questions,
            nextLesson = next,
            onAnswered = { i, correct ->
                store.recordAnswer(questionKey(lesson.id, lesson.questions[i]), correct)
            },
            onFinish = { score -> store.saveScore(lesson.id, score) },
            onNextLesson = { n ->
                lessonId = n.id
                inQuiz = false
            },
            onExit = {
                lessonId = null
                inQuiz = false
            },
            onBack = { inQuiz = false }
        )
    }
}

// ───────────────────────── الشاشة الرئيسية ─────────────────────────

@Composable
fun HomeScreen(
    lessons: List<Lesson>,
    store: ProgressStore,
    onReview: () -> Unit,
    onOpen: (String) -> Unit
) {
    val streak = remember { store.streak() }
    val wrongCount = remember { reviewItems(lessons, store).size }
    val bests = remember { lessons.associate { it.id to store.bestScore(it.id) } }
    val done = remember { lessons.count { store.isPassed(it.id, it.questions.size) } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("نحو ميسّر", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SummaryCard(done = done, total = lessons.size, streak = streak) }
            if (wrongCount > 0) {
                item { ReviewCard(count = wrongCount, onClick = onReview) }
            }
            items(lessons) { lesson ->
                val number = lessons.indexOf(lesson) + 1
                val best = bests[lesson.id] ?: -1
                val total = lesson.questions.size
                val passed = best >= 0 && best * 100 >= total * 60
                LessonRow(
                    number = number,
                    lesson = lesson,
                    status = when {
                        best < 0 -> "جديد"
                        passed -> "✓  $best/$total"
                        else -> "$best/$total"
                    },
                    onClick = { onOpen(lesson.id) }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(done: Int, total: Int, streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("تقدّمك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.fillMaxWidth()
            )
            Text("$done من $total دروس مكتملة", style = MaterialTheme.typography.bodyMedium)
            if (streak > 0) {
                Text("🔥 أيام متواصلة: $streak", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("ابدأ درساً اليوم لتبدأ سلسلتك", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReviewCard(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("مراجعة أخطائي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$count سؤال بانتظار المراجعة", style = MaterialTheme.typography.bodyMedium)
            }
            Text("ابدأ ←", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LessonRow(number: Int, lesson: Lesson, status: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    lesson.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ───────────────────────── شاشة الدرس ─────────────────────────

@Composable
fun LessonScreen(lesson: Lesson, onBack: () -> Unit, onStartQuiz: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.title, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("ابدأ التدريب (${lesson.questions.size} أسئلة)")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            lesson.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        section.text,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp
                    )
                }
            }

            if (lesson.examples.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("أمثلة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        lesson.examples.forEachIndexed { i, example ->
                            if (i > 0) HorizontalDivider()
                            Text(
                                example.text,
                                fontSize = 22.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                example.parse,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── شاشة التدريب ─────────────────────────

private val optionLetters = listOf("أ", "ب", "ج", "د")

private enum class OptionState { NEUTRAL, CORRECT, WRONG }

@Composable
fun QuizScreen(
    quizId: String,
    topTitle: String,
    questions: List<Question>,
    nextLesson: Lesson?,
    onAnswered: (Int, Boolean) -> Unit,
    onFinish: (Int) -> Unit,
    onNextLesson: (Lesson) -> Unit,
    onExit: () -> Unit,
    onBack: () -> Unit,
    passedText: String = "أحسنت! أتممت الدرس",
    failedText: String = "اقتربت! راجع الدرس وحاول مرة أخرى"
) {
    val total = questions.size
    var index by rememberSaveable(quizId) { mutableIntStateOf(0) }
    var selected by rememberSaveable(quizId) { mutableIntStateOf(-1) }
    var score by rememberSaveable(quizId) { mutableIntStateOf(0) }
    var finished by rememberSaveable(quizId) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
            )
        }
    ) { padding ->
        if (total == 0) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("لا توجد أسئلة هنا بعد.", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onExit) { Text("رجوع") }
            }
        } else if (finished) {
            ResultView(
                modifier = Modifier.padding(padding),
                score = score,
                total = total,
                passedText = passedText,
                failedText = failedText,
                nextLesson = nextLesson,
                onRetry = {
                    index = 0
                    selected = -1
                    score = 0
                    finished = false
                },
                onNextLesson = onNextLesson,
                onExit = onExit
            )
        } else {
            val question = questions[index]
            // ترتيب الخيارات يتغير من سؤال لآخر لكنه ثابت للسؤال نفسه
            val order = remember(quizId, index) {
                question.options.indices.shuffled(Random(quizId.hashCode() * 31 + index))
            }
            val answered = selected >= 0

            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (index + if (answered) 1 else 0).toFloat() / total },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "السؤال ${index + 1} من $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    question.text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp
                )

                order.forEachIndexed { position, optionIndex ->
                    val state = when {
                        !answered -> OptionState.NEUTRAL
                        optionIndex == question.answer -> OptionState.CORRECT
                        optionIndex == selected -> OptionState.WRONG
                        else -> OptionState.NEUTRAL
                    }
                    OptionCard(
                        letter = optionLetters.getOrElse(position) { "" },
                        text = question.options[optionIndex],
                        state = state
                    ) {
                        if (!answered) {
                            selected = optionIndex
                            val correct = optionIndex == question.answer
                            if (correct) score++
                            onAnswered(index, correct)
                        }
                    }
                }

                if (answered) {
                    FeedbackCard(correct = selected == question.answer, explanation = question.explanation)
                    val isLast = index + 1 >= total
                    Button(
                        onClick = {
                            if (isLast) {
                                finished = true
                                onFinish(score)
                            } else {
                                index++
                                selected = -1
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLast) "إنهاء" else "التالي")
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val container = when (state) {
        OptionState.CORRECT -> MaterialTheme.colorScheme.primaryContainer
        OptionState.WRONG -> MaterialTheme.colorScheme.errorContainer
        OptionState.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (state) {
        OptionState.CORRECT -> MaterialTheme.colorScheme.onPrimaryContainer
        OptionState.WRONG -> MaterialTheme.colorScheme.onErrorContainer
        OptionState.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("$letter)", fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            when (state) {
                OptionState.CORRECT -> Text("✓", fontWeight = FontWeight.Bold)
                OptionState.WRONG -> Text("✗", fontWeight = FontWeight.Bold)
                OptionState.NEUTRAL -> Unit
            }
        }
    }
}

@Composable
private fun FeedbackCard(correct: Boolean, explanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (correct) "إجابة صحيحة ✓" else "إجابة خاطئة ✗",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(explanation, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
        }
    }
}

@Composable
private fun ResultView(
    modifier: Modifier,
    score: Int,
    total: Int,
    passedText: String,
    failedText: String,
    nextLesson: Lesson?,
    onRetry: () -> Unit,
    onNextLesson: (Lesson) -> Unit,
    onExit: () -> Unit
) {
    val passed = score * 100 >= total * 60
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (passed) passedText else failedText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$score من $total",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (passed && nextLesson != null) {
            Button(onClick = { onNextLesson(nextLesson) }, modifier = Modifier.fillMaxWidth()) {
                Text("الدرس التالي: ${nextLesson.title}")
            }
        }
        FilledTonalButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("إعادة التدريب")
        }
        TextButton(onClick = onExit) { Text("العودة إلى الدروس") }
    }
}
