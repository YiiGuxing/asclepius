package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.form.VolumePanelForm
import cn.yiiguxing.asclepius.widget.MaximizablePanel
import java.awt.BorderLayout
import java.awt.Color

/**
 * VolumePanel
 *
 * Created by Yii.Guxing on 2018/05/12
 */
class VolumePanel : VolumePanelForm(), MaximizablePanel {

    val volumeViewer = VolumeViewer()

    override var maximizeActionListener: MaximizablePanel.MaximizeActionListener? = null
    private var _isMaximize = false
    override val isMaximize: Boolean
        get() = _isMaximize

    init {
        contentPanel.add(volumeViewer.component, BorderLayout.CENTER)

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

}