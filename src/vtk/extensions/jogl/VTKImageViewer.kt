package vtk.extensions.jogl

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import vtk.*
import vtk.extensions.VTKDataObject
import vtk.extensions.VTKStreamingDemandDrivenPipeline
import vtk.extensions.internal.component6
import vtk.rendering.jogl.vtkAbstractJoglComponent
import java.awt.Component
import java.awt.Point
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
open class VTKImageViewer<T : Component>(renderWindow: vtkRenderWindow, glContainer: T)
    : vtkAbstractJoglComponent<T>(renderWindow, glContainer) {

    private val imageActor = vtkImageActor()
    private val windowLevel = vtkImageMapToWindowLevelColors()

    private var needRestScale = true

    init {
        @Suppress("LeakingThis")
        setInteractorStyle(vtkInteractorStyleImage())
        imageActor.GetMapper().SetInputConnection(windowLevel.GetOutputPort())
        renderer.AddViewProp(imageActor)
        camera.ParallelProjectionOn()

        (component as? GLAutoDrawable)?.let {
            it.addGLEventListener(object : GLEventListener {
                override fun display(glDrawable: GLAutoDrawable) {
                    restScale()
                }

                override fun reshape(glDrawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {}
                override fun init(glDrawable: GLAutoDrawable) {}
                override fun dispose(glDrawable: GLAutoDrawable) {}
            })
            it.addGLEventListener(glEventListener)
        }
    }

    var inputData: vtkImageData?
        get() = windowLevel.GetInput() as? vtkImageData
        set(value) {
            windowLevel.SetInputData(value)
            updateDisplayExtent()
        }

    val inputAlgorithm: vtkAlgorithm? get() = windowLevel.GetInputAlgorithm()

    val inputInformation: vtkInformation? get() = windowLevel.GetInputInformation()

    private var _slice = 0

    var slice: Int
        get() = _slice
        set(value) {
            if (value != _slice) {
                val valueToSet = sliceRange?.run { maxOf(first, minOf(value, last)) } ?: 0
                if (valueToSet != _slice) {
                    _slice = valueToSet

                    updateDisplayExtent()
                    Render()
                }
            }
        }

    val sliceRange: IntRange?
        get() {
            val input = inputAlgorithm ?: return null
            val info = input.apply { UpdateInformation() }.GetOutputInformation(0)
            val key = VTKStreamingDemandDrivenPipeline.WHOLE_EXTENT

            return with(info) {
                val index = sliceOrientation.value * 2
                Get(key, index)..Get(key, index + 1)
            }
        }

    val minSlice: Int? get() = sliceRange?.first

    val maxSlice: Int? get() = sliceRange?.last

    var colorWindow: Double
        set(value) = windowLevel.SetWindow(value)
        get() = windowLevel.GetWindow()

    var colorLevel: Double
        set(value) = windowLevel.SetLevel(value)
        get() = windowLevel.GetLevel()

    enum class SliceOrientation(val value: Int) {
        YZ(0),
        XZ(1),
        XY(2)
    }

    var sliceOrientation = SliceOrientation.XY
        set(value) {
            if (value == field) return

            field = value

            updateOrientation()
            updateDisplayExtent()
        }

    fun updateOrientation() {
        with(camera) {
            SetFocalPoint(0.0, 0.0, 0.0)
            when (sliceOrientation) {
                SliceOrientation.XY -> {
                    SetPosition(0.0, 0.0, 1.0)
                    SetViewUp(0.0, 1.0, 0.0)
                }
                SliceOrientation.XZ -> {
                    SetPosition(0.0, 1.0, 0.0)
                    SetViewUp(0.0, 0.0, -1.0)
                }
                SliceOrientation.YZ -> {
                    SetPosition(1.0, 0.0, 0.0)
                    SetViewUp(0.0, 0.0, -1.0)
                }
            }
        }
    }

    var position: Point
        set(value) = setPosition(value.x, value.y)
        get() = with(renderWindow.GetPosition()) {
            Point(get(0), get(1))
        }

    fun setPosition(x: Int, y: Int) = renderWindow.SetPosition(x, y)

    fun setInputConnection(input: vtkAlgorithmOutput?) {
        windowLevel.SetInputConnection(input)
        updateDisplayExtent()
        needRestScale = true
    }

    fun updateDisplayExtent() {
        val input = inputAlgorithm ?: return
        val info = input.apply { UpdateInformation() }.GetOutputInformation(0)
        val key = VTKStreamingDemandDrivenPipeline.WHOLE_EXTENT

        val wholeExtent = IntArray(6) { info.Get(key, it) }
        val sliceIndex = sliceOrientation.value * 2
        val slice = fixSlice(wholeExtent[sliceIndex], wholeExtent[sliceIndex + 1])

        when (sliceOrientation) {
            SliceOrientation.XY ->
                imageActor.SetDisplayExtent(
                        wholeExtent[0],
                        wholeExtent[1],
                        wholeExtent[2],
                        wholeExtent[3],
                        slice, slice)
            SliceOrientation.XZ ->
                imageActor.SetDisplayExtent(
                        wholeExtent[0],
                        wholeExtent[1],
                        slice, slice,
                        wholeExtent[4],
                        wholeExtent[5])
            SliceOrientation.YZ ->
                imageActor.SetDisplayExtent(
                        slice, slice,
                        wholeExtent[2],
                        wholeExtent[3],
                        wholeExtent[4],
                        wholeExtent[5])
        }

        val interactorStyle = windowInteractor.GetInteractorStyle() as? vtkInteractorStyleImage
        if (interactorStyle?.GetAutoAdjustCameraClippingRange() != 0) {
            resetCameraClippingRange()
        } else {
            val bounds = imageActor.GetBounds()
            val sPosition = bounds[sliceOrientation.value * 2]
            val cPosition = camera.GetPosition()[sliceOrientation.value]
            val range = abs(sPosition - cPosition)
            val spacingKay = VTKDataObject.SPACING
            val avgSpacing = with(info) {
                (Get(spacingKay, 0) + Get(spacingKay, 1) + Get(spacingKay, 2)) / 3.0
            }
            camera.SetClippingRange(range - avgSpacing * 3.0, range + avgSpacing * 3.0)
        }
    }

    private fun fixSlice(min: Int, max: Int): Int {
        return _slice.takeIf { it in min..max } ?: ((min + max) / 2).also { _slice = it }
    }

    private fun restScale() {
        if (needRestScale) {
            val (xMin, xMax, yMin, yMax, zMin, zMax) = imageActor.GetBounds()
            val x = xMax - xMin
            val y = yMax - yMin
            val z = zMax - zMin

            resetCamera()
            camera.SetParallelScale(maxOf(x, y, z) / 2.0)
            needRestScale = false
        }
    }

}