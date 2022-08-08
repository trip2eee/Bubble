# Bubble
A simple Android game mimicking puzzle bobble.

![screenshot](images/screenshot.jpg)

The goal of this game is to clear all the bubbles in the screen. If identically-colored bubbles form a group of three or more bubbles, they explode and disappear. When the group of bubbles explodes, bubbles hanging from the group also fall down and are removed.


The bubble to be fired can be switched with a bomb if the bomb icon is touched. The bombs explode and clear surrounding bubbles when they collide with walls or other bubbles.

The number of remaining bombs are displayed on the right of bomb icon and a new bomb is given at clearing of every 10 bubbles.


## Development environment
- Android Studio Chipmunk | 2021.2.1 Patch 1
- Kotlin
- OpenGL ES 3.0
- Python
- Samsung Galaxy S20, Android version: 12
- Blender 2.93
    - Bubble and bomb are designed by Blender and saved in ./objects folder. They are exported to wavefront obj files. Then converted into Kotlin class by [objects/obj2floatarray.py](objects/obj2floatarray.py)





