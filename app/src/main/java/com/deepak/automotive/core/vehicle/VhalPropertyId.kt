package com.deepak.automotive.core.vehicle

/**
 * A VHAL property ID is a packed 32-bit int:
 *
 *   0x1 1 60 0207
 *     │ │ │  └── unique ID        (bits 0-15)
 *     │ │ └───── value type       (bits 16-23)  0x60 = FLOAT
 *     │ └─────── area type        (bits 24-27)  0x1  = GLOBAL
 *     └───────── property group   (bits 28-31)  0x1  = SYSTEM (0x2 = VENDOR/OEM)
 */
data class VhalPropertyId(val raw: Int) {
    val group: Int get() = raw and 0xF0000000.toInt()
    val area: Int get() = raw and 0x0F000000
    val type: Int get() = raw and 0x00FF0000
    val uniqueId: Int get() = raw and 0x0000FFFF

    val groupName: String get() = when (group) {
        0x10000000 -> "SYSTEM"
        0x20000000 -> "VENDOR"
        0x30000000 -> "BACKPORTED"
        else -> "UNKNOWN"
    }
    val areaName: String get() = when (area) {
        0x01000000 -> "GLOBAL"
        0x03000000 -> "WINDOW"
        0x04000000 -> "MIRROR"
        0x05000000 -> "SEAT"
        0x06000000 -> "DOOR"
        0x07000000 -> "WHEEL"
        0x08000000 -> "VENDOR"
        else -> "UNKNOWN"
    }
    val typeName: String get() = when (type) {
        0x00100000 -> "STRING"
        0x00200000 -> "BOOLEAN"
        0x00400000 -> "INT32"
        0x00410000 -> "INT32_VEC"
        0x00500000 -> "INT64"
        0x00510000 -> "INT64_VEC"
        0x00600000 -> "FLOAT"
        0x00610000 -> "FLOAT_VEC"
        0x00700000 -> "BYTES"
        0x00E00000 -> "MIXED"
        else -> "UNKNOWN"
    }

    fun hex(): String = "0x%08X".format(raw)

    companion object {
        /** Build an OEM-specific property the way a VHAL engineer would. */
        fun vendor(area: Int, type: Int, id: Int) = VhalPropertyId(0x20000000 or area or type or (id and 0xFFFF))
    }
}

enum class ChangeMode { STATIC, ON_CHANGE, CONTINUOUS }
enum class Access { READ, WRITE, READ_WRITE }

/** Catalog of the properties used in this app (IDs match android.car.VehiclePropertyIds). */
data class VhalPropertyInfo(
    val name: String, val id: Int, val changeMode: ChangeMode, val access: Access,
    val permission: String, val unit: String,
)

object VhalCatalog {
    val properties = listOf(
        VhalPropertyInfo("PERF_VEHICLE_SPEED", 0x11600207, ChangeMode.CONTINUOUS, Access.READ, "CAR_SPEED", "m/s"),
        VhalPropertyInfo("ENGINE_RPM", 0x11600305, ChangeMode.CONTINUOUS, Access.READ, "CAR_ENGINE_DETAILED", "rpm"),
        VhalPropertyInfo("GEAR_SELECTION", 0x11400400, ChangeMode.ON_CHANGE, Access.READ, "CAR_POWERTRAIN", "VehicleGear"),
        VhalPropertyInfo("PARKING_BRAKE_ON", 0x11200402, ChangeMode.ON_CHANGE, Access.READ, "CAR_POWERTRAIN", "bool"),
        VhalPropertyInfo("IGNITION_STATE", 0x11400409, ChangeMode.ON_CHANGE, Access.READ, "CAR_POWERTRAIN", "enum"),
        VhalPropertyInfo("FUEL_LEVEL", 0x11600307, ChangeMode.CONTINUOUS, Access.READ, "CAR_ENERGY", "mL"),
        VhalPropertyInfo("EV_BATTERY_LEVEL", 0x11600309, ChangeMode.CONTINUOUS, Access.READ, "CAR_ENERGY", "Wh"),
        VhalPropertyInfo("ENV_OUTSIDE_TEMPERATURE", 0x11600703, ChangeMode.CONTINUOUS, Access.READ, "CAR_EXTERIOR_ENVIRONMENT", "°C"),
        VhalPropertyInfo("NIGHT_MODE", 0x11200407, ChangeMode.ON_CHANGE, Access.READ, "CAR_EXTERIOR_ENVIRONMENT", "bool"),
        VhalPropertyInfo("HVAC_TEMPERATURE_SET", 0x15600503, ChangeMode.ON_CHANGE, Access.READ_WRITE, "CONTROL_CAR_CLIMATE", "°C"),
        VhalPropertyInfo("HVAC_FAN_SPEED", 0x15400500, ChangeMode.ON_CHANGE, Access.READ_WRITE, "CONTROL_CAR_CLIMATE", "level"),
        VhalPropertyInfo("INFO_MAKE", 0x11100101, ChangeMode.STATIC, Access.READ, "CAR_INFO", "string"),
        VhalPropertyInfo("DOOR_LOCK", 0x16200B02, ChangeMode.ON_CHANGE, Access.READ_WRITE, "CONTROL_CAR_DOORS", "bool"),
    )
}
