package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.form.SliceViewerForm
import vtk.extensions.jogl.VTKJoglCanvasImageViewer
import vtk.rendering.vtkAbstractEventInterceptor
import vtk.vtkAlgorithmOutput
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.border.LineBorder

/**
 * SliceViewer
 *
 * Created by Yii.Guxing on 2018/05/11
 */
class SliceViewer(title: String) : SliceViewerForm() {

    private val viewer = VTKJoglCanvasImageViewer()

    var sliceOrientation
        get() = viewer.sliceOrientation
        set(value) {
            viewer.sliceOrientation = value
        }

    init {
        viewer.apply {
            renderer.SetUseFXAA(true)
            interactorForwarder.eventInterceptor = EventInterceptor()
        }

        titleLabel.text = title
        contentPanel.add(viewer.component, BorderLayout.CENTER)
        scrollBar.addAdjustmentListener { viewer.slice = it.value }
    }

    fun setBorder(color: AWTColor) {
        contentPanel.border = LineBorder(color)
    }

    fun setInputConnection(input: vtkAlgorithmOutput?) {
        viewer.setInputConnection(input)
        updateSlice()
    }

    private fun updateSlice() {
        val scrollBar = scrollBar
        val sliceRange = viewer.sliceRange
        if (sliceRange != null) {
            val min = sliceRange.first
            val max = sliceRange.last
            val value = (min + max) / 2
            val extent = Math.sqrt(max - min + 1.0).toInt()

            scrollBar.blockIncrement = extent
            scrollBar.setValues(value, extent, min, max + extent)
        } else {
            scrollBar.setValues(0, 0, 0, 0)
        }
    }

    private inner class EventInterceptor : vtkAbstractEventInterceptor() {
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

            scrollBar.value += increment
            return true
        }

        override fun mouseDragged(e: MouseEvent): Boolean {
            if (!adjustWWWC) {
                return super.mouseDragged(e)
            }

            with(viewer) {
                colorLevel += e.y - lastY
                colorWindow = maxOf(0.0, colorWindow + e.x - lastX)
                Render()
            }

            lastX = e.x
            lastY = e.y

            return true
        }
    }

}
