/*
 * VTKNative
 * 
 * Created by Yii.Guxing on 2018/04/04.
 */


package vtk.extensions

import vtk.vtkNativeLibrary
import vtk.vtkObjectBase
import java.io.File

object VTK {

    fun loadAllNativeLibraries(debug: Boolean = false) {
        if (!vtkNativeLibrary.LoadAllNativeLibraries() && debug) {
            for (lib in vtkNativeLibrary.values()) {
                if (!lib.IsLoaded()) {
                    println(lib.GetLibraryName() + " not loaded")
                }
            }
        }
    }

    fun disableOutputWindow(logFile: File) {
        vtkNativeLibrary.DisableOutputWindow(logFile)
    }

    fun gc() {
        vtkObjectBase.JAVA_OBJECT_MANAGER.gc(false)
    }
}