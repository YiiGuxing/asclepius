package vtk.extensions

import vtk.vtkInformationIntegerVectorKey
import vtk.vtkStreamingDemandDrivenPipeline

object VTKStreamingDemandDrivenPipeline : vtkStreamingDemandDrivenPipeline() {

    val WHOLE_EXTENT = WHOLE_EXTENT() as vtkInformationIntegerVectorKey

}