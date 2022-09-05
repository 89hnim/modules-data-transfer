package m.tech.datatransfer

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import m.tech.datatransfer.lifecycle.OneDefaultLifecycleObserver
import m.tech.datatransfer.scope.OneDataTransferScope
import m.tech.datatransfer.strategy.OneDataTransferStrategy
import m.tech.datatransfer.utils.OneCollectorWeakReference
import m.tech.datatransfer.utils.OnePendingCollector

// pair: raw data - scope
internal typealias StickyData = Pair<String, OneDataTransferScope>

class OneDataTransfer {

    private val internalCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val collectorLocker = Mutex()
    private val stickyDataLocker = Mutex()
    private val pendingCollectorsLocker = Mutex()

    private val collectors = hashSetOf<OneCollectorWeakReference>()
    private val stickyData = hashSetOf<StickyData>()
    private val pendingCollectors = hashSetOf<OnePendingCollector>()

    private val gson = Gson()

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
        internalCoroutineScope.launch {
            val rawData = gson.toJson(data)
            collectorLocker.withLock {
                collectors.forEach { collectorWeakRef ->
                    collectorWeakRef.get()?.let { collector ->
                        // check if collector's scope is the same with emitting data scope
                        if (collector.scope == scope) {
                            // only collect data if collector is active
                            if (collector.isActive) {
                                collector.onDataChanged(rawData)
                            } else {
                                // if not active, add to pending collectors queue to execute later
                                pendingCollectorsLocker.withLock {
                                    pendingCollectors.add(
                                        OnePendingCollector(
                                            collectorHashCode = collector.hashCode(),
                                            rawData = rawData,
                                            pendingExecution = { collector.onDataChanged(rawData) }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isSticky) {
                stickyDataLocker.withLock {
                    stickyData.add(rawData to scope)
                }
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
        internalCoroutineScope.launch {
            collector.scope = scope
            collectorLocker.withLock {
                collectors.add(OneCollectorWeakReference(collector))
            }

            // emit sticky data to the collector
            stickyDataLocker.withLock {
                stickyData.forEach {
                    val data = it.first
                    val stickyDataScope = it.second
                    if (collector.scope == stickyDataScope) {
                        collector.onDataChanged(data)
                    }
                }
            }

            // setup lifecycle aware
            if (strategy is OneDataTransferStrategy.LifecycleAware) {
                strategy.owner?.lifecycle?.addObserver(object : OneDefaultLifecycleObserver() {
                    override fun onStart(owner: LifecycleOwner) {
                        super.onStart(owner)
                        collector.isActive = true

                        // invoke pending collector (in case the view returns visible)
                        invokePendingCollectors(collector.hashCode())
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
    }

    private fun invokePendingCollectors(collectorHashCode: Int) {
        internalCoroutineScope.launch {
            pendingCollectorsLocker.withLock {
                // trigger collect of all pending collectors
                pendingCollectors.filter { it.collectorHashCode == collectorHashCode }.forEach {
                    it.pendingExecution.invoke()
                }
                // remove pending collectors after collected
                pendingCollectors.removeAll { it.collectorHashCode == collectorHashCode }
            }
        }
    }

    fun removeCollector(collector: Collector) {
        internalCoroutineScope.launch {
            collectorLocker.withLock {
                collectors.removeAll {
                    it.get() == null || it.hashCode() == collector.hashCode()
                }
            }
        }
    }

    fun removeStickyData(
        data: String,
        scope: OneDataTransferScope = OneDataTransferScope.Application
    ) {
        internalCoroutineScope.launch {
            stickyDataLocker.withLock {
                stickyData.removeAll { it.first == data && it.second == scope }
            }
        }
    }

    fun removeAllStickyData(scope: OneDataTransferScope = OneDataTransferScope.Application) {
        internalCoroutineScope.launch {
            stickyDataLocker.withLock {
                stickyData.removeAll { it.second == scope }
            }
        }
    }

    abstract class Collector {

        internal var scope: OneDataTransferScope = OneDataTransferScope.Application

        internal var isActive: Boolean = true

        abstract fun onDataChanged(data: String)
    }

}