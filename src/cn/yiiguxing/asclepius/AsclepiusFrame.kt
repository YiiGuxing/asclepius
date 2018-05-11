package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.form.MainFrame
import vtk.extensions.jogl.VTKImageViewer
import vtk.vtkDICOMImageReader
import java.io.File

class AsclepiusFrame(dcmDir: File) : MainFrame() {

    init {
        val reader = vtkDICOMImageReader().apply {
            SetDirectoryName(dcmDir.absolutePath)
            SetDataByteOrderToLittleEndian()
            Update()
        }

        val slice = reader.GetOutputPort()
        axialViewer.apply {
            sliceOrientation = VTKImageViewer.SliceOrientation.XY
            setInputConnection(slice)
        }
        coronalViewer.apply {
            sliceOrientation = VTKImageViewer.SliceOrientation.XZ
            setInputConnection(slice)
        }
        sagittalViewer.apply {
            sliceOrientation = VTKImageViewer.SliceOrientation.YZ
            setInputConnection(slice)
        }

        volumeViewer.volumeViewer.imageData = reader.GetOutput()
    }
}