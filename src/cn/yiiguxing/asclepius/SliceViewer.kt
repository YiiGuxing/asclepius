package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.form.SliceViewerForm
import cn.yiiguxing.asclepius.widget.MaximizablePanel
import vtk.extensions.jogl.VTKJoglCanvasImageViewer
import vtk.rendering.vtkAbstractEventInterceptor
import vtk.vtkAlgorithmOutput
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.ButtonGroup
import javax.swing.JMenu
import javax.swing.JPopupMenu
import javax.swing.JRadioButtonMenuItem
import javax.swing.border.LineBorder
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * SliceViewer
 *
 * Created by Yii.Guxing on 2018/05/11
 */
class SliceViewer(title: String) : SliceViewerForm(), MaximizablePanel {

    private val viewer = VTKJoglCanvasImageViewer()
    private val popupMenu = createPopupMenu()

    var sliceOrientation
        get() = viewer.sliceOrientation
        set(value) {
            viewer.sliceOrientation = value
        }

    override var maximizeActionListener: MaximizablePanel.MaximizeActionListener? = null
    private var _isMaximize = false
    override val isMaximize: Boolean
        get() = _isMaximize

    init {
        viewer.apply {
            renderer.SetUseFXAA(true)
            interactorForwarder.eventInterceptor = EventInterceptor()
        }

        titleLabel.text = title
        contentPanel.add(viewer.component, BorderLayout.CENTER)
        scrollBar.addAdjustmentListener { viewer.slice = it.value }

        initMaximizeButton()
    }

    private fun initMaximizeButton() {
        with(maximizeButton) {
            hoveringColor = Color(0xE6E6E6)
            actionListener = { fireToggleMaximize() }
        }
    }

    override fun onMaximize(): Boolean {
        _isMaximize = true
        maximizeButton.icon = Icons.RESTORE
        return true
    }

    override fun onRestore(): Boolean {
        _isMaximize = false
        maximizeButton.icon = Icons.MAXIMIZE
        return true
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

    private fun createPopupMenu(): JPopupMenu = JPopupMenu().apply {
        val group = ButtonGroup()
        val pseudoColorMenu = JMenu("Pseudo Color").apply {
            add(JRadioButtonMenuItem("OFF", true).apply {
                group.add(this)
                addActionListener { viewer.lookupTable = null }
            })
            addSeparator()

            val lookUpTables = Presets.defaultLookUpTables.map { it.name to it.vtkLookupTable } +
                    Presets.colorLookUpTables.map { it.name to it.vtkLookupTable }
            lookUpTables.sortedBy { it.first }.forEach { (name, lookupTable) ->
                add(JRadioButtonMenuItem(name).apply {
                    group.add(this)
                    addActionListener { viewer.lookupTable = lookupTable }
                })
            }
        }

        add(pseudoColorMenu)
        addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) = Unit
            override fun popupMenuCanceled(e: PopupMenuEvent) = Unit
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                val isEnabled = viewer.sliceRange != null
                for (i in 0 until componentCount) {
                    getComponent(i).isEnabled = isEnabled
                }
            }
        })
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

        override fun mouseClicked(e: MouseEvent): Boolean {
            return if (e.button == MouseEvent.BUTTON3) {
                popupMenu.show(e.component, e.x, e.y)
                true
            } else {
                super.mousePressed(e)
            }
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
