package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Patient::class,
        Condition::class,
        LabResult::class,
        Vital::class,
        Medication::class,
        MedicationLog::class,
        CareTask::class,
        Document::class,
        HealthSummary::class,
        Observation::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_summaries (
                        id TEXT PRIMARY KEY NOT NULL,
                        patientId TEXT NOT NULL,
                        content TEXT NOT NULL,
                        generatedAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        approvedAt INTEGER,
                        clinicianName TEXT,
                        auditTrail TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS medication_logs (
                        id TEXT PRIMARY KEY NOT NULL,
                        medicationId TEXT NOT NULL,
                        takenAt INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure no conflicting indices exist
                db.execSQL("DROP INDEX IF EXISTS index_conditions_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_lab_results_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_vitals_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_medications_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_care_tasks_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_documents_patientId")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 4 to 5: Clean up any stray indices so TableInfo matches Entity specifications exactly
                db.execSQL("DROP INDEX IF EXISTS index_conditions_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_lab_results_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_vitals_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_medications_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_care_tasks_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_documents_patientId")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS observations (
                        id TEXT PRIMARY KEY NOT NULL,
                        sourceDocumentId TEXT NOT NULL,
                        sourcePage INTEGER NOT NULL,
                        sourceText TEXT NOT NULL,
                        testNameRaw TEXT NOT NULL,
                        testNameNormalized TEXT NOT NULL,
                        value REAL NOT NULL,
                        unit TEXT NOT NULL,
                        referenceRangeLow REAL,
                        referenceRangeHigh REAL,
                        referenceRangeText TEXT,
                        collectionDate TEXT,
                        confidence REAL NOT NULL,
                        verificationStatus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
