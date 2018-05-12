package cn.yiiguxing.asclepius

import javax.swing.Icon
import javax.swing.ImageIcon

object Icons {
    val MAXIMIZE: Icon = loadIcon("icons/maximize.png")
    val RESTORE: Icon = loadIcon("icons/restore.png")
}

private fun loadIcon(path: String): Icon {
    return ImageIcon(Icons::class.java.classLoader.getResource(path))
}