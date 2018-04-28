/*
 * Presets
 * 
 * Created by Yii.Guxing on 2018/04/25.
 */

@file:Suppress("unused")

package cn.yiiguxing.asclepius

import com.google.gson.Gson
import java.io.File
import java.io.FileReader


object Presets {

    private val clutList = mutableListOf<ColorLookUpTable>()
    private val presetsList = mutableListOf<RayCastingPreset>()

    val colorLookUpTables get() = clutList
    val rayCastingPresets get() = presetsList

    fun getColorLookUpTable(name: String) = clutList.find { name == it.name }

    fun getRayCastingPreset(name: String) = presetsList.find { name == it.name }

    fun loadPresets() {
        val gson = Gson()

        val presetsDir = File("presets")
        val colorsPresetsDir = File(presetsDir, "colors")
        val rayCastingPresetsDir = File(presetsDir, "ray-casting")

        colorsPresetsDir
                .listFiles()
                ?.map { gson.fromJson(FileReader(it), ColorLookUpTable::class.java) }
                ?.sortedBy { it.name }
                ?.let {
                    clutList.apply {
                        clear()
                        addAll(it)
                    }
                }

        rayCastingPresetsDir
                .listFiles()
                ?.map { gson.fromJson(FileReader(it), RayCastingPreset::class.java) }
                ?.sortedBy { it.name }
                ?.let {
                    presetsList.apply {
                        clear()
                        addAll(it)
                    }
                }
    }
}


enum class Kernels(val data: DoubleArray) {

    BASIC_SMOOTH_5X5(doubleArrayOf(
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 4.0, 4.0, 4.0, 1.0,
            1.0, 4.0, 12.0, 4.0, 1.0,
            1.0, 4.0, 4.0, 4.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0))

}

enum class Shading(val ambient: Double, val diffuse: Double, val specular: Double, val specularPower: Double) {
    DEFAULT(0.15, 0.9, 0.3, 15.0),
    GLOSSY_VASCULAR(0.15, 0.28, 1.42, 50.0),
    GLOSSY_BONE(0.15, 0.24, 1.17, 6.98),
    ENDOSCOPY(0.12, 0.64, 0.73, 50.0)
}

data class Color(val red: Double, val green: Double, val blue: Double) {

    fun toAWTColor() = java.awt.Color(red.toFloat(), green.toFloat(), blue.toFloat())

    companion object {
        val BLACK = Color(0.0, 0.0, 0.0)
        val WHITE = Color(1.0, 1.0, 1.0)
    }
}

data class Point(val x: Double, val y: Double)

@Suppress("ArrayInDataClass")
data class RayCastingPreset(
        val name: String,
        val group: String,
        val colors: List<List<Color>> = emptyList(),
        val curves: List<List<Point>> = emptyList(),
        val advancedCLUT: Boolean = false,
        val clut: String? = null,
        val mip: Boolean = false,
        val backgroundColor: Color = Color.BLACK,
        val convolutionFilters: List<Kernels> = emptyList(),
        val shading: Shading = Shading.DEFAULT,
        val useShading: Boolean = false,
        val windowLevel: Double = 128.0,
        val windowWidth: Double = 255.0
)

private val defaultColorList = (0..255).map { Color(it / 255.0, it / 255.0, it / 255.0) }

val RayCastingPreset.colorLookUpTable: List<Color>
    get() = clut?.let { Presets.getColorLookUpTable(it) }?.colorList ?: defaultColorList

data class ColorLookUpTable(val name: String, val red: List<Int>, val green: List<Int>, val blue: List<Int>) {

    @Suppress("unused")
    private constructor() : this("Empty", emptyList<Int>(), emptyList<Int>(), emptyList<Int>())

    val colorList: List<Color> by lazy {
        val green = green
        val blue = blue
        red.mapIndexed { index, r -> Color(r / 255.0, green[index] / 255.0, blue[index] / 255.0) }
    }
}