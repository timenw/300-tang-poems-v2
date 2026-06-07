package com.poem300.data.db

import android.content.Context
import androidx.room.*
import com.poem300.data.model.Poem
import com.poem300.data.model.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface PoemDao {
    @Query("SELECT * FROM poems ORDER BY id")
    fun getAllPoems(): Flow<List<Poem>>

    @Query("SELECT * FROM poems WHERE id = :id")
    suspend fun getPoemById(id: Int): Poem?

    @Query("SELECT * FROM poems WHERE author = :author ORDER BY id")
    fun getPoemsByAuthor(author: String): Flow<List<Poem>>

    @Query("SELECT * FROM poems WHERE category LIKE '%' || :category || '%' ORDER BY id")
    fun getPoemsByCategory(category: String): Flow<List<Poem>>

    @Query("SELECT * FROM poems WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR titleEn LIKE '%' || :query || '%' ORDER BY id")
    fun searchPoems(query: String): Flow<List<Poem>>

    @Query("SELECT DISTINCT author FROM poems ORDER BY author")
    fun getAllAuthors(): Flow<List<String>>

    @Query("SELECT DISTINCT dynasty FROM poems ORDER BY dynasty")
    fun getAllDynasties(): Flow<List<String>>

    @Query("SELECT * FROM poems WHERE difficulty = :level ORDER BY id")
    fun getPoemsByDifficulty(level: Int): Flow<List<Poem>>

    @Query("SELECT * FROM poems ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomPoem(): Poem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(poems: List<Poem>)

    @Query("SELECT COUNT(*) FROM poems")
    suspend fun getCount(): Int
}

@Dao
interface FavoriteDao {
    @Query("SELECT poems.* FROM poems INNER JOIN favorites ON poems.id = favorites.poemId ORDER BY favorites.createdAt DESC")
    fun getFavoritePoems(): Flow<List<Poem>>

    @Query("SELECT poemId FROM favorites")
    fun getFavoriteIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE poemId = :poemId)")
    fun isFavorite(poemId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE poemId = :poemId")
    suspend fun removeFavorite(poemId: Int)

    @Query("UPDATE favorites SET note = :note WHERE poemId = :poemId")
    suspend fun updateNote(poemId: Int, note: String)

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}

@Database(entities = [Poem::class, Favorite::class], version = 1, exportSchema = false)
abstract class PoemDatabase : RoomDatabase() {
    abstract fun poemDao(): PoemDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: PoemDatabase? = null

        fun getInstance(context: Context): PoemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PoemDatabase::class.java,
                    "poem300.db"
                )
                    .createFromAsset("poems.db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
