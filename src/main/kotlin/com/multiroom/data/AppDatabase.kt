package com.multiroom.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.multiroom.data.dao.PlayerDao
import com.multiroom.data.dao.SettingsDao
import com.multiroom.data.entity.PlayerEntity
import com.multiroom.data.entity.SettingsEntity
import com.multiroom.data.entity.ThemeMode
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.io.path.createDirectories

@Database(
    entities = [SettingsEntity::class, PlayerEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settings(): SettingsDao
    abstract fun players(): PlayerDao
}

class Converters {
    @TypeConverter
    fun themeToName(mode: ThemeMode): String = mode.name

    @TypeConverter
    fun nameToTheme(name: String): ThemeMode =
        runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
}

/**
 * Opens the database under %APPDATA%, creating the directory if needed.
 *
 * Uses the bundled SQLite driver so no system library is required on the target
 * machine.
 */
fun openDatabase(path: Path = defaultDatabasePath()): AppDatabase {
    path.parent?.createDirectories()
    return Room.databaseBuilder<AppDatabase>(name = path.toString())
        .addMigrations(RenameRoomsToPlayers)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

fun defaultDatabasePath(): Path {
    val base = System.getenv("APPDATA") ?: System.getProperty("user.home")
    return Path.of(base, "SnapcastRooms", "snapcast-rooms.db")
}

/** Carries existing configuration through the rename of a room to a player. */
private object RenameRoomsToPlayers : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE rooms RENAME TO players")
    }
}
