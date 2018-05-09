package cn.yiiguxing.asclepius

data class SurfaceInfo(
        val contourValue: Double,
        val color: Color,
        val smoothing: Boolean,
        val numberOfSmoothingIterations: Int)