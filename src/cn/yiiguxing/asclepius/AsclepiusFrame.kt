package cn.yiiguxing.asclepius

import vtk.extensions.jogl.VTKImageViewer
import vtk.extensions.jogl.VTKJoglCanvasImageViewer
import vtk.rendering.vtkAbstractEventInterceptor
import vtk.vtkAlgorithmOutput
import vtk.vtkDICOMImageReader
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.io.File
import javax.swing.JScrollBar

class AsclepiusFrame(dcmDir: File) : MainFrame() {

    init {
        val reader = vtkDICOMImageReader().apply {
            SetDirectoryName(dcmDir.absolutePath)
            SetDataByteOrderToLittleEndian()
            Update()
        }

        val axialViewer = createViewer(reader.GetOutputPort(), VTKImageViewer.SliceOrientation.XY, axialScrollBar)
        val coronalViewer = createViewer(reader.GetOutputPort(), VTKImageViewer.SliceOrientation.XZ, coronalScrollBar)
        val sagittalViewer = createViewer(reader.GetOutputPort(), VTKImageViewer.SliceOrientation.YZ, sagittalScrollBar)
        val volumeViewer = VolumeViewer().apply { imageData = reader.GetOutput() }

        axial.add(axialViewer.component, BorderLayout.CENTER)
        coronal.add(coronalViewer.component, BorderLayout.CENTER)
        sagittal.add(sagittalViewer.component, BorderLayout.CENTER)
        volume.add(volumeViewer.component, BorderLayout.CENTER)
    }

    private fun createViewer(output: vtkAlgorithmOutput,
                             sliceOrientation: VTKImageViewer.SliceOrientation,
                             scrollBar: JScrollBar): VTKImageViewer<*> {
        return VTKJoglCanvasImageViewer().apply {
            this.sliceOrientation = sliceOrientation
            setInputConnection(output)

            val sliceRange = sliceRange
            if (sliceRange != null) {
                val min = sliceRange.first
                val max = sliceRange.last
                val value = (min + max) / 2
                val extent = Math.sqrt(max - min + 1.0).toInt()

                slice = value

                scrollBar.blockIncrement = extent
                scrollBar.setValues(value, extent, min, max + extent)
                scrollBar.addAdjustmentListener { slice = it.value }
            } else {
                scrollBar.setValues(0, 0, 0, 0)
            }

            renderer.SetUseFXAA(true)
            setupEventInterceptor(scrollBar)
        }
    }

    private fun VTKJoglCanvasImageViewer.setupEventInterceptor(scrollBar: JScrollBar) {
        interactorForwarder.eventInterceptor = object : vtkAbstractEventInterceptor() {
            private var lastX: Int = 0
            private var lastY: Int = 0
            private var adjustWWWC = false

            override fun mousePressed(e: MouseEvent): Boolean {
                lastX = e.x
                lastY = e.y
                adjustWWWC = e.button == MouseEvent.BUTTON1
                return super.mousePressed(e)
            }

            override fun mouseWheelMoved(e: MouseWheelEvent): Boolean {
                val increment = when {
                    e.wheelRotation > 0 -> 1
                    e.wheelRotation < 0 -> -1
                    else -> return true
                }

                scrollBar.value = scrollBar.value + increment
                return true
            }

            override fun mouseDragged(e: MouseEvent): Boolean {
                if (!adjustWWWC) {
                    return super.mouseDragged(e)
                }

                colorWindow += e.x - lastX
                colorLevel += e.y - lastY
                lastX = e.x
                lastY = e.y

                Render()
                return true
            }
        }
    }
}