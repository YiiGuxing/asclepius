package vtk.extensions

import vtk.vtkDataObject
import vtk.vtkInformationDoubleVectorKey

object VTKDataObject : vtkDataObject() {

    val SPACING = SPACING() as vtkInformationDoubleVectorKey

}