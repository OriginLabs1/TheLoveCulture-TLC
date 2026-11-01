package culture.thelove.tlc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import culture.thelove.tlc.android.ui.FeeFlowScreen
import culture.thelove.tlc.android.ui.theme.TlcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TlcTheme {
                FeeFlowScreen()
            }
        }
    }
}
