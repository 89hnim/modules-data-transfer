package m.tech.datatransfer.strategy

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

sealed class OneDataTransferStrategy {
    /**
     * always collect values
     */
    object Always: OneDataTransferStrategy()
    /**
     * respect lifecycle to collect values
     * start collecting when onStart()
     * stop collecting when onPause()
     * remove collector when onDestroy()
     */
    class LifecycleAware(var owner: LifecycleOwner?): OneDataTransferStrategy() {

        // avoid leak. call when onDestroy()
        internal fun free(observer: LifecycleObserver) {
            owner?.lifecycle?.removeObserver(observer)
            owner = null
        }
    }

}