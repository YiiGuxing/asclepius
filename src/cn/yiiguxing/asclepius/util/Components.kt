/*
 * Components
 *
 * Created by Yii.Guxing on 2018/05/12
 */

package cn.yiiguxing.asclepius.util

import java.awt.Component
import java.awt.Container

inline fun Container.forEachComponent(action: (Component) -> Unit) {
    for (i in 0 until componentCount) {
        action(getComponent(i))
    }
}

inline fun Container.forEachComponentIndexed(action: (Int, Component) -> Unit) {
    for (i in 0 until componentCount) {
        action(i, getComponent(i))
    }
}

inline fun Container.findComponent(predicate: (Component) -> Boolean): Component? {
    for (i in 0 until componentCount) {
        val com = getComponent(i)
        if (predicate(com)) {
            return com
        }
    }

    return null
}