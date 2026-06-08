package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Word::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "word_lock_database"
                )
                    .addCallback(WordDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class WordDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.wordDao())
                }
            }
        }

        suspend fun populateDatabase(wordDao: WordDao) {
            val initialWords = listOf(
                Word(
                    english = "Hello",
                    russian = "Здравствуйте / Привет",
                    uzbek = "Salom",
                    exampleEnglish = "Hello, how are you today?",
                    exampleRussian = "Здравствуйте, как у вас сегодня дела?",
                    exampleUzbek = "Salom, bugun ishlaringiz qalay?"
                ),
                Word(
                    english = "Goodbye",
                    russian = "До свидания / Пока",
                    uzbek = "Xayr",
                    exampleEnglish = "Goodbye, see you tomorrow!",
                    exampleRussian = "До свидания, увидимся завтра!",
                    exampleUzbek = "Xayr, ertagacha ko'rishguncha!"
                ),
                Word(
                    english = "Thank you",
                    russian = "Спасибо",
                    uzbek = "Rahmat",
                    exampleEnglish = "Thank you very much for your kind help.",
                    exampleRussian = "Большое спасибо за вашу добрую помощь.",
                    exampleUzbek = "Mehribon yordamingiz uchun kattakon rahmat."
                ),
                Word(
                    english = "Please",
                    russian = "Пожалуйста",
                    uzbek = "Iltimos",
                    exampleEnglish = "Please, give me some water.",
                    exampleRussian = "Пожалуйста, дайте мне немного воды.",
                    exampleUzbek = "Iltimos, menga biroz suv bering."
                ),
                Word(
                    english = "Friend",
                    russian = "Друг",
                    uzbek = "Do'st",
                    exampleEnglish = "He is my best friend in school.",
                    exampleRussian = "Он мой лучший друг в школе.",
                    exampleUzbek = "U mening maktabdagi eng yaqin do'stim."
                ),
                Word(
                    english = "Family",
                    russian = "Семья",
                    uzbek = "Oila",
                    exampleEnglish = "I love spending weekend with my family.",
                    exampleRussian = "Я люблю проводить выходные со своей семьей.",
                    exampleUzbek = "Men oilam bilan dam olish kunlarini o'tkazishni yaxshi ko'raman."
                ),
                Word(
                    english = "Beautiful",
                    russian = "Красивый / Прекрасный",
                    uzbek = "Chiroyli",
                    exampleEnglish = "What a beautiful flower!",
                    exampleRussian = "Какой красивый цветок!",
                    exampleUzbek = "Qanday chiroyli gul!"
                ),
                Word(
                    english = "Success",
                    russian = "Успех",
                    uzbek = "Muvaffaqiyat",
                    exampleEnglish = "Hard work is the key to success.",
                    exampleRussian = "Упорный труд — ключ к успеху.",
                    exampleUzbek = "Muvaffaqiyat kaliti tinimsiz mehnatdir."
                ),
                Word(
                    english = "Language",
                    russian = "Язык",
                    uzbek = "Til",
                    exampleEnglish = "Learning a foreign language opens new doors.",
                    exampleRussian = "Изучение иностранного языка открывает новые двери.",
                    exampleUzbek = "Xorijiy tilni o'rganish yangi imkoniyatlar eshigini ochadi."
                ),
                Word(
                    english = "Time",
                    russian = "Время",
                    uzbek = "Vaqt",
                    exampleEnglish = "Time is a restricted resource.",
                    exampleRussian = "Время — ограниченный ресурс.",
                    exampleUzbek = "Vaqt cheklangan resursdir."
                ),
                Word(
                    english = "Knowledge",
                    russian = "Знание",
                    uzbek = "Bilim",
                    exampleEnglish = "Knowledge is the most powerful weapon.",
                    exampleRussian = "Знание — самое мощное оружие.",
                    exampleUzbek = "Bilim eng kuchli quroldir."
                ),
                Word(
                    english = "Opportunity",
                    russian = "Возможность",
                    uzbek = "Imkoniyat",
                    exampleEnglish = "Every mistake is an opportunity to learn.",
                    exampleRussian = "Каждая ошибка — это возможность учиться.",
                    exampleUzbek = "Har bir xato - o'rganish uchun bir imkoniyatdir."
                ),
                Word(
                    english = "Education",
                    russian = "Образование",
                    uzbek = "Ta'lim",
                    exampleEnglish = "Education is the foundation of growth.",
                    exampleRussian = "Образование — основа роста.",
                    exampleUzbek = "Ta'lim o'sib-ulg'ayish poydevoridir."
                ),
                Word(
                    english = "Freedom",
                    russian = "Свобода",
                    uzbek = "Ozodlik",
                    exampleEnglish = "Everyone has the right to freedom.",
                    exampleRussian = "Каждый человек имеет право на свободу.",
                    exampleUzbek = "Har kim erkinlik huquqiga ega."
                ),
                Word(
                    english = "Health",
                    russian = "Здоровье",
                    uzbek = "Sog'liq",
                    exampleEnglish = "Take care of your health.",
                    exampleRussian = "Берегите свое здоровье.",
                    exampleUzbek = "Sog'lig'ingizni asrang."
                ),
                Word(
                    english = "Future",
                    russian = "Будущее",
                    uzbek = "Kelajak",
                    exampleEnglish = "The future belongs to those who believe in their dreams.",
                    exampleRussian = "Будущее принадлежит тем, кто верит в свои мечты.",
                    exampleUzbek = "Kelajak o'z orzulariga ishonganlarnikidir."
                ),
                Word(
                    english = "Book",
                    russian = "Книга",
                    uzbek = "Kitob",
                    exampleEnglish = "This book is extremely interesting.",
                    exampleRussian = "Эта книга чрезвычайно интересна.",
                    exampleUzbek = "Bu kitob juda qiziqarli."
                ),
                Word(
                    english = "Happiness",
                    russian = "Счастье",
                    uzbek = "Baxt",
                    exampleEnglish = "Happiness comes from within.",
                    exampleRussian = "Счастье приходит изнутри.",
                    exampleUzbek = "Baxt insonning o'zidan boshlanadi."
                ),
                Word(
                    english = "Peace",
                    russian = "Мир / Покой",
                    uzbek = "Tinchlik",
                    exampleEnglish = "We all pray for world peace.",
                    exampleRussian = "Мы все молимся о мире во всем мире.",
                    exampleUzbek = "Barchamiz dunyo tinchligi uchun duo qilamiz."
                ),
                Word(
                    english = "Achieve",
                    russian = "Достигать / Добиваться",
                    uzbek = "Erishmoq",
                    exampleEnglish = "You can achieve anything you set your mind to.",
                    exampleRussian = "Вы можете достичь всего, к чему стремитесь.",
                    exampleUzbek = "Siz o'z oldingizga qo'ygan har qanday narsaga erisha olasiz."
                )
            )
            wordDao.insertWords(initialWords)
        }
    }
}
