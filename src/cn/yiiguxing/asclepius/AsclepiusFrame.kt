package cn.yiiguxing.asclepius

import com.sun.java.swing.plaf.windows.WindowsMenuItemUI
import vtk.*
import vtk.extensions.VTK
import vtk.extensions.jogl.VTKImageViewer
import vtk.extensions.jogl.VTKJoglCanvasImageViewer
import vtk.rendering.jogl.vtkJoglCanvasComponent
import vtk.rendering.vtkAbstractComponent
import vtk.rendering.vtkAbstractEventInterceptor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.io.File
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JScrollBar
import kotlin.math.abs

class AsclepiusFrame(dcmDir: File) : MainFrame() {

    init {
        val reader = vtkDICOMImageReader().apply {
            SetDirectoryName(dcmDir.absolutePath)
            Update()
        }

        val axialViewer = createViewer(reader.GetOutputPort(), VTKImageViewer.SliceOrientation.XY, axialScrollBar)
        val coronalViewer = createViewer(reader.GetOutputPort(), VTKImageViewer.SliceOrientation.XZ, coronalScrollBar)
        val sagittalViewer = createViewer(reader.GetOutputPort(), VTKImageViewer.SliceOrientation.YZ, sagittalScrollBar)
        val volumeViewer = createVolumeViewer(reader.GetOutput())

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
            interactorForwarder.eventInterceptor = object : vtkAbstractEventInterceptor() {
                override fun mouseWheelMoved(e: MouseWheelEvent): Boolean {
                    val increment = when {
                        e.wheelRotation > 0 -> 1
                        e.wheelRotation < 0 -> -1
                        else -> return true
                    }

                    scrollBar.value = scrollBar.value + increment

                    return true
                }
            }
        }
    }

    private fun createVolumeViewer(imageData: vtkImageData): vtkJoglCanvasComponent {
        val scale = imageData.GetScalarRange()
        val shiftScale = vtkImageShiftScale().apply {
            SetInputData(imageData)
            SetShift(abs(scale[0]))
            SetOutputScalarTypeToUnsignedShort()
        }

        val colorTransferFunction = vtkColorTransferFunction()
        val opacityTransferFunction = vtkPiecewiseFunction()

        val volumeProperty = vtkVolumeProperty().apply {
            SetColor(colorTransferFunction)
            SetScalarOpacity(opacityTransferFunction)
            SetInterpolationTypeToLinear()
        }

        val volumeMapper = vtkSmartVolumeMapper().apply {
            SetRequestedRenderModeToGPU()
        }

        // Using these lines to improve the raycasting quality.
        // These values seems related to the distance from ray from raycasting.
        // TODO: Need to see values that improve the quality and don't decrease
        // the performance. 2.0 seems to be a good value to pixDiag
        val pixDiag = 2.0
        volumeMapper.SetSampleDistance(pixDiag / 5.0)
        volumeProperty.SetScalarOpacityUnitDistance(pixDiag)

        val volume = vtkVolume().apply {
            SetMapper(volumeMapper)
            SetProperty(volumeProperty)
        }

        val viewer = vtkJoglCanvasComponent().apply {
            component.background = Color.black
            renderer.apply {
                SetUseFXAA(true)
                AddVolume(volume)
            }
            renderWindow.apply {
                PointSmoothingOn()
                LineSmoothingOn()
            }
            vtkAbstractComponent.attachOrientationAxes(this)
            resetCamera()
        }

        fun setPreset(preset: RayCastingPreset) {
            val (r, g, b) = preset.backgroundColor
            viewer.renderer.SetBackground(r, g, b)

            colorTransferFunction.RemoveAllPoints()
            opacityTransferFunction.apply {
                RemoveAllPoints()
                AddSegment(0.0, 0.0, ((1 shl 16) - 1).toDouble(), 0.0)
            }

            if (preset.advancedCLUT) {
                val colors = preset.colors
                val curves = preset.curves
                curves.forEachIndexed { i, list ->
                    list.forEachIndexed { j, point ->
                        val grayLevel = point.x
                        val scaledGrayLevel = grayLevel.translateScale(scale)
                        val opacity = point.y
                        val color = colors[i][j]

                        colorTransferFunction.AddRGBPoint(scaledGrayLevel, color.red, color.green, color.blue)
                        opacityTransferFunction.AddPoint(scaledGrayLevel, opacity)
                    }
                }
            } else {
                val colors = preset.colorLookUpTable
                val ww = preset.windowWidth
                val wl = preset.windowLevel.translateScale(scale)
                val init = wl - ww / 2.0
                val inc = ww / (colors.size - 1.0)

                colors.forEachIndexed { index, (r, g, b) ->
                    colorTransferFunction.AddRGBPoint(init + index * inc, r, g, b)
                }
                opacityTransferFunction.apply {
                    AddPoint(wl - ww / 2.0, 0.0)
                    AddPoint(wl + ww / 2.0, 1.0)
                }
            }

            if (preset.mip) {
                volumeMapper.SetBlendModeToMaximumIntensity()
            } else {
                volumeMapper.SetBlendModeToComposite()
            }

            volumeProperty.apply {
                if (preset.useShading) {
                    ShadeOn()
                } else {
                    ShadeOff()
                }

                preset.shading.let {
                    SetAmbient(it.ambient)
                    SetDiffuse(it.diffuse)
                    SetSpecular(it.specular)
                    SetSpecularPower(it.specularPower)
                }
            }

            // Apply convolve.
            var lastImageAlgorithm: vtkImageAlgorithm = shiftScale
            for (filter in preset.convolutionFilters) {
                lastImageAlgorithm = vtkImageConvolve().apply {
                    SetInputConnection(lastImageAlgorithm.GetOutputPort())
                    SetKernel5x5(DoubleArray(filter.data.size) { filter.data[it] / 60.0 })
                }
            }
            volumeMapper.SetInputConnection(lastImageAlgorithm.GetOutputPort())
            VTK.gc()

            viewer.apply {
                resetCamera()
                Render()
            }
        }

        val popupMenu = JPopupMenu()
        Presets.rayCastingPresets.forEach { preset ->
            popupMenu.add(JMenuItem(preset.name).apply {
                setUI(WindowsMenuItemUI())
                addActionListener {
                    setPreset(preset)
                }
            })
        }

        viewer.interactorForwarder.eventInterceptor = object : vtkAbstractEventInterceptor() {
            override fun mouseClicked(e: MouseEvent): Boolean {
                if (e.button == MouseEvent.BUTTON3) {
                    popupMenu.show(e.component, e.x, e.y)
                    return true
                }

                return super.mouseClicked(e)
            }
        }

        return viewer
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Double.translateScale(scale: DoubleArray) = this - scale[0]
}