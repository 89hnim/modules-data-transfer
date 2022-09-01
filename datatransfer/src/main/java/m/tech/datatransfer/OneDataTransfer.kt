package m.tech.datatransfer

import androidx.lifecycle.LifecycleOwner
import m.tech.datatransfer.lifecycle.OneDefaultLifecycleObserver
import m.tech.datatransfer.strategy.OneDataTransferStrategy
import m.tech.datatransfer.utils.OneCollectorWeakReference
import java.util.concurrent.ConcurrentHashMap

typealias PendingCollector = () -> Unit

class OneDataTransfer {

    private val collectorLocker = Unit
    private val stickyDataLocker = Unit
    private val collectors = hashSetOf<OneCollectorWeakReference>()
    private val stickyData = hashSetOf<Any>()
    private val pendingCollectors = ConcurrentHashMap<Int, PendingCollector>()

    companion object {
        @Volatile
        private var INSTANCE: OneDataTransfer? = null

        fun get(): OneDataTransfer {
            return INSTANCE ?: synchronized(this) {
                OneDataTransfer().also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * @param data
     * @param isSticky new collector should receive this data or not
     */
    fun emit(data: Any, isSticky: Boolean = false) {
        synchronized(collectorLocker) {
            collectors.forEach { collectorWeakRef ->
                collectorWeakRef.get()?.let { collector ->
                    if (collector.isActive) {
                        collector.onDataChanged(data)
                    } else {
                        pendingCollectors[collector.hashCode()] = { collector.onDataChanged(data) }
                    }
                }
            }
        }
        if (isSticky) {
            synchronized(stickyDataLocker) {
                stickyData.add(data)
            }
        }
    }

    /**
     * @param collector the collector
     * @param strategy how we should collect the emitted values
     */
    fun collect(
        collector: Collector,
        strategy: OneDataTransferStrategy = OneDataTransferStrategy.Always
    ) {
        synchronized(collectorLocker) {
            collectors.add(OneCollectorWeakReference(collector))
        }
        synchronized(stickyDataLocker) {
            stickyData.forEach { data ->
                collector.onDataChanged(data)
            }
        }
        if (strategy is OneDataTransferStrategy.LifecycleAware) {
            strategy.owner?.lifecycle?.addObserver(object : OneDefaultLifecycleObserver() {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    collector.isActive = true
                    pendingCollectors.forEach { (key, pendingCollector) ->
                        if (key == collector.hashCode()) {
                            pendingCollector.invoke()
                        }
                    }
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    collector.isActive = false
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    strategy.free(this) // this wont leak :-/
                    removeCollector(collector)
                }
            })
        }
    }

    fun removeCollector(collector: Collector) {
        synchronized(collectorLocker) {
            collectors.removeAll { it.get() == null || it.get().hashCode() == collector.hashCode() }
        }
    }

    fun removeStickyData(data: Any) {
        synchronized(stickyDataLocker) {
            stickyData.remove(data)
        }
    }

    abstract class Collector {

        internal var isActive: Boolean = true

        abstract fun onDataChanged(data: Any)
    }

}