package luigi.wal.walgif.utils


object RectangleCalculator {

    @JvmStatic
    fun getRectanglePoints(width: Int, height: Int, dimension: Pair<Int, Int>): List<Pair<Int, Int>> {
        var list: List<Pair<Int, Int>> = emptyList()
        val xMax = width / dimension.first
        val yMax = height / dimension.second
        for (i in 0 until dimension.first) {
            for (j in 0 until dimension.second) {
                list += Pair(i * xMax, j * yMax)
            }
        }
        return list

    }


    @JvmStatic
    fun getRectDimension(width: Int, height: Int, dimension: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(width / dimension.first, height / dimension.second)


    }

}
