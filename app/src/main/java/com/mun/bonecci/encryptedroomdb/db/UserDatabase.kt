package com.mun.bonecci.encryptedroomdb.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mun.bonecci.encryptedroomdb.data.cipher.PassPhraseUtils
import com.mun.bonecci.encryptedroomdb.data.cipher.SQLCipherUtils
import com.mun.bonecci.encryptedroomdb.data.models.Log
import com.mun.bonecci.encryptedroomdb.data.models.Session
import com.mun.bonecci.encryptedroomdb.data.models.User
import net.sqlcipher.database.SupportFactory

/**
 * Room database class representing the user database.
 */
@Database(
    entities = [
        User::class,
        Session::class,
        Log::class
               ],
    version = 3,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    /**
     * Provides access to the UserDao interface for database operations.
     *
     * @return The UserDao instance.
     */
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun logDao(): LogDao

    /**
     * Companion object for accessing the database instance.
     */
    companion object {
        @Volatile private var instance: UserDatabase? = null
        private const val DATABASE_NAME = "user_database.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user ADD COLUMN age INTEGER")
                database.execSQL("ALTER TABLE user ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Створюємо таблицю session
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER
                    )
                    """.trimIndent()
                )
                // Створюємо таблицю log
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        message TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        /**
         * Returns the singleton instance of the UserDatabase.
         *
         * @param context The application context.
         * @return The UserDatabase instance.
         */

        @Synchronized
        fun getInstance(context: Context): UserDatabase {
            // Get the user passphrase and convert it to a byte array
            val userPassphrase = PassPhraseUtils.getPassphrase(context)
            val passphrase = userPassphrase.toByteArray()

            // Check the state of the database encryption
            val state = SQLCipherUtils.getDatabaseState(context, DATABASE_NAME)

            // Create a SupportFactory using the passphrase
            val factory = SupportFactory(passphrase)

            // Migrate the database to an encrypted one if it is currently unencrypted
            if (state == SQLCipherUtils.State.UNENCRYPTED) {
                SQLCipherUtils.migrateToEncryptedDatabase(DATABASE_NAME, context, userPassphrase)
            }
            return instance ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext, UserDatabase::class.java,
                    "user_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .openHelperFactory(factory)
                    .build()
                instance = db
                db
            }
        }
    }
}