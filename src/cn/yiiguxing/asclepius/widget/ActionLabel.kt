package cn.yiiguxing.asclepius.widget

import java.awt.Color
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JLabel

/**
 * ActionLabel
 *
 * Created by Yii.Guxing on 2018/05/12
 */
class ActionLabel(icon: Icon) : JLabel(icon) {

    var hoveringColor: Color? = null

    var actionListener: (() -> Unit)? = null

    private var isHovering = false

    init {
        addMouseListener(MouseHandler())
    }

    override fun paint(g: Graphics) {
        hoveringColor?.takeIf { isHovering }?.let {
            val oldColor = g.color
            g.color = it
            g.fillRect(0, 0, width, height)
            g.color = oldColor
        }
        super.paint(g)
    }

    private inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 1) {
                actionListener?.invoke()
                e.consume()
            }
        }

        override fun mouseEntered(e: MouseEvent) {
            isHovering = true
            repaint()
        }

        override fun mouseExited(e: MouseEvent) {
            isHovering = false
            repaint()
        }
    }
}