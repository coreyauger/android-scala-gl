package com.affinetechnology.draw

import android.os.Environment
import java.io.File
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import android.graphics.Bitmap
import java.io.FileOutputStream
import android.graphics.BitmapFactory

object ImageUtils {

    private val TAG = "ImageUtils"
	
	def getAlbumDir(albumName: String): File = {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),albumName)			
		} else {
			Log.v(TAG, "External storage is not mounted READ/WRITE.");
			null
		}			
	}
    
    def loadBitmap(imgpath: String, scale: Float ): Bitmap = {
      val bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imgpath, bmOptions);
		val photoW = bmOptions.outWidth;
		val photoH = bmOptions.outHeight;
		
		// TODO: this is pretty arbitrary right now..
		val targetW = (photoW * scale).asInstanceOf[Int]
		val targetH = (photoH * scale).asInstanceOf[Int]
		
		// Figure out which way needs to be reduced less 
		var scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}

		// Set bitmap options to scale the image decode target
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		// Decode the JPEG file into a Bitmap		
		BitmapFactory.decodeFile(imgpath, bmOptions);
    }      
    
	
	def createImageFile(albumName: String): File = {
	    // Create an image file name
	    val timeStamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date);
	    val imageFileName =  timeStamp//String = new String( "IMG_%s_" ).format(timeStamp)
	    val image = File.createTempFile(
	        imageFileName, 
	        ".jpg", 
	        ImageUtils.getAlbumDir(albumName)
	    );
	    //mCurrentPhotoPath = image.getAbsolutePath();
	    return image;
	}
	
	
	def saveBitmapToGallery(bitmap: Bitmap, gallery: String ): File = {
		  Log.d(TAG, "saveBitmapToGallery")
		  val file = createImageFile(gallery)
	      val out = new FileOutputStream(file)
	      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
	      out.flush();
	      out.close();
	      file	  
	}
}