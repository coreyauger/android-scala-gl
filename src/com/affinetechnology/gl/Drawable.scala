package com.affinetechnology.gl

import java.nio.FloatBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.opengl.GLES20
import android.graphics.BitmapFactory
import android.opengl.GLUtils
import com.affinetechnology.draw

abstract class Drawable {	
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
           
    def loadGLTexture(imgpath: String): TextureInfo = {
		// loading texture
		/* Get the size of the image */
		val textures = new Array[Int](1)
      
      
		
		/* Decode the JPEG file into a Bitmap */		
		val bitmap = com.affinetechnology.draw.ImageUtils.loadBitmap(imgpath, .5f)
	
		GLES20.glGenTextures(1, textures, 0);
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures(0)); 
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	
	    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		
		// Clean up
		bitmap.recycle();
		
		new TextureInfo(textures(0), bitmap.getWidth, bitmap.getHeight)
	}

}

abstract class DrawableObject(vshader: String, fshader: String, val verts: Array[Float], val color: Array[Float], val texture: Array[Float], val textureBank: Int = -1, val drawMode: Int = GLES20.GL_TRIANGLES ) extends Drawable {
	def getVerrtexShaderCode = vshader
	def getFragShaderCode = fshader	
	
	
	val bb = ByteBuffer.allocateDirect(verts.length * 4)
    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());
    // create a floating point buffer from the ByteBuffer
    val vertexBuffer = bb.asFloatBuffer();
    // add the coordinates to the FloatBuffer
    vertexBuffer.put(verts);
    // set the buffer to read the first coordinate
    vertexBuffer.position(0);
    
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
    	  GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBank);
    	   // get handle to vertex shader's vPosition member
	        val texHandle = GLES20.glGetAttribLocation(program, "vTexCoordinate");
	
	        // Enable a handle to the triangle vertices
	        GLES20.glEnableVertexAttribArray(texHandle);
	
	        // Prepare the triangle coordinate data
	        GLES20.glVertexAttribPointer(texHandle, Drawable.COORDS_PER_TEX,
	                                     GLES20.GL_FLOAT, false,
	                                     Drawable.COORDS_PER_TEX * 4, textureBuffer);
        
    	}
    	      

        // get handle to vertex shader's vPosition member
        val posHandle = GLES20.glGetAttribLocation(program, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(posHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(posHandle, Drawable.COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     Drawable.COORDS_PER_VERTEX * 4 , vertexBuffer);	// 4 bytes per float

        // get handle to fragment shader's vColor member
        val colorHandle = GLES20.glGetUniformLocation(program, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        val matrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        Drawable.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0);
        Drawable.checkGlError("glUniformMatrix4fv");

        // Draw the triangle
        GLES20.glDrawArrays(drawMode, 0, verts.length / Drawable.COORDS_PER_VERTEX);

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
        v,c, null, -1, GLES20.GL_LINES){
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
        v,c, t, tex, GLES20.GL_TRIANGLES){
}


class Sprite(v: Array[Float], c: Array[Float], t: Array[Float], tex:Int ) extends Square(v, c, t, tex )

class CropBox (val maxWidth: Float, val maxHeight: Float, val width: Float, val height: Float, val depth: Float ) extends Drawable {
  // left -maxWidth/2
  
  private val maxHW = (maxWidth/2.0f)
  private val maxHH = (maxHeight/2.0f)
  private val hW = width/2.0f
  private val hH = height/2.0f
  
  private val sideW = maxHW - hW
 
  private val blackout = Array(0.0f, 0.0f, 0.0f, 0.7f)
  
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

/*

class Sprite2(val textureid: Int, val imgPath: String, w: Float, h: Float) {

    private val TAG = "SpriteRender";
        
    private val vertexShaderCode =
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
        "}";
    
    private val fragmentShaderCode =
        "precision mediump float;"+
		"varying vec2 v_TexCoordinate;"+
		"uniform sampler2D u_Texture;"+		
		"uniform vec4 vColor;" +
		"void main() {"+
		  "gl_FragColor = texture2D(u_Texture, v_TexCoordinate);"+
		"}";
    
    
    
    private var vertexBuffer: FloatBuffer = null
    private var texturebuffer: FloatBuffer  = null;
    private var drawListBuffer: ShortBuffer = null
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mTextureHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0

    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX: Int = 3;
    
    
    
    morph( Array( 	-0.5f*w,  0.5f*h, 0.0f,   // top left
                -0.5f*w, -0.5f*h, 0.0f,   // bottom left
                 0.5f*w, -0.5f*h, 0.0f,   // bottom right
                 0.5f*w,  0.5f*h, 0.0f ) ); // top right
    // u,v 
    val COORDS_PER_TEX: Int = 2;
    val texturedata =   Array(0.0f, 0.0f,
    						  1.0f, 0.0f,
                              1.0f, 1.0f,
                              0.0f, 1.0f);
           
    private val drawOrder = Array( 0, 1, 2, 0, 2, 3 ); // order to draw vertices

    private val vertexStride: Int  = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private val texStride: Int = COORDS_PER_TEX * 4;

    // Set color with red, green, blue and alpha (opacity) values
    val color = Array( 0.2f, 0.709803922f, 0.898039216f, 1.0f );    
    
    val bb_texture = ByteBuffer.allocateDirect(texturedata.length * 4);
    bb_texture.order(ByteOrder.nativeOrder());
    texturebuffer = bb_texture.asFloatBuffer();
    texturebuffer.put(texturedata);
    texturebuffer.position(0);

    // initialize byte buffer for the draw list
    val dlb = ByteBuffer.allocateDirect(
    // (# of coordinate values * 2 bytes per short)
            drawOrder.length * 2);
    dlb.order(ByteOrder.nativeOrder());
   // drawListBuffer = dlb.asShortBuffer();
    drawListBuffer = dlb.asShortBuffer();
    //for( i <- 0 to drawOrder.length )drawListBuffer.put(i, drawOrder(i).asInstanceOf[Short])
    // (CA) - NOTE that this needs to be this way :(
    drawListBuffer.put(0, 0)
    drawListBuffer.put(1, 1)
    drawListBuffer.put(2, 2)
    drawListBuffer.put(3, 0)
    drawListBuffer.put(4, 2)
    drawListBuffer.put(5, 3)
    
    //drawListBuffer.put(drawOrder.asInstanceOf[Array[Short]]);
    drawListBuffer.position(0);
    
    val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    mProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(mProgram, fragmentShader);
    GLES20.glAttachShader(mProgram, vertexShader);
    GLES20.glLinkProgram(mProgram);
    
    val textureInf = MyGLRenderer.loadGLTexture(imgPath);
        
    def morph( coords: Array[Float] ) = {
	      // initialize vertex byte buffer for shape coordinates
	    val bb = ByteBuffer.allocateDirect(
	    // (# of coordinate values * 4 bytes per float)
	            coords.length * 4);
	    bb.order(ByteOrder.nativeOrder());
	    vertexBuffer = bb.asFloatBuffer();
	    vertexBuffer.put(coords);
	    vertexBuffer.position(0);
    }
    
    def draw(mvpMatrix: Array[Float]) = {                  
      // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);        

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInf._1);
        
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);
        
        
        
        // get handle to vertex shader's vPosition member
        mTextureHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mTextureHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mTextureHandle, COORDS_PER_TEX,
                                     GLES20.GL_FLOAT, false,
                                     texStride, texturebuffer);
        
        

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
      
    }

}





class Triangle extends Drawable {

    def getVerrtexShaderCode = 
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = vPosition * uMVPMatrix;" +
        "}";

    def getFragShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

   // private var vertexBuffer: FloatBuffer = null
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0

    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX = 3;
    val triangleCoords = Array( // in counterclockwise order:
         0.0f,  0.622008459f, 0.0f,   // top
        -0.5f, -0.311004243f, 0.0f,   // bottom left
         0.5f, -0.311004243f, 0.0f    // bottom right
    );
    private val vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private val vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    val color = Array( 0.63671875f, 0.76953125f, 0.22265625f, 1.0f )

    // initialize vertex byte buffer for shape coordinates
    val bb = ByteBuffer.allocateDirect(
            // (number of coordinate values * 4 bytes per float)
            triangleCoords.length * 4);
    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());

    // create a floating point buffer from the ByteBuffer
    val vertexBuffer = bb.asFloatBuffer();
    // add the coordinates to the FloatBuffer
    vertexBuffer.put(triangleCoords);
    // set the buffer to read the first coordinate
    vertexBuffer.position(0);

    // prepare shaders and OpenGL program
    val vertexShader = Drawable.loadShader(GLES20.GL_VERTEX_SHADER,getVerrtexShaderCode);
    val fragmentShader = Drawable.loadShader(GLES20.GL_FRAGMENT_SHADER,getFragShaderCode);

    val program = GLES20.glCreateProgram();             // create empty OpenGL Program
    GLES20.glAttachShader(program, vertexShader);   // add the vertex shader to program
    GLES20.glAttachShader(program, fragmentShader); // add the fragment shader to program
    GLES20.glLinkProgram(program);                  // create OpenGL program executables



    def draw(mvpMatrix: Array[Float]) = {
        // Add program to OpenGL environment
        GLES20.glUseProgram(program);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(program, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(program, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        Drawable.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        Drawable.checkGlError("glUniformMatrix4fv");

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}


class Square {

	val TAG = "Square"
  
    private val vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = vPosition * uMVPMatrix;" +
        "}";

    private val fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    private var vertexBuffer: FloatBuffer = null
    private var drawListBuffer: ShortBuffer = null
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0

    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX: Int = 3;
    val squareCoords = Array( 		-0.5f,  0.5f, 0.0f,   // top left
                                    -0.5f, -0.5f, 0.0f,   // bottom left
                                     0.5f, -0.5f, 0.0f,   // bottom right
                                     0.5f,  0.5f, 0.0f ); // top right
           
    private val drawOrder = Array( 0, 1, 2, 0, 2, 3 ); // order to draw vertices

    private val vertexStride: Int  = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    val color = Array( 0.2f, 0.709803922f, 0.898039216f, 1.0f );

    // initialize vertex byte buffer for shape coordinates
    val bb = ByteBuffer.allocateDirect(
    // (# of coordinate values * 4 bytes per float)
            squareCoords.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(squareCoords);
    vertexBuffer.position(0);

    // initialize byte buffer for the draw list
    val dlb = ByteBuffer.allocateDirect(
    // (# of coordinate values * 2 bytes per short)
            drawOrder.length * 2);
    dlb.order(ByteOrder.nativeOrder());
   // drawListBuffer = dlb.asShortBuffer();
    drawListBuffer = dlb.asShortBuffer();
    //for( i <- 0 to drawOrder.length )drawListBuffer.put(i, drawOrder(i).asInstanceOf[Short])
    // (CA) - NOTE that this needs to be this way :(
    drawListBuffer.put(0, 0)
    drawListBuffer.put(1, 1)
    drawListBuffer.put(2, 2)
    drawListBuffer.put(3, 0)
    drawListBuffer.put(4, 2)
    drawListBuffer.put(5, 3)
    
    //drawListBuffer.put(drawOrder.asInstanceOf[Array[Short]]);
    drawListBuffer.position(0);

    // prepare shaders and OpenGL program
    val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                                               vertexShaderCode);
    val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                 fragmentShaderCode);

    mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
    GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
/*
    var linkStatus = Array(0); 
    GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
    if (linkStatus(0) == 0){
        // Displays error message
      Log.e(TAG, "Failed to link Square shader")
    }
  */  
    def draw(mvpMatrix: Array[Float]) = {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}




class Sprite(val textureid: Int, val imgPath: String, w: Float, h: Float) {

    private val TAG = "SpriteRender";
        
    private val vertexShaderCode =
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
        "}";
    
    private val fragmentShaderCode =
        "precision mediump float;"+
		"varying vec2 v_TexCoordinate;"+
		"uniform sampler2D u_Texture;"+		
		"void main() {"+
		  "gl_FragColor = texture2D(u_Texture, v_TexCoordinate);"+
		"}";
    
    
    
    private var vertexBuffer: FloatBuffer = null
    private var texturebuffer: FloatBuffer  = null;
    private var drawListBuffer: ShortBuffer = null
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mTextureHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0

    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX: Int = 3;
    
    
    
    morph( Array( 	-0.5f*w,  0.5f*h, 0.0f,   // top left
                -0.5f*w, -0.5f*h, 0.0f,   // bottom left
                 0.5f*w, -0.5f*h, 0.0f,   // bottom right
                 0.5f*w,  0.5f*h, 0.0f ) ); // top right
    // u,v 
    val COORDS_PER_TEX: Int = 2;
    val texturedata =   Array(0.0f, 0.0f,  // top left
    						  1.0f, 0.0f,  // bottom left
                              1.0f, 1.0f,  // bottom right
                              0.0f, 1.0f); // top right
           
    private val drawOrder = Array( 0, 1, 2, 0, 2, 3 ); // order to draw vertices

    private val vertexStride: Int  = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private val texStride: Int = COORDS_PER_TEX * 4;

    // Set color with red, green, blue and alpha (opacity) values
    val color = Array( 0.2f, 0.709803922f, 0.898039216f, 1.0f );    
    
    val bb_texture = ByteBuffer.allocateDirect(texturedata.length * 4);
    bb_texture.order(ByteOrder.nativeOrder());
    texturebuffer = bb_texture.asFloatBuffer();
    texturebuffer.put(texturedata);
    texturebuffer.position(0);

    // initialize byte buffer for the draw list
    val dlb = ByteBuffer.allocateDirect(
    // (# of coordinate values * 2 bytes per short)
            drawOrder.length * 2);
    dlb.order(ByteOrder.nativeOrder());
   // drawListBuffer = dlb.asShortBuffer();
    drawListBuffer = dlb.asShortBuffer();
    //for( i <- 0 to drawOrder.length )drawListBuffer.put(i, drawOrder(i).asInstanceOf[Short])
    // (CA) - NOTE that this needs to be this way :(
    drawListBuffer.put(0, 0)
    drawListBuffer.put(1, 1)
    drawListBuffer.put(2, 2)
    drawListBuffer.put(3, 0)
    drawListBuffer.put(4, 2)
    drawListBuffer.put(5, 3)
    
    //drawListBuffer.put(drawOrder.asInstanceOf[Array[Short]]);
    drawListBuffer.position(0);
    
    val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    mProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(mProgram, fragmentShader);
    GLES20.glAttachShader(mProgram, vertexShader);
    GLES20.glLinkProgram(mProgram);
    
    val textureInf = MyGLRenderer.loadGLTexture(imgPath);
        
    def morph( coords: Array[Float] ) = {
	      // initialize vertex byte buffer for shape coordinates
	    val bb = ByteBuffer.allocateDirect(
	    // (# of coordinate values * 4 bytes per float)
	            coords.length * 4);
	    bb.order(ByteOrder.nativeOrder());
	    vertexBuffer = bb.asFloatBuffer();
	    vertexBuffer.put(coords);
	    vertexBuffer.position(0);
    }
    
    def draw(mvpMatrix: Array[Float]) = {                  
      // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);        

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInf._1);
        
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);
        
        
        
        // get handle to vertex shader's vPosition member
        mTextureHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mTextureHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mTextureHandle, COORDS_PER_TEX,
                                     GLES20.GL_FLOAT, false,
                                     texStride, texturebuffer);
        
        

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
      
    }

}




