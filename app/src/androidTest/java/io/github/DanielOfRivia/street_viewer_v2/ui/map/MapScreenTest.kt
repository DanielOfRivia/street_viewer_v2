package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.ui.theme.Street_viewer_v2Theme
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersEmptyStateWithoutCrashing() {
        composeTestRule.setContent {
            Street_viewer_v2Theme {
                MapScreen(uiState = MapUiState())
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun rendersGoogleMapWithTrackWithoutCrashing() {
        composeTestRule.setContent {
            Street_viewer_v2Theme {
                MapScreen(
                    uiState = MapUiState(
                        points = listOf(
                            LocationPoint(id = 1, latitude = 43.6532, longitude = -79.3832, timestampMillis = 0L, accuracyMeters = 8f),
                            LocationPoint(id = 2, latitude = 43.6540, longitude = -79.3820, timestampMillis = 30_000L, accuracyMeters = 6f),
                        ),
                    ),
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
