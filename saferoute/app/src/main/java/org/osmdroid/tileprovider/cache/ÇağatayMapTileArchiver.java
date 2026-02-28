package org.osmdroid.tileprovider.cache;

import android.util.Log;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.MapTileIndex;

import java.io.File;

/**
 * A class that is not part of the osmdroid library, but is used to download map tiles for offline use.
 * This is a placeholder and should be replaced with a proper implementation.
 */
public class ÇağatayMapTileArchiver {
    public void archiveTiles(File osmdroidTileCache, ITileSource tileSource, int zoomMin, int zoomMax, BoundingBox boundingBox) {
        Log.d("ÇağatayMapTileArchiver", "archiveTiles called with zoomMin: " + zoomMin + ", zoomMax: " + zoomMax);
    }
}
