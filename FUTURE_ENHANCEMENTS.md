# Future enhancements

Ideas worth revisiting later, deliberately not built now.

## Move street-coverage matching to the backend

The "color streets I've visited" feature (see the street coverage repository under
`data/repository/` and `domain/repository/StreetCoverageRepository.kt`) currently does
everything on-device: the app queries the Overpass API for street geometry in the area
around the user's recorded points, and matches "is this street segment within 30m of a
point" locally in Kotlin.

An alternative: have the existing backend do this instead. The server would fetch/cache
OSM data, do the point-to-segment matching (probably more efficiently at scale, e.g. with
a spatial index or PostGIS), and expose a new endpoint returning precomputed visited-street
geometry (e.g. GeoJSON) for the app to fetch and render. This would need:
- A new API contract (what the app sends: which points or a bounding box; what it gets
  back: visited street segments for that area).
- The app's `StreetCoverageRepository` implementation swapped for a Retrofit client against
  that endpoint, with the on-device Overpass client and matching logic removed.

Worth doing if: the matching computation becomes too slow/battery-heavy on-device, the
visited-area history grows large enough that repeated on-device Overpass queries become
impractical, or the backend already has better OSM tooling than reimplementing it in Kotlin.

## More precise street-segment coloring

The current matching treats each short OSM way segment as entirely "visited" or entirely
"not visited," based on whether *any* point along it falls within 30m of a recorded point.
This is a simplification -- real point-to-segment clipping (only coloring the exact sub-
portion of a segment within the 30m buffer) would look more accurate at segment boundaries,
at the cost of noticeably more geometry math. Worth revisiting if the segment-level
approximation looks visibly blocky in practice.
