package com.example.exposeit.Post.Utils;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SpatialIndexingUtilsTest
 *
 * Tests the spatial indexing utilities that convert coordinates (latitude and longitude)
 * into Geohashes and bounding box prefixes. These calculations are critical for spatial query
 * performance, search accuracy, and nearby post partitioning.
 */
class SpatialIndexingUtilsTest {

    @Test
    void testGetExactGeoHash() {
        double lat = 37.7749;
        double lon = -122.4194;
        String geohash = SpatialIndexingUtils.getExactGeoHash(lat, lon);
        assertNotNull(geohash);
        assertEquals(7, geohash.length());
        assertEquals(geohash, SpatialIndexingUtils.getExactGeoHash(lat, lon));
    }

    @Test
    void testGetBoundingBoxPrefix() {
        double lat = 37.7749;
        double lon = -122.4194;
        String prefix = SpatialIndexingUtils.getBoundingBoxPrefix(lat, lon);
        assertNotNull(prefix);
        assertEquals(4, prefix.length());
        String exact = SpatialIndexingUtils.getExactGeoHash(lat, lon);
        assertTrue(exact.startsWith(prefix));
    }

    @Test
    void testGetNineBoxGeohashes() {
        double lat = 37.7749;
        double lon = -122.4194;
        List<String> nineBoxes = SpatialIndexingUtils.getNineBoxGeohashes(lat, lon);
        assertNotNull(nineBoxes);
        assertEquals(9, nineBoxes.size());
        String center = SpatialIndexingUtils.getBoundingBoxPrefix(lat, lon);
        assertEquals(center, nineBoxes.get(0));
        for (String box : nineBoxes) {
            assertEquals(4, box.length());
        }
    }
}
