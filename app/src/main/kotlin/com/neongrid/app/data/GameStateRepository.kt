package com.neongrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import android.content.Context
import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GameStateRepository(context: Context, scope: CoroutineScope) {

    private val store: DataStore<GameSnapshot> = DataStoreFactory.create(
        serializer = CborDataStoreSerializer(GameSnapshot(), GameSnapshot.serializer()),
        corruptionHandler = ReplaceFileCorruptionHandler { GameSnapshot() },
        scope = scope,
        produceFile = { context.dataStoreFile("game_snapshot.cbor") },
    )

    val snapshot: Flow<GameSnapshot> = store.data

    suspend fun read(): GameSnapshot = store.data.first()

    suspend fun save(snapshot: GameSnapshot) {
        store.updateData { snapshot }
    }
}

class ProgressRepository(context: Context, scope: CoroutineScope) {

    private val store: DataStore<PlayerProgress> = DataStoreFactory.create(
        serializer = CborDataStoreSerializer(PlayerProgress(), PlayerProgress.serializer()),
        corruptionHandler = ReplaceFileCorruptionHandler { PlayerProgress() },
        scope = scope,
        produceFile = { context.dataStoreFile("player_progress.cbor") },
    )

    val progress: Flow<PlayerProgress> = store.data

    suspend fun read(): PlayerProgress = store.data.first()

    suspend fun update(transform: (PlayerProgress) -> PlayerProgress) {
        store.updateData(transform)
    }
}

class SettingsRepository(context: Context, scope: CoroutineScope) {

    private val store: DataStore<Settings> = DataStoreFactory.create(
        serializer = CborDataStoreSerializer(Settings(), Settings.serializer()),
        corruptionHandler = ReplaceFileCorruptionHandler { Settings() },
        scope = scope,
        produceFile = { context.dataStoreFile("settings.cbor") },
    )

    val settings: Flow<Settings> = store.data

    suspend fun update(transform: (Settings) -> Settings) {
        store.updateData(transform)
    }
}
