package com.icov.app.models

import java.util.*

class UserMongoDb {
    companion object {
        var firstName: String? = null
        var surname: String? = null
        var fullName: String? = null
        var email: String? = null
        var password: String? = null

        var checkedIn: Boolean = false
        var checkedInTimeToday: String? = null
        var checkedInDateToday: String? = null
        var checkedInDateFormat: Date? = null


        var checkedOut: Boolean = false
        var checkedOutTimeToday: String? = null
        var checkedOutDateToday: String? = null
        var checkedOutDateFormat: Date? = null

        var checkedInLatitude: Double? = null
        var checkedInLongitude: Double? = null

        var checkedOutLatitude: Double? = null
        var checkedOutLongitude: Double? = null

    }
}