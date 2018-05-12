package cn.yiiguxing.asclepius.widget

/**
 * MaximizablePanel
 *
 * Created by Yii.Guxing on 2018/05/12
 */
interface MaximizablePanel {

    val isMaximize: Boolean
    var maximizeActionListener: MaximizeActionListener?

    fun onMaximize(): Boolean = false

    fun onRestore(): Boolean = false

    fun fireToggleMaximize() {
        if (isMaximize) {
            if (onRestore()) {
                maximizeActionListener?.onRestore(this)
            }
        } else {
            if (onMaximize()) {
                maximizeActionListener?.onMaximize(this)
            }
        }
    }

    interface MaximizeActionListener {
        fun onMaximize(panel: MaximizablePanel) {}
        fun onRestore(panel: MaximizablePanel) {}
    }
}