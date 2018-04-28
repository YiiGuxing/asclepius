package vtk.extensions.jogl

import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import vtk.vtkGenericOpenGLRenderWindow
import vtk.vtkRenderWindow

@Suppress("unused")
class VTKJoglCanvasImageViewer(renderWindow: vtkRenderWindow, capabilities: GLCapabilities)
    : VTKImageViewer<GLCanvas>(renderWindow, GLCanvas(capabilities)) {

    constructor(renderWindow: vtkRenderWindow) : this(renderWindow, GLCapabilities(GLProfile.getDefault()))

    constructor() : this(vtkGenericOpenGLRenderWindow())

}