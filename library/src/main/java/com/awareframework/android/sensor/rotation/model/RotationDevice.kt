package com.awareframework.android.sensor.rotation.model

import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

/**
 * Holds the information of the rotation sensor of the device
 *
 * @author  sercant
 * @date 20/08/2018
 */
data class RotationDevice(
        var maxRange: Float = 0f,
        var minDelay: Float = 0f,
        var name: String = "",
        var power: Float = 0f, // in Ma
        var resolution: Float = 0f,
        var type: String = "",
        var vendor: String = "",
        var version: String = ""
): AwareObject(jsonVersion = 1) {

    companion object {
        const val TABLE_NAME = "rotationDevice"
    }

    override fun toString(): String = Gson().toJson(this)
}