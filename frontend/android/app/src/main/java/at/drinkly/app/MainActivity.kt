package at.drinkly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import at.drinkly.app.ui.DrinklyApp
import at.drinkly.app.ui.DrinklyViewModel
import at.drinkly.app.ui.theme.DrinklyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DrinklyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrinklyTheme {
                DrinklyApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
