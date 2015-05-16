# Mapsforge-OsmDroid-GraphHopper
**A combination of Osmdroid (With OsmBonusPack) as map viewer, Mapsforge as tile renderer and Graphhopper as routing provider**

This project shows how to easily combine three great libraris to build up a handy offline map application in android.

It uses [OsmDroid](https://github.com/osmdroid/osmdroid) as map viewer, [OsmBonusPack](https://code.google.com/p/osmbonuspack/), [Mapsforge](http://mapsforge.com) as offline OSM map renderer and [Graphhopper](https://graphhopper.com) as OSM-based routing provider.

Please note that you need to specify your own mapsforge .map file and rendertheme:

**MainActivity.java**

    final ITileSource tileSource = new MFTileSource(5,20, 256,
            Environment.getExternalStorageDirectory() + "/Mapsforge/map/iran.map",
            Environment.getExternalStorageDirectory() + "/Mapsforge/renderthemes/detailed.xml"
            , this);


The map center is focused at (32.653906, 51.659088), pointing on Isfahan, Iran. change it as you want.

You can find pre-built .map file in [mapsforge download center](http://download.mapsforge.org/maps/).

Rendertheme are used to style your rendered maps, you can edit it as you want.
The render theme "Detailed.xml" is available in "Data" folder of this repository. (You may want to try [diffrent render themes](https://github.com/mapsforge/mapsforge/tree/master/Applications/Android/Samples/assets/renderthemes))


** GraphHopper routing is not available at the moment, I will update the repository soon.

