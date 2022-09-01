package m.tech.datatransfer

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import m.tech.datatransfer.lifecycle.OneDefaultLifecycleObserver
import m.tech.datatransfer.scope.OneDataTransferScope
import m.tech.datatransfer.strategy.OneDataTransferStrategy
import m.tech.datatransfer.utils.OneCollectorWeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

typealias PendingCollector = () -> Unit
typealias StickyData = Pair<String, OneDataTransferScope>

class OneDataTransfer {

    private val gson = Gson()
    private val collectorLocker = Any()
    private val stickyDataLocker = Any()
    private val collectors = hashSetOf<OneCollectorWeakReference>()
    private val stickyData = hashSetOf<StickyData>()
    private val pendingCollectors = ConcurrentHashMap<Int, PendingCollector?>()

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
     * @param data data to transfer
     * @param scope scope of emitting data
     * @param isSticky new collector should receive this data or not
     */
    fun emit(
        data: Any,
        scope: OneDataTransferScope = OneDataTransferScope.Application,
        isSticky: Boolean = false
    ) {
        val rawData = gson.toJson(data)
        synchronized(collectorLocker) {
            Log.e("DSK", "emit: emitting $data ${data.hashCode()}")
            collectors.forEach { collectorWeakRef ->
                collectorWeakRef.get()?.let { collector ->
                    // check if collector's scope is the same with emitting data scope
                    if (collector.scope == scope) {
                        // only collect data if collector is active
                        if (collector.isActive) {
                            collector.onDataChanged(rawData)
                        } else {
                            // if not active, add to pending collectors queue to execute later
                            pendingCollectors[collector.hashCode()] =
                                { collector.onDataChanged(rawData) }
                        }
                    }
                }
            }
        }
        if (isSticky) {
            synchronized(stickyDataLocker) {
                stickyData.add(rawData to scope)
            }
        }
    }

    /**
     * @param collector the collector
     * @param scope collect emitted values in this scope only
     * @param strategy how we should collect the emitted values
     */
    fun collect(
        collector: Collector,
        scope: OneDataTransferScope = OneDataTransferScope.Application,
        strategy: OneDataTransferStrategy = OneDataTransferStrategy.Always
    ) {
        collector.scope = scope
        synchronized(collectorLocker) {
            collectors.add(OneCollectorWeakReference(collector))
        }
        synchronized(stickyDataLocker) {
            stickyData.forEach {
                val data = it.first
                val stickyDataScope = it.second
                if (collector.scope == stickyDataScope) {
                    collector.onDataChanged(data)
                }
            }
        }
        if (strategy is OneDataTransferStrategy.LifecycleAware) {
            strategy.owner?.lifecycle?.addObserver(object : OneDefaultLifecycleObserver() {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    collector.isActive = true
                    pendingCollectors[collector.hashCode()]?.let { pendingCollector ->
                        pendingCollector.invoke()
                        pendingCollectors.remove(collector.hashCode())
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
            Log.e("DSK", "removeCollector: removed $collector")
        }
    }

    fun removeStickyData(
        data: Any,
        scope: OneDataTransferScope = OneDataTransferScope.Application
    ) {
        synchronized(collectorLocker) {
            stickyData.removeAll { it.first == data && it.second == scope }
            Log.e("DSK", "removeStickyData: removed $data in $scope")
        }
    }

    fun removeAllStickyData(scope: OneDataTransferScope = OneDataTransferScope.Application) {
        synchronized(stickyDataLocker) {
            stickyData.removeAll { it.second == scope }
        }
    }

    abstract class Collector {

        internal var scope: OneDataTransferScope = OneDataTransferScope.Application

        internal var isActive: Boolean = true

        abstract fun onDataChanged(data: String)
    }

}