package com.arman.osmdroidmapsforge.map;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.BitmapPool;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.ITileSource;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

public class MFTileSource implements ITileSource{

	private final int mMinimumZoomLevel;
	private final int mMaximumZoomLevel;
	private final int mTileSizePixels;
	
	static final String TAG = "MFTileSource";
	private static String mapFilePath = null;
	private static File mapFile = null;
	
	private static AndroidGraphicFactory androidGraphicFactory;
	private static DisplayModel displayModel ;
	private static MapViewPosition mapViewPosition ;
	private static DatabaseRenderer databaseRenderer;
	private static XmlRenderTheme xmlRenderTheme;
	private static RendererJob rendererJob ;
	private static Context context;
	private static RenderThemeFuture renderThemeFuture;

	private static MapDataStore mapDataStore;

	public MFTileSource(int minZoom, int maxZoom, int tileSizePixels, String mapsFolder, String renderThemeAddr, Context context) {
			mTileSizePixels = tileSizePixels;
			mMinimumZoomLevel = minZoom;
			mMaximumZoomLevel = maxZoom;
			this.context = context;

			initialize(mapsFolder, renderThemeAddr, context);
	}
	
	
	public static boolean initialize(String locationOfMapFile,String renderThemeAddr, Context _context) {
		Log.i(TAG, "Initilizing started");
		mapFilePath = locationOfMapFile;
		androidGraphicFactory = AndroidGraphicFactory.INSTANCE;
		
		mapFile = new File(mapFilePath);
		
		displayModel = new DisplayModel();

		
		mapViewPosition = new MapViewPosition(displayModel);
		
		mapDataStore = new MapFile(mapFile);

		displayModel.setFixedTileSize(256);
		
		TileCache tileCache = AndroidUtil.createTileCache(context, "111", 256, 1.0f, 1.2, false, 0);
		
		databaseRenderer = new DatabaseRenderer(mapDataStore, androidGraphicFactory, tileCache);
		context = _context;
		
		try {
			xmlRenderTheme = new ExternalRenderTheme(new File(renderThemeAddr	));
			Log.i(TAG, "xmlRenderTheme initialized successfully");
		}
		catch(Throwable e)
		{
			Log.i(TAG, "xmlRenderTheme error initializing ");
			e.printStackTrace();
		}

		renderThemeFuture = new RenderThemeFuture(AndroidGraphicFactory.INSTANCE, xmlRenderTheme, displayModel);
		new Thread(renderThemeFuture).run();

		Log.i(TAG, "Mapsforge Initializing Ended");
		
		return true;
	}
	
	
	@Override
	public int getMinimumZoomLevel() {
		return mMinimumZoomLevel;
	}
	
	@Override
	public int getMaximumZoomLevel() {
		return mMaximumZoomLevel;
	}
	
	@Override
	public int getTileSizePixels() {
		return mTileSizePixels;
	}
	
	
	
	@Override
	public Drawable getDrawable(final String aFilePath) {
		try {
			// default implementation will load the file as a bitmap and create
			// a BitmapDrawable from it
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			BitmapPool.getInstance().applyReusableOptions(bitmapOptions);
			final Bitmap bitmap = BitmapFactory.decodeFile(aFilePath, bitmapOptions);
			if (bitmap != null) {
				return new ReusableBitmapDrawable(bitmap);
			} else {
				// if we couldn't load it then it's invalid - delete it
				try {
					new File(aFilePath).delete();
				} catch (final Throwable e) {
//					logger.error("Error deleting invalid file: " + aFilePath, e);
				}
			}
		} catch (final OutOfMemoryError e) {
//			logger.error("OutOfMemoryError loading bitmap: " + aFilePath);
			System.gc();
		}
		return null;
	}


	
	@Override
	public Drawable getDrawable(final InputStream aFileInputStream) {
		try {
			// default implementation will load the file as a bitmap and create
			// a BitmapDrawable from it
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			BitmapPool.getInstance().applyReusableOptions(bitmapOptions);
			final Bitmap bitmap = BitmapFactory.decodeStream(aFileInputStream, null, bitmapOptions);
			if (bitmap != null) {
				return new ReusableBitmapDrawable(bitmap);
			}
		} catch (final OutOfMemoryError e) {
//			logger.error("OutOfMemoryError loading bitmap");
			System.gc();
//			throw new LowMemoryException(e);
		}
		return null;
	}
	

