package dev.shushant.kourier.storage.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = KourierDatabase.Schema,
            name = "kourier_telemetry.db"
        )
    }
}
