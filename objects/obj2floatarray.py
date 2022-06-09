"""
This script converts Wavefront (.obj) files into float array of Kotlin.
"""

from lib2to3.pgen2 import token
import sys

from numpy import array

class Vertex3D:
    def __init__(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z

class Vertex2D:
    def __init__(self, u, v):
        self.u = u
        self.v = v

class Face:
    def __init__(self, v1, v2, v3):

        # vertex indices
        self.v = [0] * 3
        
        # vertex texture coordinate indices
        self.vt = [0] * 3
        
        # vertex normal indices
        self.vn = [0] * 3

        v1 = v1.split('/')
        v2 = v2.split('/')
        v3 = v3.split('/')

        if len(v1) >= 1:
            self.v[0] = int(v1[0]) - 1
            self.v[1] = int(v2[0]) - 1
            self.v[2] = int(v3[0]) - 1
        
        if len(v1) >= 2:
            self.vt[0] = int(v1[1]) - 1
            self.vt[1] = int(v2[1]) - 1
            self.vt[2] = int(v3[1]) - 1

        if len(v1) >= 3:
            self.vn[0] = int(v1[2]) - 1
            self.vn[1] = int(v2[2]) - 1
            self.vn[2] = int(v3[2]) - 1

if __name__ == '__main__':
    obj_file_path = 'bubble.obj'
    array_file_path = 'array.kt'
    scale = 1.0

    list_vertex = []
    list_vertex_normal = []
    list_face = []

    with open(obj_file_path, 'r') as f:
        while True:
            line = f.readline()
            if line == '':
                break

            tokens = line.split()
            # vertex
            if len(tokens) == 4 and tokens[0] == 'v':
                x = float(tokens[1]) * scale
                y = float(tokens[2]) * scale
                z = float(tokens[3]) * scale
                vertex = Vertex3D(x, y, z)
                list_vertex.append(vertex)
            
            # vertex normal
            elif len(tokens) == 4 and tokens[0] == 'vn':
                x = float(tokens[1])
                y = float(tokens[2])
                z = float(tokens[3])
                vertex = Vertex3D(x, y, z)
                list_vertex_normal.append(vertex)

            # face element
            elif len(tokens) == 4 and tokens[0] == 'f':
                v1 = tokens[1]
                v2 = tokens[2]
                v3 = tokens[3]
                face = Face(v1, v2, v3)
                list_face.append(face)


    with open(array_file_path, 'w') as f:
        f.write('val vertexCoords = floatArrayOf(\n')
        for i in range(len(list_face)):
            face = list_face[i]

            v = list_vertex[face.v[0]]
            f.write('    {:f}f, {:f}f, {:f}f, '.format(v.x, v.y, v.z))

            v = list_vertex[face.v[1]]
            f.write('{:f}f, {:f}f, {:f}f, '.format(v.x, v.y, v.z))

            v = list_vertex[face.v[2]]
            f.write('{:f}f, {:f}f, {:f}f'.format(v.x, v.y, v.z))

            if i < len(list_face)-1:            
                f.write(',')
            f.write('\n')
        f.write(')\n')

        f.write('val vertexNormal = floatArrayOf(\n')
        for i in range(len(list_face)):
            face = list_face[i]
            
            v = list_vertex_normal[face.vn[0]]
            f.write('    {:f}f, {:f}f, {:f}f, '.format(v.x, v.y, v.z))

            v = list_vertex_normal[face.vn[1]]
            f.write('{:f}f, {:f}f, {:f}f, '.format(v.x, v.y, v.z))

            v = list_vertex_normal[face.vn[2]]
            f.write('{:f}f, {:f}f, {:f}f'.format(v.x, v.y, v.z))

            if i < len(list_face)-1:            
                f.write(',')
            f.write('\n')
        f.write(')\n')



                
                
