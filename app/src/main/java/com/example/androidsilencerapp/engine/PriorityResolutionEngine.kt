package com.example.androidsilencerapp.engine

import com.example.androidsilencerapp.data.model.Profile

/**
 * Engine responsible for resolving conflicts when multiple silencing profiles
 * are active simultaneously.
 */
class PriorityResolutionEngine {

    /**
     * Determines which profile should take precedence among multiple active triggers.
     *
     * Resolution Strategy:
     * 1. Priority Level: Profiles with a higher priority level (e.g., 5 vs 1) win.
     * 2. Recency: If priority levels are equal, the most recently modified profile wins.
     *
     * @param activeProfiles A list of [Profile] objects whose trigger conditions are currently met.
     * @return The winning [Profile] to be applied, or null if the list is empty.
     */
    fun resolve(activeProfiles: List<Profile>): Profile? {
        if (activeProfiles.isEmpty()) return null

        // Using maxWithOrNull is O(n), making it significantly more efficient than
        // sorting the entire list (O(n log n)) when only the top element is needed.
        return activeProfiles.maxWithOrNull(
            compareBy<Profile> { it.priorityLevel }
                .thenBy { it.lastModified }
        )
    }
}
