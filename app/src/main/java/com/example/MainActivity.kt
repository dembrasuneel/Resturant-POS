package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.repository.PosRepository
import com.example.hardware.payment.MockCardPaymentService
import com.example.hardware.printer.MockPosPrinterService
import com.example.ui.navigation.PosNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.PosViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val printerService = MockPosPrinterService()
        val cardPaymentService = MockCardPaymentService()
        val repository = PosRepository(database, printerService, cardPaymentService)
        val viewModelFactory = PosViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PosViewModel = viewModel(factory = viewModelFactory)
                    PosNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
