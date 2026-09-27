package com.deepak.automotive.carapp

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

/**
 * Car App Library entry point (Android Auto + Android Automotive OS).
 * The HOST (Android Auto / AAOS system UI) binds to this service and renders our TEMPLATES.
 * We never draw pixels ourselves - which is why templates are automatically
 * distraction-optimized and match every OEM's look.
 */
class ChargingStationCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        // SECURITY: in release, only allow known hosts (Android Auto, AAOS templates host)
        // identified by package name + signing certificate digest.
        return if (debuggable) HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        else HostValidator.Builder(applicationContext)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = StationListScreen(carContext)
    }
}

data class ChargingStation(
    val name: String, val address: String, val powerKw: Int,
    val freePorts: Int, val lat: Double, val lng: Double,
)

private val stations = listOf(
    ChargingStation("Tata Power EZ Charge", "Whitefield, Bengaluru", 60, 2, 12.9698, 77.7500),
    ChargingStation("Statiq Hub", "Koramangala, Bengaluru", 120, 4, 12.9352, 77.6245),
    ChargingStation("ChargeZone Mall", "Hebbal, Bengaluru", 30, 0, 13.0358, 77.5970),
)

class StationListScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        // Hosts cap list length while driving (typically 6 rows, OEM-defined): templates enforce it.
        val list = ItemList.Builder().apply {
            stations.forEach { s ->
                addItem(
                    Row.Builder()
                        .setTitle(s.name)
                        .addText("${s.powerKw} kW DC · ${s.freePorts} free · ${s.address}")
                        .setOnClickListener { screenManager.push(StationDetailScreen(carContext, s)) }
                        .build()
                )
            }
        }.build()
        return ListTemplate.Builder()
            .setTitle("Nearby chargers")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list)
            .build()
    }
}

class StationDetailScreen(carContext: CarContext, private val station: ChargingStation) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val navigate = Action.Builder()
            .setTitle("Navigate")
            .setOnClickListener {
                // Hand off to the car's navigation app via the standard geo intent.
                carContext.startCarApp(
                    Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:${station.lat},${station.lng}"))
                )
            }.build()
        val pane = Pane.Builder()
            .addRow(Row.Builder().setTitle("Address").addText(station.address).build())
            .addRow(Row.Builder().setTitle("Max power").addText("${station.powerKw} kW").build())
            .addRow(Row.Builder().setTitle("Free connectors").addText("${station.freePorts}").build())
            .addAction(navigate)
            .build()
        return PaneTemplate.Builder(pane)
            .setTitle(station.name)
            .setHeaderAction(Action.BACK)
            .build()
    }
}
