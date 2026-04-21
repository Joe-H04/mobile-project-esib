package com.betnow.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileDao {

    @Query("SELECT * FROM cached_profile WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): CachedProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CachedProfileEntity)
}
