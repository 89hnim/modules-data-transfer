package m.tech.feature_b

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import m.tech.datatransfer.OneDataTransfer
import m.tech.datatransfer.scope.OneDataTransferScope
import m.tech.datatransfer.strategy.OneDataTransferStrategy

class FeatureBActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_bactivity)

        OneDataTransfer.get().collect(object : OneDataTransfer.Collector() {
            override fun onDataChanged(data: String) {
                Log.e("DSK", "onDataChanged: feature B $data")
            }
        }, OneDataTransferScope.Custom("FeatureB"), OneDataTransferStrategy.LifecycleAware(this))
    }

    fun emitValue(view: View) {
        OneDataTransfer.get()
            .emit("Value from feature B activity", OneDataTransferScope.Custom("FeatureB"))
    }

    fun emitValue2(view: View) {
        OneDataTransfer.get()
            .emit("Value 2 from feature B activity", OneDataTransferScope.Custom("FeatureB"))
    }
}