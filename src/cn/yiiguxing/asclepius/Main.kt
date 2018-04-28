/*
 * Main
 * 
 * Created by Yii.Guxing on 2018/04/27.
 */

package cn.yiiguxing.asclepius

import vtk.extensions.VTK
import java.io.File

fun main(args: Array<String>) {
    VTK.loadAllNativeLibraries(true)
    VTK.disableOutputWindow(File(File("log").apply { mkdirs() }, "vtkError.log"))
    Presets.loadPresets()

    // TODO 指定DICOM序列目录（目录内所有DICOM文件必须属于同一序列），由于使用的是vtkDICOMImageReader，所以一些DICOM文件无法读取
    val dcmDir = File("D:\\Home\\Data\\dcm\\zzl\\0021608414\\7")
    AsclepiusFrame(dcmDir).apply {
        setSize(1280, 1080)
        setLocationRelativeTo(null)
        isVisible = true
    }
}