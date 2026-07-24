package com.hwnix.smsagent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: SmsEntity): Long

    @Update
    suspend fun update(sms: SmsEntity)

    @Query("SELECT * FROM sms_cache WHERE status = 'pending_upload' ORDER BY createdAt ASC")
    suspend fun getPendingUploads(): List<SmsEntity>

    @Query("SELECT EXISTS(SELECT * FROM sms_cache WHERE messageRef = :messageRef AND direction = 'incoming')")
    suspend fun exists(messageRef: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM sms_cache WHERE idempotencyKey = :key)")
    suspend fun existsByIdempotencyKey(key: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM sms_cache WHERE phoneNumber = :phone AND messageBody = :body AND sentAt = :timestamp AND direction = 'incoming')")
    suspend fun existsIncoming(phone: String, body: String, timestamp: Long): Boolean

    @Query("DELETE FROM sms_cache WHERE status = 'uploaded' AND createdAt < :timestamp")
    suspend fun cleanOldLogs(timestamp: Long)
}
