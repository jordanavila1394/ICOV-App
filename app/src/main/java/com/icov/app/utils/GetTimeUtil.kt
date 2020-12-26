package com.icov.app.utils

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and

class GetTimeUtil(listener_: Listener) {

    private val REFERENCE_TIME_OFFSET = 16
    private val ORIGINATE_TIME_OFFSET = 24
    private val RECEIVE_TIME_OFFSET = 32
    private val TRANSMIT_TIME_OFFSET = 40
    private val NTP_PACKET_SIZE = 48

    private val NTP_PORT = 123
    private val NTP_MODE_CLIENT = 3
    private val NTP_VERSION = 3

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    // 70 years plus 17 leap days
    private val OFFSET_1900_TO_1970 = (365L * 70L + 17L) * 24L * 60L * 60L

    // system time computed from NTP server response
    private var mNtpTime: Long = 0

    // value of SystemClock.elapsedRealtime() corresponding to mNtpTime
    private var mNtpTimeReference: Long = 0

    // round trip time in milliseconds
    private var mRoundTripTime: Long = 0

    // callback listener
    private val listener: Listener = listener_

    /**
     * Sends an SNTP request to the given host and processes the response.
     *
     * @param host    host name of the server.
     * @param timeout network timeout in milliseconds.
     * @return true if the transaction was successful.
     */
    fun requestTime(host: String, timeout: Int): Boolean {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeout
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = (NTP_MODE_CLIENT or (NTP_VERSION shl 3)).toByte()

            // get current time and write it to the request packet
            val requestTime = System.currentTimeMillis()
            val requestTicks = SystemClock.elapsedRealtime()
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime)

            socket.send(request)

            // read the response
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val responseTicks = SystemClock.elapsedRealtime()
            val responseTime = requestTime + (responseTicks - requestTicks)

            // extract the results
            val originateTime: Long = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            val receiveTime: Long = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            val transmitTime: Long = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
            val roundTripTime: Long = responseTicks - requestTicks - (transmitTime - receiveTime);
            // receiveTime = originateTime + transit + skew
            // responseTime = transmitTime + transit - skew
            // clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2
            //             = ((originateTime + transit + skew - originateTime) +
            //                (transmitTime - (transmitTime + transit - skew)))/2
            //             = ((transit + skew) + (transmitTime - transmitTime - transit + skew))/2
            //             = (transit + skew - transit + skew)/2
            //             = (2 * skew)/2 = skew
            // receiveTime = originateTime + transit + skew
            // responseTime = transmitTime + transit - skew
            // clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2
            //             = ((originateTime + transit + skew - originateTime) +
            //                (transmitTime - (transmitTime + transit - skew)))/2
            //             = ((transit + skew) + (transmitTime - transmitTime - transit + skew))/2
            //             = (transit + skew - transit + skew)/2
            //             = (2 * skew)/2 = skew
            val clockOffset = (receiveTime - originateTime + (transmitTime - responseTime)) / 2
            // if (false) Log.d(TAG, "round trip: " + roundTripTime + " ms");
            // if (false) Log.d(TAG, "clock offset: " + clockOffset + " ms");

            // save our results - use the times on this side of the network latency
            // (response rather than request time)
            mNtpTime = responseTime + clockOffset
            mNtpTimeReference = responseTicks
            mRoundTripTime = roundTripTime
        } catch (e: Exception) {
            listener?.onError(e)
            return false
        } finally {
            socket?.close()
        }
        return true
    }

    /**
     * Returns the time computed from the NTP transaction.
     *
     * @return time value computed from NTP server response.
     */
    fun getNtpTime(): Long {
        return mNtpTime
    }

    /**
     * Returns the reference clock value (value of SystemClock.elapsedRealtime())
     * corresponding to the NTP time.
     *
     * @return reference clock corresponding to the NTP time.
     */
    fun getNtpTimeReference(): Long {
        return mNtpTimeReference
    }

    /**
     * Returns the round trip time of the NTP transaction
     *
     * @return round trip time in milliseconds.
     */
    fun getRoundTripTime(): Long {
        return mRoundTripTime
    }

    /**
     * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
     */
    private fun read32(buffer: ByteArray, offset: Int): Long {
        val b0: Byte = buffer[offset]
        val b1: Byte = buffer[offset + 1]
        val b2: Byte = buffer[offset + 2]
        val b3: Byte = buffer[offset + 3]

        // convert signed bytes to unsigned values
        val i0 = if (b0 and 0x80.toByte() == 0x80.toByte()) (b0 and 0x7F) + 0x80 else b0.toInt()
        val i1 = if (b1 and 0x80.toByte() == 0x80.toByte()) (b1 and 0x7F) + 0x80 else b1.toInt()
        val i2 = if (b2 and 0x80.toByte() == 0x80.toByte()) (b2 and 0x7F) + 0x80 else b2.toInt()
        val i3 = if (b3 and 0x80.toByte() == 0x80.toByte()) (b3 and 0x7F) + 0x80 else b3.toInt()

        return (i0.toLong() shl 24) + (i1.toLong() shl 16) + (i2.toLong() shl 8) + i3.toLong()
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
        val seconds = read32(buffer, offset)
        val fraction = read32(buffer, offset + 4)
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L)
    }

    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
     * at the given offset in the buffer.
     */
    private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
        var seconds: Long = time / 1000L
        val milliseconds: Long = time - seconds * 1000L
        seconds += OFFSET_1900_TO_1970

        var offsetTemp: Int = offset
        // write seconds in big endian format
        buffer[offsetTemp++] = (seconds shr 24).toByte()
        buffer[offsetTemp++] = (seconds shr 16).toByte()
        buffer[offsetTemp++] = (seconds shr 8).toByte()
        buffer[offsetTemp++] = (seconds shr 0).toByte()

        val fraction = milliseconds * 0x100000000L / 1000L
        // write fraction in big endian format
        buffer[offsetTemp++] = (fraction shr 24).toByte()
        buffer[offsetTemp++] = (fraction shr 16).toByte()
        buffer[offsetTemp++] = (fraction shr 8).toByte()
        // low order bits should be random data
        buffer[offsetTemp++] = (Math.random() * 255.0).toInt().toByte()

    }

    companion object {
        fun getDate(timeListener: Listener) {
            GlobalScope.launch(Dispatchers.IO) {
                val getTimeUtil = GetTimeUtil(listener_ = timeListener)

                if (getTimeUtil.requestTime("time.google.com", 5000)) {
                    val nowAsPerDeviceTimeZone: Long = getTimeUtil.getNtpTime()

                    val timeFormat = SimpleDateFormat("H:mm", Locale.getDefault())
                    val amOrPmFormat = SimpleDateFormat("a", Locale.getDefault())
                    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    val time: String = timeFormat.format(nowAsPerDeviceTimeZone)
                    val amOrPm: String = amOrPmFormat.format(nowAsPerDeviceTimeZone)
                    val dayOfWeek: String = dayOfWeekFormat.format(nowAsPerDeviceTimeZone)
                    val date: String = dateFormat.format(nowAsPerDeviceTimeZone)

                    timeListener.onTimeReceived(time, amOrPm, dayOfWeek, date)
                }
            }
        }
    }

}

interface Listener {
    fun onTimeReceived(time: String?, amOrPm: String?, dayOfWeek: String?, date: String?)
    fun onError(ex: Exception?)
}