package m.tech.datatransfer.utils

import m.tech.datatransfer.OneDataTransfer
import java.lang.ref.WeakReference

internal class OneCollectorWeakReference(
    private val collector: OneDataTransfer.Collector
) : WeakReference<OneDataTransfer.Collector>(collector) {

    override fun equals(other: Any?): Boolean {
        return if (other !is OneCollectorWeakReference) false
        else collector == other.collector
    }

    override fun hashCode(): Int {
        return collector.hashCode()
    }

}