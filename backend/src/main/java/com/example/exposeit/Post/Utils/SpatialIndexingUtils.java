package com.example.exposeit.Post.Utils;

import ch.hsr.geohash.GeoHash;

import java.util.ArrayList;
import java.util.List;

public class SpatialIndexingUtils {
    public static String getExactGeoHash(double latitude, double longitude)
    {
        return GeoHash.geoHashStringWithCharacterPrecision(
                latitude,
                longitude,
                7
        );
    }

    public static String getBoundingBoxPrefix(double latitude, double longitude)
    {
        return GeoHash.geoHashStringWithCharacterPrecision(
                latitude,
                longitude,
                7
        );
    }

    public static List<String> getNineBoxGeohashes(double latitude, double longitude){
        GeoHash centreBox = GeoHash.withCharacterPrecision(latitude, longitude, 4);
        GeoHash[] surroundingBoxes = centreBox.getAdjacent();

        List<String> prefixes = new ArrayList<>();
        prefixes.add(centreBox.toBase32());

        for(GeoHash box: surroundingBoxes)
            prefixes.add(box.toBase32());

        return prefixes;
    }
}
