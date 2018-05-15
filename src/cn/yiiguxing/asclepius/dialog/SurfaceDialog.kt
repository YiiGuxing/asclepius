package cn.yiiguxing.asclepius.dialog

import cn.yiiguxing.asclepius.AWTColor
import cn.yiiguxing.asclepius.SurfaceInfo
import cn.yiiguxing.asclepius.form.SurfaceFrame
import cn.yiiguxing.asclepius.toColor
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import java.awt.Component
import java.awt.Dimension
import java.awt.Frame
import java.awt.Graphics
import java.awt.event.*
import javax.swing.*
import javax.swing.colorchooser.ColorChooserComponentFactory
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

class SurfaceDialog private constructor(owner: Frame? = null) : SurfaceFrame(owner) {

    var surfaceInfo: SurfaceInfo? = null
        private set

    init {
        title = "Create Surface"
        isModal = true
        isResizable = false
        setContentPane(contentPane)
        getRootPane().defaultButton = buttonOK
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

        contourValue.formatterFactory = DefaultFormatterFactory(NumberFormatter())
        noiComboBox.apply {
            model = DefaultComboBoxModel(NUMBER_OF_SMOOTHING_ITERATIONS)
            selectedIndex = 3
        }

        initActions()
    }

    private fun initActions() {
        enableSmoothingCheckBox.addItemListener {
            enableSmoothingCheckBox.isSelected.let {
                noiLabel.isEnabled = it
                noiComboBox.isEnabled = it
            }
        }
        colorPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    onPickColor()
                }
            }
        })

        buttonOK.addActionListener { onOK() }
        buttonCancel.addActionListener { onCancel() }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                onCancel()
            }
        })

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction({ onCancel() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    }

    private fun onPickColor() {
        val chooser = ColorChooser(colorPanel.background)
        val ok: (ActionEvent) -> Unit = { colorPanel.background = chooser.color }
        JColorChooser.createDialog(this, "Color", true, chooser, ok, null).apply {
            addComponentListener(object : ComponentAdapter() {
                override fun componentHidden(e: ComponentEvent) {
                    dispose()
                }
            })
            isResizable = false
            isVisible = true
        }
    }

    private fun onOK() {
        val contourVal = contourValue.value as? Number ?: return
        val color = colorPanel.background.toColor()
        val smoothing = enableSmoothingCheckBox.isSelected
        val numberOfSmoothingIterations = noiComboBox.selectedItem as Int
        surfaceInfo = SurfaceInfo(contourVal.toDouble(), color, smoothing, numberOfSmoothingIterations)
        dispose()
    }

    private fun onCancel() {
        surfaceInfo = null
        dispose()
    }

    companion object {
        private val NUMBER_OF_SMOOTHING_ITERATIONS = arrayOf(5, 10, 20, 30, 50, 70, 100)

        fun show(component: Component? = null): SurfaceInfo? {
            val frame = component?.let { JOptionPane.getFrameForComponent(it) }
            return with(SurfaceDialog(frame)) {
                pack()
                setLocationRelativeTo(frame)
                isVisible = true

                surfaceInfo
            }
        }
    }
}

private class ColorChooser(private val initialColor: AWTColor = AWTColor.white) : JColorChooser(initialColor) {

    init {
        chooserPanels = ColorChooserComponentFactory
                .getDefaultChooserPanels()
                .take(1)
                .toTypedArray()
        previewPanel = PreviewPanel()
    }

    private inner class PreviewPanel : JPanel() {
        override fun getPreferredSize() = Dimension(200, 50)

        override fun paintComponent(g: Graphics) {
            val hafW = width / 2
            g.color = initialColor
            g.fillRect(0, 0, hafW, height)
            g.color = color
            g.fillRect(hafW, 0, hafW, height)
        }
    }
}

fun main(args: Array<String>) {
    UIManager.setLookAndFeel(WindowsLookAndFeel())
    println(SurfaceDialog.show())
    System.exit(0)
}