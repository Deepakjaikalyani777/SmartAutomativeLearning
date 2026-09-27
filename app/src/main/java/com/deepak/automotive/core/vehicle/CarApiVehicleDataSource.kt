package com.deepak.automotive.core.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reads REAL vehicle data on Android Automotive OS.
 *
 *   App ──CarPropertyManager──▶ CarService (system_server-like process)
 *        ──AIDL──▶ Vehicle HAL (IVehicle) ──CAN/LIN/Ethernet──▶ ECUs
 *
 * Only instantiate this class after CarAvailability.isAutomotive() returned true,
 * otherwise android.car.* classes are missing at runtime.
 */
class CarApiVehicleDataSource(private val context: Context) : VehicleDataSource {

    private val _state = MutableStateFlow(VehicleState(source = DataSourceType.CAR_API))
    override val state: StateFlow<VehicleState> = _state.asStateFlow()

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var fuelCapacityMl = 50_000f
    private var batteryCapacityWh = 64_000f

    /** Properties we subscribe to and the rate (Hz) we want them at. */
    private val subscriptions = mapOf(
        VehiclePropertyIds.PERF_VEHICLE_SPEED to CarPropertyManager.SENSOR_RATE_UI,   // CONTINUOUS
        VehiclePropertyIds.ENGINE_RPM to CarPropertyManager.SENSOR_RATE_UI,           // CONTINUOUS
        VehiclePropertyIds.GEAR_SELECTION to CarPropertyManager.SENSOR_RATE_ONCHANGE, // ON_CHANGE
        VehiclePropertyIds.IGNITION_STATE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.PARKING_BRAKE_ON to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.FUEL_LEVEL to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.EV_BATTERY_LEVEL to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.NIGHT_MODE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.HVAC_TEMPERATURE_SET to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.HVAC_FAN_SPEED to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        VehiclePropertyIds.HVAC_AC_ON to CarPropertyManager.SENSOR_RATE_ONCHANGE,
    )

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = onPropertyChanged(value)
        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            reportError("Error event for ${VehiclePropertyIds.toString(propertyId)} area=$areaId")
        }
    }

    override fun start() {
        if (car != null) return
        // Asynchronous connection: CarService may restart (e.g. after a crash); the listener
        // fires again with ready=true and we re-subscribe. Never cache managers across restarts.
        car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { connectedCar, ready ->
            if (ready) onCarReady(connectedCar) else propertyManager = null
        }
    }

    override fun stop() {
        propertyManager?.unregisterCallback(callback)
        propertyManager = null
        car?.disconnect()
        car = null
    }

    @Suppress("DEPRECATION") // registerCallback works from API 29; subscribePropertyEvents is API 35+.
    private fun onCarReady(connectedCar: Car) {
        val manager = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        propertyManager = manager
        readStaticInfo(manager)
        subscriptions.forEach { (propertyId, rate) ->
            try {
                // A property that the OEM did not implement simply has no config.
                if (manager.getCarPropertyConfig(propertyId) != null) {
                    manager.registerCallback(callback, propertyId, rate)
                }
            } catch (e: SecurityException) {
                reportError("Missing permission for ${VehiclePropertyIds.toString(propertyId)}")
            }
        }
    }

    private fun readStaticInfo(manager: CarPropertyManager) {
        // STATIC properties never change, so read them once instead of subscribing.
        runCatching {
            fuelCapacityMl = manager.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, 0)
        }
        runCatching {
            batteryCapacityWh = manager.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)
        }
    }

    private fun onPropertyChanged(value: CarPropertyValue<*>) {
        if (value.status != CarPropertyValue.STATUS_AVAILABLE) return
        val v = value.value ?: return
        _state.update { s ->
            when (value.propertyId) {
                // VHAL reports speed in metres per second.
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> s.copy(speedKmh = (v as Float) * 3.6f)
                VehiclePropertyIds.ENGINE_RPM -> s.copy(rpm = v as Float)
                VehiclePropertyIds.GEAR_SELECTION -> s.copy(gear = mapGear(v as Int))
                VehiclePropertyIds.IGNITION_STATE -> s.copy(ignition = mapIgnition(v as Int))
                VehiclePropertyIds.PARKING_BRAKE_ON -> s.copy(parkingBrakeOn = v as Boolean)
                VehiclePropertyIds.FUEL_LEVEL -> s.copy(fuelPercent = (v as Float) / fuelCapacityMl * 100f)
                VehiclePropertyIds.EV_BATTERY_LEVEL -> s.copy(evBatteryPercent = (v as Float) / batteryCapacityWh * 100f)
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> s.copy(isCharging = v as Boolean)
                VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> s.copy(outsideTempC = v as Float)
                VehiclePropertyIds.NIGHT_MODE -> s.copy(nightMode = v as Boolean)
                VehiclePropertyIds.HVAC_TEMPERATURE_SET -> when {
                    value.areaId and HvacZone.DRIVER.areaId != 0 -> s.copy(driverTempC = v as Float)
                    value.areaId and HvacZone.PASSENGER.areaId != 0 -> s.copy(passengerTempC = v as Float)
                    else -> s
                }
                VehiclePropertyIds.HVAC_FAN_SPEED -> s.copy(fanSpeed = v as Int)
                VehiclePropertyIds.HVAC_AC_ON -> s.copy(acOn = v as Boolean)
                else -> s
            }
        }
    }

    override fun setHvacTemperature(zone: HvacZone, celsius: Float) =
        setHvac(VehiclePropertyIds.HVAC_TEMPERATURE_SET, zone.areaId, Float::class.javaObjectType, celsius)

    override fun setFanSpeed(speed: Int) =
        setHvac(VehiclePropertyIds.HVAC_FAN_SPEED, HvacZone.DRIVER.areaId, Int::class.javaObjectType, speed)

    override fun setAcOn(on: Boolean) =
        setHvac(VehiclePropertyIds.HVAC_AC_ON, HvacZone.DRIVER.areaId, Boolean::class.javaObjectType, on)

    /**
     * HVAC properties are "area" properties: the same property ID exists once per seat/zone.
     * The OEM decides the exact area IDs (e.g. driver may be 0x1 or 0x31 = ROW_1_LEFT|ROW_2_LEFT),
     * so we look up the real area that contains our seat bit.
     */
    private fun <T> setHvac(propertyId: Int, seatBit: Int, type: Class<T>, value: T) {
        val manager = propertyManager ?: return reportError("Car not connected")
        try {
            val areaId = manager.getCarPropertyConfig(propertyId)?.areaIds
                ?.firstOrNull { it and seatBit != 0 }
                ?: return reportError("${VehiclePropertyIds.toString(propertyId)} not supported")
            manager.setProperty(type, propertyId, areaId, value)
        } catch (e: SecurityException) {
            // CONTROL_CAR_CLIMATE is signature|privileged: only OEM-signed or /system/priv-app apps get it.
            reportError("CONTROL_CAR_CLIMATE denied - only OEM/privileged apps may change HVAC")
        } catch (e: IllegalArgumentException) {
            reportError(e.message ?: "Invalid HVAC value")
        }
    }

    private fun reportError(message: String) {
        Log.w(TAG, message)
        _state.update { it.copy(lastError = message) }
    }

    private fun mapGear(gear: Int) = when (gear) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        else -> Gear.DRIVE
    }

    private fun mapIgnition(state: Int) = when (state) {
        VehicleIgnitionState.LOCK -> Ignition.LOCK
        VehicleIgnitionState.OFF -> Ignition.OFF
        VehicleIgnitionState.ACC -> Ignition.ACC
        VehicleIgnitionState.START -> Ignition.START
        else -> Ignition.ON
    }

    private companion object { const val TAG = "CarApiVehicleData" }
}