class Line(LineCoords: Array[Float]) {

	private var vertexBuffer: FloatBuffer = null

	private val vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}";

	private val fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

	protected var GlProgram = 0;
	protected var PositionHandle = 0;
	protected var ColorHandle = 0;
	protected var MVPMatrixHandle = 0;

// number of coordinates per vertex in this array
	val COORDS_PER_VERTEX = 3;

	private val VertexCount = LineCoords.length / COORDS_PER_VERTEX;
	private val VertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

	// 	Set color with red, green, blue and alpha (opacity) values
	val color = Array( 1.0f, 1.0f, 1.0f, 0.4f )

    // initialize vertex byte buffer for shape coordinates
    val bb = ByteBuffer.allocateDirect(
            // (number of coordinate values * 4 bytes per float)
            LineCoords.length * 4);
    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());

    // create a floating point buffer from the ByteBuffer
    vertexBuffer = bb.asFloatBuffer();
    // add the coordinates to the FloatBuffer
    vertexBuffer.put(LineCoords);
    // set the buffer to read the first coordinate
    vertexBuffer.position(0);


    val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    GlProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
    GLES20.glAttachShader(GlProgram, vertexShader);   // add the vertex shader to program
    GLES20.glAttachShader(GlProgram, fragmentShader); // add the fragment shader to program
    GLES20.glLinkProgram(GlProgram);                  // creates OpenGL ES program executables


	def SetVerts(v0: Float, v1: Float, v2: Float, v3: Float, v4: Float, v5: Float) =
	{
		vertexBuffer.put(Array(v0,v1,v2,v3,v4,v5));
	    // set the buffer to read the first coordinate
	    vertexBuffer.position(0);
	
	}
	
	def SetColor(red: Float, green: Float, blue: Float, alpha: Float) =
	{
	    color(0) = red;
	    color(1) = green;
	    color(2) = blue;
	    color(3) = alpha;
	}

	def draw(mvpMatrix: Array[Float]) = {
	    // Add program to OpenGL ES environment
	    GLES20.glUseProgram(GlProgram);
	
	    // get handle to vertex shader's vPosition member
	    PositionHandle = GLES20.glGetAttribLocation(GlProgram, "vPosition");
	
	    // Enable a handle to the triangle vertices
	    GLES20.glEnableVertexAttribArray(PositionHandle);
	
	    // Prepare the triangle coordinate data
	    GLES20.glVertexAttribPointer(PositionHandle, COORDS_PER_VERTEX,
	                                 GLES20.GL_FLOAT, false,
	                                 VertexStride, vertexBuffer);
	
	    // get handle to fragment shader's vColor member
	    ColorHandle = GLES20.glGetUniformLocation(GlProgram, "vColor");
	
	    // Set color for drawing the triangle
	    GLES20.glUniform4fv(ColorHandle, 1, color, 0);
	
	    // get handle to shape's transformation matrix
	    MVPMatrixHandle = GLES20.glGetUniformLocation(GlProgram, "uMVPMatrix");
	
	    // Apply the projection and view transformation
	    GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, mvpMatrix, 0);
	
	
	    // Draw the triangle
	    GLES20.glDrawArrays(GLES20.GL_LINES, 0, VertexCount);
	
	    // Disable vertex array
	    GLES20.glDisableVertexAttribArray(PositionHandle);
	}
}
*/