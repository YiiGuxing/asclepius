package cn.yiiguxing.asclepius

/**
 * Color
 *
 * Created by Yii.Guxing on 2018/05/12
 */
data class Color(val red: Double, val green: Double, val blue: Double) {

    fun toAWTColor() = AWTColor(red.toFloat(), green.toFloat(), blue.toFloat())

    companion object {
        val BLACK = Color(0.0, 0.0, 0.0)
        val WHITE = Color(1.0, 1.0, 1.0)
    }
}

typealias AWTColor = java.awt.Color

fun AWTColor.toColor() = Color(red / 255.0, green / 255.0, blue / 255.0)