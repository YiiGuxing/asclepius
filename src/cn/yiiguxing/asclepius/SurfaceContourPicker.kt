package cn.yiiguxing.asclepius

import java.awt.Component
import java.awt.Frame
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.KeyStroke
import javax.swing.WindowConstants
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

class SurfaceContourPicker(
        owner: Frame? = null,
        private val onPick: (Double) -> Unit
) : SurfaceContourPickerFrame(owner) {

    init {
        title = "Contour Value"
        isModal = true
        setContentPane(contentPane)
        getRootPane().defaultButton = buttonOK

        textField.formatterFactory = DefaultFormatterFactory(NumberFormatter())
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
        (textField.value as? Number)?.let {
            onPick(it.toDouble())
            dispose()
        }
    }

    private fun onCancel() {
        dispose()
    }

    companion object {
        inline fun show(c: Component, crossinline onPick: (Double) -> Unit) {
            val frame = JOptionPane.getFrameForComponent(c)
            SurfaceContourPicker(frame) { onPick(it) }.apply {
                pack()
                setLocationRelativeTo(frame)
                isVisible = true
            }
        }
    }
}

fun main(args: Array<String>) {
    SurfaceContourPicker { println(it) }.apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
    System.exit(0)
}