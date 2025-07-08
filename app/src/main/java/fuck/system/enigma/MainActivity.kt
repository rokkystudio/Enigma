package fuck.system.enigma

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager.
            findFragmentById(R.id.main_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val navView: BottomNavigationView = findViewById(R.id.main_nav_view)

        val appBarConfiguration = AppBarConfiguration(
            setOf( R.id.navigation_encoder, R.id.navigation_decoder, R.id.navigation_about )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }
}