	public final class LowMemoryException extends Exception {
		private static final long serialVersionUID = 146526524087765134L;

		public LowMemoryException(final String pDetailMessage) {
			super(pDetailMessage);
		}

		public LowMemoryException(final Throwable pThrowable) {
			super(pThrowable);
		}
	}


	// Save rendered tile to cache folder
	private class SaveTileAsync extends AsyncTask<Object, Integer, Boolean> {
	    @Override
	    protected Boolean doInBackground(Object... objects) {

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				((AndroidBitmap)objects[0]).compress(bos);
			
				byte[] bitmapdata = bos.toByteArray();

				File cachedTile = new File((String)objects[1]);


				//write the bytes in file
				FileOutputStream fos = new FileOutputStream(cachedTile);
				fos.write(bitmapdata);
				fos.flush();
				fos.close();
				
			} catch (Throwable e) {
				e.printStackTrace();
			}
	    	return true;
	    }
	    @Override
	    protected void onPostExecute(Boolean result) {
	    }

	    @Override
	    protected void onProgressUpdate(Integer... values) {
	        super.onProgressUpdate(values);
	    }
	}


	private Object renderLock = new Object();
	private int numOfConcurrentRenders = 0;

	@SuppressWarnings("deprecation")
	public Drawable renderTile(MapTile pTile) {

		synchronized (renderLock) {
			// Allow to have 10 concurrent tile renders, if it exceeds 10 concurrent renders,
			// wait for previous renders to complete.
			if (numOfConcurrentRenders > 10) {
				try {
					renderLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			numOfConcurrentRenders++;

			String cachedTilePath = context.getFilesDir().getPath() + "/MapCache/MapsForge/" +
					pTile.getZoomLevel() + "/" + pTile.getX() + "/" + pTile.getY() + ".png";

			File cachedTile = new File(cachedTilePath);
			if (cachedTile.exists()) {
				numOfConcurrentRenders--;
				renderLock.notifyAll();

				return new BitmapDrawable(BitmapFactory.decodeFile(cachedTilePath));
			} else {
				Tile tile = new Tile(pTile.getX(), pTile.getY(), (byte) pTile.getZoomLevel(), mTileSizePixels);

				rendererJob = new RendererJob(tile, mapDataStore, renderThemeFuture, displayModel, 0.8f, true, false);


				try {
					AndroidBitmap bitmap = (AndroidBitmap) databaseRenderer.executeJob(rendererJob);

					if (bitmap != null) {
						new File(context.getFilesDir().getPath() + "/MapCache/MapsForge/" +
								pTile.getZoomLevel() + "/" + pTile.getX()).mkdirs();

						SaveTileAsync saveTileAsync = new SaveTileAsync();
						saveTileAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap, cachedTilePath);


						Drawable d = new BitmapDrawable(AndroidGraphicFactory.getBitmap(bitmap));
						numOfConcurrentRenders--;
						renderLock.notifyAll();

						return d;
					}
				} catch (Throwable e) {
					Log.i("TileCache", "TileCache get failed");
					Log.i("TileCache", e.getMessage());
					numOfConcurrentRenders--;
					renderLock.notifyAll();
					return null;
				}
				return null;
			}
		}
	}


	// Use it for clear cache folder, currently has no usage.
	private static void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);

	    fileOrDirectory.delete();
	}

	@Override
	public int ordinal() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String localizedName(ResourceProxy proxy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTileRelativeFilenameString(MapTile aTile) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
