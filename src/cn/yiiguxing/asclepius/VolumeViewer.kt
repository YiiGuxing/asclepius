package cn.yiiguxing.asclepius

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.sun.java.swing.plaf.windows.WindowsMenuItemUI
import com.sun.java.swing.plaf.windows.WindowsRadioButtonMenuItemUI
import vtk.*
import vtk.extensions.VTK
import vtk.rendering.jogl.vtkJoglCanvasComponent
import vtk.rendering.vtkAbstractEventInterceptor
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JRadioButtonMenuItem
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import kotlin.math.abs

class VolumeViewer : vtkJoglCanvasComponent() {

    // Using these lines to improve the raycasting quality.
    // These values seems related to the distance from ray from raycasting.
    // TODO: Need to see values that improve the quality and don't decrease
    // the performance. 2.0 seems to be a good value to pixDiag
    private val pixDiag = 2.0

    private val colorTransferFunction = vtkColorTransferFunction()
    private val opacityTransferFunction = vtkPiecewiseFunction()

    private val volumeProperty = vtkVolumeProperty().apply {
        SetInterpolationTypeToLinear()
        SetColor(colorTransferFunction)
        SetScalarOpacity(opacityTransferFunction)
        SetScalarOpacityUnitDistance(pixDiag)
    }

    private val volumeMapper = vtkSmartVolumeMapper().apply {
        SetRequestedRenderModeToGPU()
        SetSampleDistance(pixDiag / 5.0)
    }

    private val shiftScale = vtkImageShiftScale().apply { SetOutputScalarTypeToUnsignedShort() }

    private val volume = vtkVolume().apply {
        SetMapper(volumeMapper)
        SetProperty(volumeProperty)
        VisibilityOff()
    }

    private val surfaceActors = mutableListOf<vtkActor>()

    private var isDisplay = false
    private var isFirst = true
    private var lastSelectedItem: JRadioButtonMenuItem? = null

    init {
        setBackgroundColor(Color.BLACK)
        renderer.apply {
            SetUseFXAA(true)
            AddVolume(volume)
        }
        renderWindow.apply {
            PointSmoothingOn()
            LineSmoothingOn()
        }
        camera.apply {
            SetFocalPoint(0.0, 0.0, 0.0)
            SetPosition(0.0, 1.0, 0.0)
            SetViewUp(0.0, 0.0, -1.0)
        }

        setupPopupMenu()

        component.addGLEventListener(object : GLEventListener {
            override fun display(glDrawable: GLAutoDrawable) {
                isDisplay = true
            }

            override fun reshape(glDrawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {}
            override fun init(glDrawable: GLAutoDrawable) {}
            override fun dispose(glDrawable: GLAutoDrawable) {}
        })
    }

    private fun setupPopupMenu() {
        val popupMenu = JPopupMenu()
        popupMenu.add(JMenuItem("Add Surface").apply {
            setUI(WindowsMenuItemUI())
            addActionListener { pickSurfaceContour() }
        })
        popupMenu.add(JMenuItem("Clear Surfaces").apply {
            setUI(WindowsMenuItemUI())
            addActionListener { clearSurfaces() }
        })
        popupMenu.addSeparator()

        popupMenu.add(JRadioButtonMenuItem("OFF", true).apply {
            lastSelectedItem = this
            setUI(WindowsRadioButtonMenuItemUI())

            fun updateAction() {
                currentPreset = null
            }

            addActionListener { doAction(::updateAction) }
        })
        Presets.rayCastingPresets.forEach { preset ->
            popupMenu.add(JRadioButtonMenuItem(preset.name).apply {
                setUI(WindowsRadioButtonMenuItemUI())

                fun updateAction() {
                    currentPreset = preset
                }

                addActionListener { doAction(::updateAction) }
            })
        }

        popupMenu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) = Unit
            override fun popupMenuCanceled(e: PopupMenuEvent) = Unit
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                val isEnabled = imageData != null
                for (i in 0 until popupMenu.componentCount) {
                    popupMenu.getComponent(i).isEnabled = isEnabled
                }
            }
        })

        interactorForwarder.eventInterceptor = object : vtkAbstractEventInterceptor() {
            override fun mouseClicked(e: MouseEvent): Boolean {
                if (e.button == MouseEvent.BUTTON3) {
                    popupMenu.show(e.component, e.x, e.y)
                    return true
                }

                return super.mouseClicked(e)
            }
        }
    }

    private fun JRadioButtonMenuItem.doAction(action: () -> Unit) {
        if (this != lastSelectedItem) {
            lastSelectedItem?.isSelected = false
            lastSelectedItem = this
            action()
        }
    }

    var imageData: vtkImageData? = null
        set(value) {
            if (value != field) {
                field = value

                removeSurfaces()
                shiftScale.SetInputData(value)
                if (value != null) {
                    val scale = value.GetScalarRange()
                    shiftScale.SetShift(abs(scale[0]))
                }

                updatePreset()
                refresh()
                VTK.gc()
            }
        }

    var currentPreset: RayCastingPreset? = null
        set(value) {
            if (value != field) {
                field = value
                updatePreset()
                refresh()
                VTK.gc()
            }
        }

    private fun updatePreset() {
        val imageData = imageData ?: return
        val scale = imageData.GetScalarRange()

        val preset = currentPreset
        if (preset == null) {
            setBackgroundColor(Color.BLACK)
            volume.VisibilityOff()
            return
        }

        setBackgroundColor(preset.backgroundColor)

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
        volume.VisibilityOn()
    }

    private fun setBackgroundColor(color: Color) {
        component.background = color.toAWTColor()
        renderer.SetBackground(color.red, color.green, color.blue)
    }

    private fun pickSurfaceContour() {
        SurfaceContourPicker.show(component) {
            val color = if (surfaceActors.isNotEmpty()) {
                val (hue, saturation, brightness) = with(Color.random.toAWTColor()) {
                    AWTColor.RGBtoHSB(red, green, blue, null)
                }
                AWTColor.getHSBColor(hue, saturation, maxOf(brightness, 0.65f)).toColor()
            } else {
                Color.WHITE
            }

            createAndAddSurface(it, color = color)
        }
    }

    private fun createAndAddSurface(vararg contourValues: Double, color: Color = Color.WHITE) {
        if (contourValues.isEmpty()) {
            return
        }

        val contourFilter = vtkContourFilter().apply {
            SetInputData(imageData)
            contourValues.forEachIndexed { index, v -> SetValue(index, v) }
        }
        val normals = vtkPolyDataNormals().apply {
            SetInputConnection(contourFilter.GetOutputPort())
            SetFeatureAngle(60.0)
        }
        val mapper = vtkPolyDataMapper().apply {
            SetInputConnection(normals.GetOutputPort())
            ScalarVisibilityOff()
        }
        val actor = vtkActor().apply {
            SetMapper(mapper)
            GetProperty().SetColor(color.red, color.green, color.blue)
        }
        renderer.AddActor(actor)
        surfaceActors += actor

        refresh()
        VTK.gc()
    }

    private fun removeSurfaces() {
        with(surfaceActors) {
            forEach { renderer.RemoveActor(it) }
            clear()
        }
    }

    private fun clearSurfaces() {
        removeSurfaces()
        refresh()
        VTK.gc()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun refresh() {
        if (!isDisplay) {
            return
        }

        if (isFirst) {
            isFirst = false
            resetCamera()
        }
        Render()
    }

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        private inline fun Double.translateScale(scale: DoubleArray) = this - scale[0]
    }

}