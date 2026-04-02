package com.example.androidsilencerapp.engine

import com.example.androidsilencerapp.data.model.Profile

class PriorityResolutionEngine {
    fun resolve(activeProfiles: List<Profile>): Profile? {
        if (activeProfiles.isEmpty()) return null
        
        // Resolve based on priority level (1-5, where 5 is highest)
        // If levels are equal, pick the one modified last
        return activeProfiles.sortedWith(
            compareByDescending<Profile> { it.priorityLevel }
                .thenByDescending { it.lastModified }
        ).firstOrNull()
    }
}
