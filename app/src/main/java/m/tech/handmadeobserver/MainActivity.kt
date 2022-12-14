package m.tech.handmadeobserver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import m.tech.datatransfer.OneDataTransfer
import m.tech.datatransfer.scope.OneDataTransferScope
import m.tech.datatransfer.strategy.OneDataTransferStrategy
import m.tech.feature_a.FeatureAActivity
import m.tech.feature_b.FeatureBActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        OneDataTransfer.get().collect(object : OneDataTransfer.Collector() {
            override fun onDataChanged(data: String) {
                Log.e("DSK", "onDataChanged: mainAct [Application] $data")
            }
        }, OneDataTransferScope.Application, OneDataTransferStrategy.LifecycleAware(this))

        OneDataTransfer.get().collect(object : OneDataTransfer.Collector() {
            override fun onDataChanged(data: String) {
                Log.e("DSK", "onDataChanged: mainAct [FeatureB] $data")
            }
        }, OneDataTransferScope.Custom("FeatureB"), OneDataTransferStrategy.LifecycleAware(this))
    }

    fun navA(view: View) {
        startActivity(Intent(this, FeatureAActivity::class.java))
    }

    fun navB(view: View) {
        startActivity(Intent(this, FeatureBActivity::class.java))
    }

    fun emitValue(view: View) {
        OneDataTransfer
            .get()
            .emit("Sticky event from main act", OneDataTransferScope.Application, true)
    }

    fun emitValueB(view: View) {
        OneDataTransfer
            .get()
            .emit("Sticky event from main act", OneDataTransferScope.Custom("FeatureB"), true)
        OneDataTransfer
            .get()
            .emit("Sticky event 2 from main act", OneDataTransferScope.Custom("FeatureB"), true)
    }
}