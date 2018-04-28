package vtk.extensions.jogl

import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel
import vtk.vtkGenericOpenGLRenderWindow
import vtk.vtkRenderWindow

@Suppress("unused")
class VTKJoglPanelImageViewer(renderWindow: vtkRenderWindow, capabilities: GLCapabilities)
    : VTKImageViewer<GLJPanel>(renderWindow, GLJPanel(capabilities)) {

    constructor(renderWindow: vtkRenderWindow) : this(renderWindow, GLCapabilities(GLProfile.getDefault()))

    constructor() : this(vtkGenericOpenGLRenderWindow())

}