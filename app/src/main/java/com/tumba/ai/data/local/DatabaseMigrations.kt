package com.tumba.ai.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive migrations keep the user's local history intact. Destructive migration is not
 * enabled anywhere in the app.
 *
 * Version 1 shipped the initial schema (conversations, messages, attachments, custom
 * providers, local models). Version 2 adds conversation pinning.
 */
object DatabaseMigrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE conversations ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
