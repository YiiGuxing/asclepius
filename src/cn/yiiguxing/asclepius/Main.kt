/*
 * Main
 * 
 * Created by Yii.Guxing on 2018/04/27.
 */

package cn.yiiguxing.asclepius

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import vtk.extensions.VTK
import vtk.vtkDICOMImageReader
import java.io.File
import javax.swing.UIManager

fun main(args: Array<String>) {
    VTK.loadAllNativeLibraries(true)
    VTK.disableOutputWindow(File(File("log").apply { mkdirs() }, "vtkError.log"))
    Presets.loadPresets()

    UIManager.setLookAndFeel(WindowsLookAndFeel())

    val frame = AsclepiusFrame().apply {
        setSize(1280, 900)
        setLocationRelativeTo(null)
    }

    // TODO 指定DICOM序列目录（目录内所有DICOM文件必须属于同一序列），由于使用的是vtkDICOMImageReader，所以一些DICOM文件无法读取
    // 注意：DICOM文件路径中只可包含ASCII字符，不可包含中文字符，否则DICOM数据将无法读取，图像将不能正常渲染。
    val dcmDir = File(args[0])
    val reader = vtkDICOMImageReader().apply {
        SetDirectoryName(dcmDir.absolutePath)
        SetDataByteOrderToLittleEndian()
        Update()
    }

    frame.setReader(reader)
    frame.isVisible = true
}
