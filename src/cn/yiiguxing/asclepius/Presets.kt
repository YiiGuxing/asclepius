/*
 * Presets
 * 
 * Created by Yii.Guxing on 2018/04/25.
 */

@file:Suppress("unused")

package cn.yiiguxing.asclepius

import cn.yiiguxing.asclepius.util.readJson
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import vtk.vtkLookupTable
import java.io.File


object Presets {

    private val clutMap = LinkedHashMap<String, ColorLookUpTable>()
    private val presetsMap = LinkedHashMap<String, RayCastingPreset>()
    private val lutPresets = LinkedHashMap<String, vtkLookupTable>()

    val colorLookUpTables: Map<String, ColorLookUpTable> get() = clutMap
    val rayCastingPresets: Map<String, RayCastingPreset> get() = presetsMap
    val lookUpTablePresets: Map<String, vtkLookupTable> get() = lutPresets

    val windowLevel: Map<String, WindowLevel> = mapOf(
            "Abdomen" to WindowLevel(400.0, 10.0),
            "Bone" to WindowLevel(2500.0, 300.0),
            "Brain" to WindowLevel(80.0, 40.0),
            "Felsenbein" to WindowLevel(4000.0, 500.0),
            "Lung" to WindowLevel(1600.0, -400.0),
            "Mediastinum" to WindowLevel(450.0, 10.0),
            "Skull" to WindowLevel(95.0, 25.0),
            "Spine" to WindowLevel(300.0, 20.0)
    )

    fun getColorLookUpTable(name: String) = clutMap[name]?.colorList

    fun getLookUpTablePresets(name: String) = lutPresets[name]

    fun getRayCastingPreset(name: String) = presetsMap[name]

    fun getWindowLevelPreset(name: String) = windowLevel[name]

    fun loadPresets() {
        val gson = Gson()

        val presetsDir = File("presets")
        val colorsPresetsDir = File(presetsDir, "colors")
        val rayCastingPresetsDir = File(presetsDir, "ray-casting")
        val defaultLookupTablesFile = File(presetsDir, "default_lookup_tables.json")

        val lookupTables = defaultLookupTablesFile
                .readJson<List<LookUpTablePreset>>(gson, LookUpTablePresetList().type)
                .map { it.name to it.vtkLookupTable }
                .toMutableList()

        val clutMap = clutMap.apply { clear() }
        colorsPresetsDir
                .listFiles()
                ?.map { it.readJson<ColorLookUpTableInner>(gson).asColorLookUpTable() }
                ?.let {
                    clutMap.apply { clear() } += it.sortedBy { it.name }.sortedBy { it.name }.map { it.name to it }
                    lookupTables += it.map { it.name to it.vtkLookupTable }
                }

        lutPresets.apply { clear() } += lookupTables.sortedBy { it.first }

        rayCastingPresetsDir
                .listFiles()
                ?.map { it.readJson<RayCastingPreset>(gson) }
                ?.sortedBy { it.name }
                ?.map { it.name to it }
                ?.let { presets ->
                    presetsMap.apply { clear() } += presets
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

data class WindowLevel(val windowWidth: Double, val windowLevel: Double)

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
    get() = clut?.let { Presets.getColorLookUpTable(it) } ?: defaultColorList

private data class ColorLookUpTableInner(
        val name: String,
        val red: List<Int>,
        val green: List<Int>,
        val blue: List<Int>)

private fun ColorLookUpTableInner.asColorLookUpTable(): ColorLookUpTable {
    val (name, red, green, blue) = this
    val colorList = red.mapIndexed { index, r ->
        Color(r / 255.0, green[index] / 255.0, blue[index] / 255.0)
    }

    return ColorLookUpTable(name, colorList)
}

data class ColorLookUpTable(val name: String, val colorList: List<Color>) {

    val vtkLookupTable
        get() = vtk.vtkLookupTable().apply {
            SetNumberOfTableValues(colorList.size)
            colorList.forEachIndexed { index, color ->
                SetTableValue(index, color.red, color.green, color.blue, 1.0)
            }
        }

}

private class LookUpTablePresetList : TypeToken<List<LookUpTablePreset>>()

data class LookUpTablePreset(val name: String, private val ranges: List<Double>) {

    val vtkLookupTable
        get() = vtk.vtkLookupTable().apply {
            val ranges = ranges
            SetSaturationRange(ranges[0], ranges[1])
            SetHueRange(ranges[2], ranges[3])
            SetValueRange(ranges[4], ranges[5])
            Build()
        }

}