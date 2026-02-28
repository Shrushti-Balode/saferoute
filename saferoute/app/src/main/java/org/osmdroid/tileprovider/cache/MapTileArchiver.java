package org.osmdroid.tileprovider.cache;

import android.util.Log;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.MapTileIndex;

import java.io.File;

public class MapTileArchiver {
    public void archiveTiles(File osmdroidTileCache, ITileSource tileSource, int zoomMin, int zoomMax, BoundingBox boundingBox) {
        Log.d("MapTileArchiver", "archiveTiles called with zoomMin: " + zoomMin + ", zoomMax: " + zoomMax);
    }
}
