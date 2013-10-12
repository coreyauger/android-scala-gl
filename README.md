android-scala-gl
================

Android OpenGL ES 2 / draw utility library written in scala

## Overview

This project serves to wrap up some common functionality regarding drawing to the screen on the Android platform. 
I am also fairly new to the scala language and have been very impressed with what I can do with it so far.  I hope 
to continue to add more usefull utilities as I encounter a need for them.  

## Features

I will try to keep adding to this list as I go.  For now here is a short list of features:

* Simple OpenGL drawable class hierarchy
  * Includes Triangle, Square, Line, Sprite class
* Simple way to extend functionality via shaders
* DRY (Do not repeat yourself)


## Example Usage

```scala
val texInfo = Drawable.loadGLTexture(imgPath)
        
val verts = Array(-0.5f*w,  0.5f*h, 0.0f,   // top left
                -0.5f*w, -0.5f*h, 0.0f,   // bottom left
                 0.5f*w,  0.5f*h, 0.0f,	  // top right
                 -0.5f*w, -0.5f*h, 0.0f,  // bottom left	
                 0.5f*w, -0.5f*h, 0.0f,   // bottom right
                 0.5f*w,  0.5f*h, 0.0f )  // top right
val color =  Array(1.0f, 1.0f, 1.0f, 1.0f)      
val tex = Array(     0.0f, 0.0f,  // top left
    						  1.0f, 0.0f,  // bottom left
    						  0.0f, 1.0f,  // top right
    						  1.0f, 0.0f,  // bottom left
                  1.0f, 1.0f,  // bottom right
                  0.0f, 1.0f) // top right
                  
val sprite = new Sprite( verts, color, tex, texInfo.textureId)
```
