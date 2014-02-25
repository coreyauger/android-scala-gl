package com.affinetechnology.gl

import java.nio.FloatBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.opengl.GLES20
import android.graphics.BitmapFactory
import android.opengl.GLUtils
import com.affinetechnology.draw
import android.util.Log
import android.graphics.Rect
import android.opengl.GLES11Ext
import javax.microedition.khronos.opengles.GL10

abstract trait Drawable {	
	def draw(matrix: Array[Float])		
}

class TextureInfo( val textureId: Int, val originalWidth: Int, val originalHeight: Int ) {  
}

object Drawable  {
  
  private val TAG = "Drawable"
  val COORDS_PER_VERTEX = 3
  val COORDS_PER_TEX = 2
  
  def loadShader(t: Int, shaderCode: String): Int = {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        val shader = GLES20.glCreateShader(t);
        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        shader
    }

    def checkGlError(glOperation: String ): Unit = {
        val error: Int = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            val sb = new java.lang.StringBuilder();
            sb.append(glOperation);
            sb.append(": glError ")
            sb.append(error)
        	android.util.Log.e(TAG, sb.toString());
            throw new RuntimeException(sb.toString());
            checkGlError(glOperation)
        }
    }
    
    def createVideoTexture(): Int = {
    	val textures = new Array[Int](1)           				
		GLES20.glGenTextures(1, textures, 0);		
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures(0));
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);
		textures(0)
    }
    
    def createTexture(): Int = {
    	val textures = new Array[Int](1)           				
		GLES20.glGenTextures(1, textures, 0);
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures(0)); 
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	    textures(0)
    }
           
    def loadGLTexture(imgpath: String): TextureInfo = {
		val texBank = createTexture
	    /* Decode the JPEG file into a Bitmap */		
		Log.d(TAG,"load img: " +imgpath)
		val bitmap = com.affinetechnology.draw.ImageUtils.loadBitmap(imgpath, .5f)
	    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		
		// Clean up
		bitmap.recycle();
		
		new TextureInfo(texBank, bitmap.getWidth, bitmap.getHeight)
	}        
}

abstract class DrawableObject(vshader: String, fshader: String, val verts: Array[Float], val color: Array[Float], val texture: Array[Float], val textureBank: Int = -1, val opts: Map[String, Int] = Map() ) extends Drawable {
	def getVerrtexShaderCode = vshader
	def getFragShaderCode = fshader	
	
	def morph(newVerts: Array[Float]) = {
	  vertexBuffer.put(newVerts)
	  // set the buffer to read the first coordinate
	  vertexBuffer.position(0)
	}
	
