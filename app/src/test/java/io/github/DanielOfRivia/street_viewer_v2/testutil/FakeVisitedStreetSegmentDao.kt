package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.data.local.VisitedStreetSegmentDao
import io.github.DanielOfRivia.street_viewer_v2.data.local.VisitedStreetSegmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVisitedStreetSegmentDao : VisitedStreetSegmentDao {

    private val segments = MutableStateFlow<List<VisitedStreetSegmentEntity>>(emptyList())

    override suspend fun insertAll(segments: List<VisitedStreetSegmentEntity>) {
        val existingKeys = this.segments.value.map { it.wayId to it.segmentIndex }.toSet()
        val newOnes = segments.filter { (it.wayId to it.segmentIndex) !in existingKeys }
        this.segments.value = this.segments.value + newOnes
    }

    override fun observeAll(): Flow<List<VisitedStreetSegmentEntity>> = segments
}
