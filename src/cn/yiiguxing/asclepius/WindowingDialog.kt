package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.form.WindowingDialogForm
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import java.awt.Component
import java.awt.Frame
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class WindowingDialog private constructor(
        owner: Frame? = null,
        initWindowWidth: Double = 255.0,
        initWindowLevel: Double = 127.0
) : WindowingDialogForm(owner, "Edit Windowing") {

    var windowLevel: WindowLevel? = null
        private set

    init {
        isResizable = false
        setContentPane(contentPane)
        getRootPane().defaultButton = buttonOK

        windowWidthSpinner.model = SpinnerNumberModel(initWindowWidth.toInt(), 1, 10000, 1)
        windowLevelSpinner.model = SpinnerNumberModel(initWindowLevel.toInt(), -10000, 10000, 1)

        buttonOK.addActionListener { onOK() }
        buttonCancel.addActionListener { onCancel() }

        // call onCancel() when cross is clicked
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
        val ww = (windowWidthSpinner.value as Int).toDouble()
        val wl = (windowLevelSpinner.value as Int).toDouble()
        windowLevel = WindowLevel(ww, wl)
        dispose()
    }

    private fun onCancel() {
        windowLevel = null
        dispose()
    }

    companion object {
        fun show(c: Component? = null, initWindowWidth: Double = 255.0, initWindowLevel: Double = 127.0): WindowLevel? {
            val frame = c?.let { JOptionPane.getFrameForComponent(it) }
            return with(WindowingDialog(frame, initWindowWidth, initWindowLevel)) {
                pack()
                setLocationRelativeTo(frame)
                isVisible = true

                windowLevel
            }
        }
    }
}

fun main(args: Array<String>) {
    UIManager.setLookAndFeel(WindowsLookAndFeel())
    println(WindowingDialog.show())
    System.exit(0)
}