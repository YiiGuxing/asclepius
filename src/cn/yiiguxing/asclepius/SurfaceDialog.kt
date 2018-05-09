package cn.yiiguxing.asclepius

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

class SurfaceDialog(
        owner: Frame? = null,
        private val onCreateSurface: (SurfaceInfo) -> Unit
) : SurfaceFrame(owner) {

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
            isVisible = true
        }
    }

    private fun onOK() {
        val contourVal = contourValue.value as? Number ?: return
        val color = colorPanel.background.toColor()
        val smoothing = enableSmoothingCheckBox.isSelected
        val numberOfSmoothingIterations = noiComboBox.selectedItem as Int
        val surfaceInfo = SurfaceInfo(contourVal.toDouble(), color, smoothing, numberOfSmoothingIterations)
        onCreateSurface(surfaceInfo)
        dispose()
    }

    private fun onCancel() {
        dispose()
    }

    companion object {
        private val NUMBER_OF_SMOOTHING_ITERATIONS = arrayOf(5, 10, 20, 30, 50, 70, 100)

        inline fun show(c: Component, crossinline onCreateSurface: (SurfaceInfo) -> Unit) {
            val frame = JOptionPane.getFrameForComponent(c)
            SurfaceDialog(frame) { onCreateSurface(it) }.apply {
                pack()
                setLocationRelativeTo(frame)
                isVisible = true
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
    SurfaceDialog { println(it) }.apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
    System.exit(0)
}