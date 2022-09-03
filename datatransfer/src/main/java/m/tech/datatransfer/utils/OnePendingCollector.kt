package m.tech.datatransfer.utils

/**
 * need override equals b/c
 * [pendingExecution] will be created new object every time
 */
internal class OnePendingCollector(
    val collectorHashCode: Int,
    val rawData: String,
    val pendingExecution: () -> Unit,
) {

    override fun hashCode(): Int {
        return rawData.hashCode() + collectorHashCode
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is OnePendingCollector) false
        else rawData == other.rawData
                && collectorHashCode == other.collectorHashCode
    }


}