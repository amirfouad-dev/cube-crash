package com.neongrid.app.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
class CborDataStoreSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T = try {
        Cbor.decodeFromByteArray(serializer, input.readBytes())
    } catch (e: SerializationException) {
        throw CorruptionException("Corrupted DataStore payload", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(Cbor.encodeToByteArray(serializer, t))
    }
}