	val bb = ByteBuffer.allocateDirect(verts.length * 4)
    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());
    // create a floating point buffer from the ByteBuffer
    val vertexBuffer = bb.asFloatBuffer
    // add the coordinates to the FloatBuffer
    vertexBuffer.put(verts)
    // set the buffer to read the first coordinate
    vertexBuffer.position(0)
    
    val len = if(texture != null) texture.length else 0
    val bbt = ByteBuffer.allocateDirect( len * 4)
    // use the device hardware's native byte order
    bbt.order(ByteOrder.nativeOrder());
    // create a floating point buffer from the ByteBuffer
    val textureBuffer = bbt.asFloatBuffer();
    
    if( texture != null ){
	    
	    // add the coordinates to the FloatBuffer
	    textureBuffer.put(texture);
	    // set the buffer to read the first coordinate
	    textureBuffer.position(0);
    }

    // prepare shaders and OpenGL program
    val vertexShader = Drawable.loadShader(GLES20.GL_VERTEX_SHADER,getVerrtexShaderCode);
    val fragmentShader = Drawable.loadShader(GLES20.GL_FRAGMENT_SHADER,getFragShaderCode);

    val program = GLES20.glCreateProgram();         // create empty OpenGL Program
    GLES20.glAttachShader(program, vertexShader);   // add the vertex shader to program
    GLES20.glAttachShader(program, fragmentShader); // add the fragment shader to program
    GLES20.glLinkProgram(program);                  // create OpenGL program executables
	
	
    def draw(mvpMatrix: Array[Float]) = {
          	      
        // Add program to OpenGL environment
        GLES20.glUseProgram(program);
        
        
        if(textureBank >= 0){
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    	  	if( opts.contains("glBindTexture") )GLES20.glBindTexture(opts("glBindTexture"), textureBank);    	    
    	  	else GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBank);
    	  	// 	get handle to vertex shader's vPosition member
	      	val texHandle = GLES20.glGetAttribLocation(program, "vTexCoordinate");
	      	
	        // Enable a handle to the triangle vertices
	        GLES20.glEnableVertexAttribArray(texHandle);
	
	        // Prepare the triangle coordinate data
	        GLES20.glVertexAttribPointer(texHandle, Drawable.COORDS_PER_TEX,
	                                     GLES20.GL_FLOAT, false,
	                                     Drawable.COORDS_PER_TEX * 4, textureBuffer);
        
    	}else{
    		// get handle to fragment shader's vColor member
    		val colorHandle = GLES20.glGetUniformLocation(program, "vColor");

        	// 	Set color for drawing the triangle
        	GLES20.glUniform4fv(colorHandle, 1, color, 0);
    	}
    	      

        // get handle to vertex shader's vPosition member
        val posHandle = GLES20.glGetAttribLocation(program, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(posHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(posHandle, Drawable.COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     Drawable.COORDS_PER_VERTEX * 4 , vertexBuffer);	// 4 bytes per float

         

        // get handle to shape's transformation matrix
        val matrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
       // Drawable.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0);
        //Drawable.checkGlError("glUniformMatrix4fv");

        // Draw the triangle
        if( opts.contains("glDrawArrays"))GLES20.glDrawArrays(opts("glDrawArrays"), 0, verts.length / Drawable.COORDS_PER_VERTEX);
        else GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, verts.length / Drawable.COORDS_PER_VERTEX);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(posHandle);
    }
    
}


class Triangle(v: Array[Float], c: Array[Float], t: Array[Float] = null, tex: Int = -1 ) extends DrawableObject("uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = vPosition * uMVPMatrix;" +
        "}", 
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}"
        , v, c, t, tex) {	
}



class Line(v: Array[Float], c: Array[Float]) extends DrawableObject(// This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}",
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}",
        v,c, null, -1, Map("glDrawArrays" -> GLES20.GL_LINES)){
}


class Square(v: Array[Float], c: Array[Float], t: Array[Float], tex:Int ) extends DrawableObject(// This matrix member variable provides a hook to manipulate
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec2 vTexCoordinate;" +
        "varying vec2 v_TexCoordinate;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  v_TexCoordinate = vTexCoordinate;" +
        "  gl_Position = vPosition * uMVPMatrix;" +
        "}",
        // the matrix must be included as a modifier of gl_Position
        if( tex >= 0 )
        "precision mediump float;"+
		"varying vec2 v_TexCoordinate;"+
		"uniform sampler2D u_Texture;"+		
		"void main() {"+
		  "gl_FragColor = texture2D(u_Texture, v_TexCoordinate);"+
		"}" else 
		"precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}",
        v,c, t, tex){
}


class Sprite(v: Array[Float], c: Array[Float], t: Array[Float], tex:Int ) extends Square(v, c, t, tex )




class DirectVideo(v: Array[Float], c: Array[Float], t: Array[Float], tex:Int ) extends DrawableObject(// This matrix member variable provides a hook to manipulate
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "#extension GL_OES_EGL_image_external : require\n"+
		"uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec2 vTexCoordinate;" +
        "varying vec2 v_TexCoordinate;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  v_TexCoordinate = vTexCoordinate;" +
        "  gl_Position = vPosition * uMVPMatrix;" +
        "}",
        // the matrix must be included as a modifier of gl_Position
        "#extension GL_OES_EGL_image_external : require\n"+
        "precision mediump float;"+
		"varying vec2 v_TexCoordinate;"+
		"uniform samplerExternalOES u_Texture;"+		
		"void main() {"+
		  "gl_FragColor = texture2D(u_Texture, v_TexCoordinate);"+
		"}" ,
        v,c, t, tex, Map("glBindTexture" -> GLES11Ext.GL_TEXTURE_EXTERNAL_OES)){
}



