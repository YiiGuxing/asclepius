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

    AsclepiusFrame().apply {
        setSize(1280, 1080)
        setLocationRelativeTo(null)
        isVisible = true
    }
}