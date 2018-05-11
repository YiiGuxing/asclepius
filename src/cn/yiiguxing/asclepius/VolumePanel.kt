package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.form.VolumePanelForm
import java.awt.BorderLayout

/**
 * VolumePanel
 *
 * Created by Yii.Guxing on 2018/05/12
 */
class VolumePanel : VolumePanelForm() {

    val volumeViewer = VolumeViewer()

    init {
        contentPanel.add(volumeViewer.component, BorderLayout.CENTER)
    }

}