class CropBox (val maxWidth: Float, val maxHeight: Float, val width: Float, val height: Float, val depth: Float ) extends Drawable {
  // left -maxWidth/2
  
  private val maxHW = (maxWidth/2.0f)		
  private val maxHH = (maxHeight/2.0f)
  private val hW = width/2.0f
  private val hH = height/2.0f
  
  private val sideW = maxHW - hW
 
  private val blackout = Array(0.0f, 0.0f, 0.0f, 0.9f)
  
  var lastWidth = hW;
  var lastHeight = hH;
  
  def getCropPoints(): Array[Float] = {
    // TODO: this got messed and is somehow reversed...
    Array(lastWidth, lastHeight, -lastWidth, -lastHeight)
    //Array(lastHeight, lastWidth, -lastHeight, -lastWidth)
  }
  
  def scale(xf: Float, yf: Float) = {
    val hx = (2.0f) * xf
    val wx = (2.0f) * yf
    
    val h = if( hx < 0 ) 0 else if( hx > maxHH ) maxHH else hx
    val w = if( wx < 0 ) 0 else if( wx > maxHH ) maxHH else wx
    
    lastWidth = w
    lastHeight = h
    
    left.morph(Array(-sideW, maxHH, depth,   // top left
                -sideW, -maxHH, depth,   // bottom left
                 -h,  maxHH, depth,	  // top right
                 -sideW, -maxHH, depth,  // bottom left	
                 -h, -maxHH, depth,   // bottom right
                 -h,  maxHH, depth ))// top right)

     right.morph(Array(sideW, maxHH, depth,   // top left
                sideW, -maxHH, depth,   // bottom left
                 h,  maxHH, depth,	  // top right
                 sideW, -maxHH, depth,  // bottom left	
                 h, -maxHH, depth,   // bottom right
                 h,  maxHH, depth ))
    top.morph(Array(-h, maxHH, depth,   // top left
                -h, w, depth,   // bottom left
                 h,  maxHH, depth,	  // top right
                 -h, w, depth,  // bottom left	
                 h, w, depth,   // bottom right
                 h,  maxHH, depth ))// top right 
     bottom.morph(Array(-h, -maxHH, depth,   // top left
                -h, -w, depth,   // bottom left
                 h,  -maxHH, depth,	  // top right
                 -h, -w, depth,  // bottom left	
                 h, -w, depth,   // bottom right
                 h,  -maxHH, depth ))// top right 
  }
  
  private val left = new Square(
          Array(-sideW, maxHH, depth,   // top left
                -sideW, -maxHH, depth,   // bottom left
                 -hW,  maxHH, depth,	  // top right
                 -sideW, -maxHH, depth,  // bottom left	
                 -hW, -maxHH, depth,   // bottom right
                 -hW,  maxHH, depth ),// top right
          blackout,
          null, -1)
  private val right = new Square(
          Array(sideW, maxHH, depth,   // top left
                sideW, -maxHH, depth,   // bottom left
                 hW,  maxHH, depth,	  // top right
                 sideW, -maxHH, depth,  // bottom left	
                 hW, -maxHH, depth,   // bottom right
                 hW,  maxHH, depth ),// top right
          blackout,
          null, -1)
  
  private val top = new Square(
          Array(-hW, maxHH, depth,   // top left
                -hW, hH, depth,   // bottom left
                 hW,  maxHH, depth,	  // top right
                 -hW, hH, depth,  // bottom left	
                 hW, hH, depth,   // bottom right
                 hW,  maxHH, depth ),// top right
          blackout,
          null, -1)
  private val bottom = new Square(
          Array(-hW, -maxHH, depth,   // top left
                -hW, -hH, depth,   // bottom left
                 hW,  -maxHH, depth,	  // top right
                 -hW, -hH, depth,  // bottom left	
                 hW, -hH, depth,   // bottom right
                 hW,  -maxHH, depth ),// top right
          blackout,
          null, -1)
  
  private val sides = List(left, right, top, bottom)
  
  override def draw(mvpMatrix: Array[Float]) = {
	  sides.foreach( s => s.draw(mvpMatrix) )
  }
}


