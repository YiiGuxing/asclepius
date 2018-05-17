package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.dialog.WindowingDialog
import cn.yiiguxing.asclepius.form.SliceViewerForm
import cn.yiiguxing.asclepius.widget.MaximizablePanel
import vtk.extensions.jogl.VTKJoglCanvasImageViewer
import vtk.rendering.vtkAbstractEventInterceptor
import vtk.vtkAlgorithmOutput
import vtk.vtkCornerAnnotation
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

    private val annotation: vtkCornerAnnotation = vtkCornerAnnotation()

    private var sliceCount: Int = 0

    var sliceOrientation
        get() = viewer.sliceOrientation
        set(value) {
            viewer.sliceOrientation = value
        }

    var synchronizer: Synchronizer = DefaultSynchronizer()

    override var maximizeActionListener: MaximizablePanel.MaximizeActionListener? = null
    private var _isMaximize = false
    override val isMaximize: Boolean
        get() = _isMaximize

    init {
        annotation.apply {
            SetText(1, "WL: ${viewer.colorLevel}\nWW: ${viewer.colorWindow}")

            SetMaximumFontSize(15)
            SetLinearFontScaleFactor(2.0)
            SetNonlinearFontScaleFactor(1.0)
            GetTextProperty().ShadowOn()
        }
        viewer.apply {
            renderer.SetUseFXAA(true)
            renderer.AddViewProp(annotation)
            interactorForwarder.eventInterceptor = EventInterceptor()
        }

        titleLabel.text = title
        contentPanel.add(viewer.component, BorderLayout.CENTER)
        scrollBar.addAdjustmentListener {
            viewer.slice = it.value
            updateSliceAnnotation()
        }

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
            sliceCount = sliceRange.last + 1
            val min = sliceRange.first
            val max = sliceRange.last
            val value = (min + max) / 2
            val extent = Math.sqrt(max - min + 1.0).toInt()

            scrollBar.blockIncrement = extent
            scrollBar.setValues(value, extent, min, max + extent)
        } else {
            viewer.slice
            sliceCount = 0
            scrollBar.setValues(0, 0, 0, 0)
        }
        updateSliceAnnotation()
    }

    private fun updateSliceAnnotation() {
        if (sliceCount > 0) {
            annotation.SetText(0, "${viewer.slice + 1}/$sliceCount")
        } else {
            annotation.SetText(0, null)
        }
    }

    private fun setWindowLevel(windowWidth: Double, windowLevel: Double) {
        annotation.SetText(1, "WL: $windowLevel\nWW: $windowWidth")
        with(viewer) {
            colorLevel = windowLevel
            colorWindow = if (isInverse) -windowWidth else windowWidth
            Render()
        }
    }

    private fun showWindowingDialog() {
        WindowingDialog.show(this, viewer.colorWindow, viewer.colorLevel)
                ?.let { (windowWidth, windowLevel) -> synchronizer.setWindowLevel(windowWidth, windowLevel) }
    }

    private fun createPopupMenu(): JPopupMenu = JPopupMenu().apply {
        val windowLevelMenu = JMenu("Window/Level").apply {
            add(JMenuItem("Custom").apply {
                addActionListener { showWindowingDialog() }
            })
            addSeparator()

            Presets.windowLevel.forEach { name, (windowWidth, windowLevel) ->
                add(JMenuItem(name).apply {
                    addActionListener { synchronizer.setWindowLevel(windowWidth, windowLevel) }
                })
            }
        }

        val itemMap = HashMap<String?, JRadioButtonMenuItem>()
        val pseudoColorGroup = ButtonGroup()
        val pseudoColorMenu = JMenu("Pseudo Color").apply {
            add(JRadioButtonMenuItem("OFF", true).apply {
                itemMap[null] = this
                pseudoColorGroup.add(this)
                addActionListener { synchronizer.setLookupTable(null) }
            })
            addSeparator()

            Presets.lookUpTablePresets.keys.forEach { name ->
                add(JRadioButtonMenuItem(name).apply {
                    itemMap[name] = this
                    pseudoColorGroup.add(this)
                    addActionListener { synchronizer.setLookupTable(name) }
                })
            }
        }

        val inverseItem = JCheckBoxMenuItem("Inverse").apply {
            addActionListener { synchronizer.setInverse(isSelected) }
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
        private var isButton1Down = false

        override fun mousePressed(e: MouseEvent): Boolean {
            lastX = e.x
            lastY = e.y
            isButton1Down = e.button == MouseEvent.BUTTON1
            return super.mousePressed(e)
        }

        override fun mouseReleased(e: MouseEvent?): Boolean {
            isButton1Down = false
            return super.mouseReleased(e)
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
            if (!isButton1Down || e.isShiftDown || e.isControlDown) {
                return super.mouseDragged(e)
            }

            val wl = viewer.colorLevel + e.y - lastY
            val ww = maxOf(1.0, abs(viewer.colorWindow) + e.x - lastX)
            synchronizer.setWindowLevel(ww, wl)

            lastX = e.x
            lastY = e.y

            return true
        }
    }

    inner class DefaultSynchronizer : Synchronizer {
        override fun setWindowLevel(colorWindow: Double, colorLevel: Double) {
            this@SliceViewer.setWindowLevel(colorWindow, colorLevel)
        }

        override fun setInverse(inverse: Boolean) {
            isInverse = inverse
        }

        override fun setLookupTable(name: String?) {
            lookupTablePreset = name
        }
    }

    interface Synchronizer {

        fun setWindowLevel(colorWindow: Double, colorLevel: Double)

        fun setInverse(inverse: Boolean)

        fun setLookupTable(name: String?)

    }

    class MultiSliceViewerSynchronizer : Synchronizer {

        private val viewers = ArrayList<SliceViewer>()

        fun register(vararg viewers: SliceViewer) {
            this.viewers.addAll(viewers)
            for (viewer in viewers) {
                viewer.synchronizer = this
            }
        }

        @Suppress("unused")
        fun unregister(viewer: SliceViewer) {
            if (viewers.remove(viewer)) {
                viewer.synchronizer = viewer.DefaultSynchronizer()
            }
        }

        override fun setWindowLevel(colorWindow: Double, colorLevel: Double) {
            for (viewer in viewers) {
                viewer.setWindowLevel(colorWindow, colorLevel)
            }
        }

        override fun setInverse(inverse: Boolean) {
            for (viewer in viewers) {
                viewer.isInverse = inverse
            }
        }

        override fun setLookupTable(name: String?) {
            for (viewer in viewers) {
                viewer.lookupTablePreset = name
            }
        }
    }
}
