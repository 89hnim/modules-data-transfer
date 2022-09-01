package m.tech.feature_a

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import m.tech.datatransfer.OneDataTransfer

class FeatureAActivity : AppCompatActivity() {

    private val collector = object: OneDataTransfer.Collector() {
        override fun onDataChanged(data: Any) {
            Log.e("DSK", "onDataChanged: Feature A $data")
            OneDataTransfer.get().removeCollector(this)
            OneDataTransfer.get().removeStickyData(data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_aactivity)

        OneDataTransfer.get().collect(collector)
    }

    fun emitValue(view: View) {
        OneDataTransfer.get().emit(TestA("Value from feature A"))
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}