package culture.thelove.tlc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import culture.thelove.tlc.android.ui.TlcApp
import culture.thelove.tlc.android.ui.theme.TlcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TlcTheme {
                TlcApp(filesDir = filesDir)
            }
        }
    }
}
