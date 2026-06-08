package com.example.data

import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()

    suspend fun getWordById(id: Int): Word? = wordDao.getWordById(id)

    suspend fun getRandomWord(): Word? = wordDao.getRandomWord()

    suspend fun getNextWordToLearn(): Word? = wordDao.getNextWordToLearn()

    suspend fun insert(word: Word): Long = wordDao.insertWord(word)

    suspend fun insertWords(words: List<Word>) = wordDao.insertWords(words)

    suspend fun update(word: Word) = wordDao.updateWord(word)

    suspend fun delete(word: Word) = wordDao.deleteWord(word)

    suspend fun incrementShownCount(id: Int) = wordDao.incrementShownCount(id)

    suspend fun getWordCount(): Int = wordDao.getWordCount()
}
