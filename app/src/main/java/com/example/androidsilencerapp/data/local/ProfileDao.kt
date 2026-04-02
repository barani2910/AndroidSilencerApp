package com.example.androidsilencerapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.androidsilencerapp.data.model.Profile

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun getProfilesForUser(userId: String): LiveData<List<Profile>>

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    suspend fun getProfilesForUserSync(userId: String): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("DELETE FROM profiles WHERE userId = :userId")
    suspend fun deleteAllProfilesForUser(userId: String)
}
