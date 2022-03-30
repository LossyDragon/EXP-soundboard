package ca.exp.soundboard.rewrite.soundboard

import java.nio.ByteBuffer
import java.nio.ByteOrder

object SoundUtils {

    fun byteToShortArray(byteArray: ByteArray): ShortArray {
        val shortArray = ShortArray(byteArray.size / 2)

        for (i in shortArray.indices) {
            val ub1: Int = byteArray[i * 2 + 0].toInt() and 0xFF
            val ub2: Int = byteArray[i * 2 + 1].toInt() and 0xFF
            shortArray[i] = ((ub2 shl 8) + ub1).toShort()
        }

        return shortArray
    }

    fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size * 2)

        ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortArray)

        return byteArray
    }
}
