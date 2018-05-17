package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.util.findComponent
import cn.yiiguxing.asclepius.util.forEachComponent
import cn.yiiguxing.asclepius.util.forEachComponentIndexed
import cn.yiiguxing.asclepius.widget.MaximizablePanel
import vtk.extensions.jogl.VTKImageViewer.SliceOrientation
import vtk.vtkDICOMImageReader
import java.awt.*
import java.awt.Color
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

class AsclepiusFrame : JFrame("Asclepius") {

    private val axialViewer: SliceViewer
    private val coronalViewer: SliceViewer
    private val sagittalViewer: SliceViewer
    private val volumeViewer: VolumePanel

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        layout = BorderLayout()

        val contentPanel = JPanel()
        val layoutManager = LayoutManager(contentPanel).also { contentPanel.layout = it }
        val maximizeAction = object : MaximizablePanel.MaximizeActionListener {
            override fun onMaximize(panel: MaximizablePanel) {
                layoutManager.maximize(panel)
            }

            override fun onRestore(panel: MaximizablePanel) {
                layoutManager.restore()
            }
        }

        axialViewer = createViewer("Axial", contentPanel, SliceOrientation.XY, Color.RED, maximizeAction)
        coronalViewer = createViewer("Coronal", contentPanel, SliceOrientation.XZ, Color.GREEN, maximizeAction)
        sagittalViewer = createViewer("Sagittal", contentPanel, SliceOrientation.XZ, Color.BLUE, maximizeAction)
        volumeViewer = VolumePanel().apply { maximizeActionListener = maximizeAction }

        SliceViewer.MultiSliceViewerSynchronizer().register(axialViewer, coronalViewer, sagittalViewer)

        contentPanel.add(volumeViewer)
        add(contentPanel, BorderLayout.CENTER)
    }

    fun setReader(reader: vtkDICOMImageReader) {
        val slice = reader.GetOutputPort()
        axialViewer.setInputConnection(slice)
        coronalViewer.setInputConnection(slice)
        sagittalViewer.setInputConnection(slice)
        volumeViewer.volumeViewer.imageData = reader.GetOutput()
    }

    private fun createViewer(title: String,
                             parent: Container,
                             orientation: SliceOrientation,
                             borderColor: Color,
                             maximizeAction: MaximizablePanel.MaximizeActionListener): SliceViewer {
        return SliceViewer(title).apply {
            setBorder(borderColor)
            sliceOrientation = orientation
            maximizeActionListener = maximizeAction

            parent.add(this)
        }
    }

    private class LayoutManager(private val parent: Container) : LayoutManager2 {

        private var isMaximizeMode = false

        fun maximize(comp: MaximizablePanel) {
            synchronized(parent.treeLock) {
                if (!isMaximizeMode) {
                    parent.forEachComponent {
                        it.isVisible = it === comp
                    }
                    isMaximizeMode = true
                }
            }
        }

        fun restore() {
            synchronized(parent.treeLock) {
                if (isMaximizeMode) {
                    parent.forEachComponent {
                        it.isVisible = true
                    }
                    isMaximizeMode = false
                }
            }
        }

        override fun layoutContainer(parent: Container) {
            require(this.parent === parent)
            synchronized(parent.treeLock) {
                if (isMaximizeMode) {
                    val w = parent.width
                    val h = parent.height
                    parent.findComponent { it.isVisible }!!.apply {
                        setSize(w, h)
                        setBounds(0, 0, w, h)
                    }
                } else {
                    val w = parent.width / 2
                    val h = parent.height / 2
                    parent.forEachComponentIndexed { i, comp ->
                        val cel = i % 2
                        val row = i / 2
                        comp.setSize(w, h)
                        comp.setBounds(w * cel, h * row, w, h)
                    }
                }
            }
        }

        override fun addLayoutComponent(comp: Component, constraints: Any?) {
            if (comp !is MaximizablePanel) {
                throw IllegalArgumentException("Can not add component: $comp")
            }
            if (parent.componentCount > 4) {
                throw IllegalStateException("Can not add components anymore")
            }
        }

        override fun addLayoutComponent(name: String?, comp: Component) {
            addLayoutComponent(comp, name)
        }

        override fun invalidateLayout(target: Container) = Unit
        override fun getLayoutAlignmentY(target: Container?) = 0.5f
        override fun getLayoutAlignmentX(target: Container?) = 0.5f
        override fun maximumLayoutSize(target: Container?) = Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
        override fun preferredLayoutSize(parent: Container?) = Dimension(0, 0)
        override fun minimumLayoutSize(parent: Container?) = Dimension(0, 0)
        override fun removeLayoutComponent(comp: Component?) = Unit
    }
}