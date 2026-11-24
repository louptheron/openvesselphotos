package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.FishingOpportunities

interface FishingOpportunityRepository {
    /** The TACs of the species Ifremer assesses, with the quotas of France, of the current regulation */
    fun find(): FishingOpportunities
}
