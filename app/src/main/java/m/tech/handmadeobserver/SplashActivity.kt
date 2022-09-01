package m.tech.handmadeobserver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import m.tech.datatransfer.OneDataTransfer
import m.tech.datatransfer.strategy.OneDataTransferStrategy

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        OneDataTransfer.get().collect(object: OneDataTransfer.Collector() {
            override fun onDataChanged(data: Any) {
                Log.e("DSK", "onDataChanged: splashAct $data")
            }
        }, OneDataTransferStrategy.Always)
    }

    fun navMain(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
    }
}