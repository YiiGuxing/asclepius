/*
 * Arrays
 * 
 * Created by Yii.Guxing on 2018/04/12.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package vtk.extensions.internal

inline fun IntArray.fill(f: (index: Int) -> Int) {
    for (i in indices) {
        this[i] = f(i)
    }
}

inline operator fun DoubleArray.component6(): Double {
    return get(5)
}