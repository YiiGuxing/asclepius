package cn.yiiguxing.asclepius

import java.awt.Component
import java.awt.Frame
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
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

        contourValue.formatterFactory = DefaultFormatterFactory(NumberFormatter())
        noiComboBox.apply {
            model = DefaultComboBoxModel(NUMBER_OF_SMOOTHING_ITERATIONS)
            selectedIndex = 3
        }
        enableSmoothingCheckBox.addItemListener {
            enableSmoothingCheckBox.isSelected.let {
                noiLabel.isEnabled = it
                noiComboBox.isEnabled = it
            }
        }
        buttonOK.addActionListener { onOK() }
        buttonCancel.addActionListener { onCancel() }

        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                onCancel()
            }
        })

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction({ onCancel() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    }

    private fun onOK() {
        val contourVal = contourValue.value as? Number ?: return
        val smoothing = enableSmoothingCheckBox.isSelected
        val numberOfSmoothingIterations = noiComboBox.selectedItem as Int
        val surfaceInfo = SurfaceInfo(contourVal.toDouble(), smoothing, numberOfSmoothingIterations)
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

fun main(args: Array<String>) {
    SurfaceDialog { println(it) }.apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
    System.exit(0)
}