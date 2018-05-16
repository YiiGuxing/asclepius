package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.dialog.WindowingDialog
import cn.yiiguxing.asclepius.form.SliceViewerForm
import cn.yiiguxing.asclepius.widget.MaximizablePanel
import vtk.extensions.jogl.VTKJoglCanvasImageViewer
import vtk.rendering.vtkAbstractEventInterceptor
import vtk.vtkAlgorithmOutput
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import kotlin.math.abs

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

    private var lookupTablePreset: String? = null
        set(value) {
            if (value != field) {
                field = value
                viewer.lookupTable = value?.let { Presets.getLookUpTablePresets(it) }
            }
        }

    private var isInverse = false
        set(value) {
            if (value != field) {
                field = value
                with(viewer) {
                    colorWindow = -colorWindow
                    Render()
                }
            }
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

    private fun setWindowLevel(windowWidth: Double, windowLevel: Double) {
        with(viewer) {
            colorLevel = windowLevel
            colorWindow = if (isInverse) -windowWidth else windowWidth
            Render()
        }
    }

    private fun showWindowingDialog() {
        WindowingDialog.show(this, viewer.colorWindow, viewer.colorLevel)
                ?.let { (windowWidth, windowLevel) -> setWindowLevel(windowWidth, windowLevel) }
    }

    private fun createPopupMenu(): JPopupMenu = JPopupMenu().apply {
        val windowLevelMenu = JMenu("Window/Level").apply {
            add(JMenuItem("Custom").apply {
                addActionListener { showWindowingDialog() }
            })
            addSeparator()

            Presets.windowLevel.forEach { name, (windowWidth, windowLevel) ->
                add(JMenuItem(name).apply {
                    addActionListener { setWindowLevel(windowWidth, windowLevel) }
                })
            }
        }

        val itemMap = HashMap<String?, JRadioButtonMenuItem>()
        val pseudoColorGroup = ButtonGroup()
        val pseudoColorMenu = JMenu("Pseudo Color").apply {
            add(JRadioButtonMenuItem("OFF", true).apply {
                itemMap[null] = this
                pseudoColorGroup.add(this)
                addActionListener { lookupTablePreset = null }
            })
            addSeparator()

            Presets.lookUpTablePresets.keys.forEach { name ->
                add(JRadioButtonMenuItem(name).apply {
                    itemMap[name] = this
                    pseudoColorGroup.add(this)
                    addActionListener { lookupTablePreset = name }
                })
            }
        }

        val inverseItem = JCheckBoxMenuItem("Inverse").apply {
            addActionListener { isInverse = isSelected }
        }

        add(windowLevelMenu)
        add(pseudoColorMenu)
        addSeparator()
        add(inverseItem)

        addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) = Unit
            override fun popupMenuCanceled(e: PopupMenuEvent) = Unit
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                inverseItem.isSelected = isInverse
                itemMap[lookupTablePreset]?.model?.let { pseudoColorGroup.setSelected(it, true) }

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
                colorWindow = maxOf(1.0, abs(colorWindow) + e.x - lastX) * if (isInverse) -1 else 1
                Render()
            }

            lastX = e.x
            lastY = e.y

            return true
        }
    }

}
