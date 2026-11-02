package culture.thelove.tlc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import culture.thelove.tlc.android.ui.TlcApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // TlcApp owns the theme: the preference is part of the durable, local state.
            TlcApp(filesDir = filesDir)
        }
    }
}
