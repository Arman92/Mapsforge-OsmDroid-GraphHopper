package com.arman.osmdroidmapsforge.map;

import java.io.File;

import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import android.graphics.drawable.Drawable;

public class MFTileModuleProvider extends MapTileFileStorageProviderBase{
	protected MFTileSource tileSource;
	
	/**
	 * Constructor
	 * 
	 * @param pRegisterReceiver
	 * @param file
	 * @param tileSource
	 */
	public MFTileModuleProvider(IRegisterReceiver receiverRegistrar, File file, MFTileSource tileSource) {

		super(receiverRegistrar, NUMBER_OF_TILE_FILESYSTEM_THREADS, TILE_FILESYSTEM_MAXIMUM_QUEUE_SIZE);

		this.tileSource = tileSource;

	}
	
	@Override
	protected String getName() {
		return "MapsforgeTiles Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return "mapsforgetilesprovider";
	}

	@Override
	protected Runnable getTileLoader() {
		return new TileLoader();
	}

	@Override
	public boolean getUsesDataConnection() {
		return false;
	}

	@Override
	public int getMinimumZoomLevel() {
		return tileSource.getMinimumZoomLevel();
	}

	@Override
	public int getMaximumZoomLevel() {
		return tileSource.getMaximumZoomLevel();
	}

	@Override
	public void setTileSource(ITileSource tileSource) {
		//prevent re-assignment of tile source
		if (tileSource instanceof MFTileSource) {
			this.tileSource = (MFTileSource) tileSource;
		}
	}
	
	private class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState pState) {
			return tileSource.renderTile(pState.getMapTile());
		}
	}
}
