package ca.exp.soundboard.rewrite.soundboard

import java.awt.Toolkit
import java.awt.event.KeyEvent

object KeyEventIntConverter {

    fun getKeyEventText(keyCode: Int): String {
        if (keyCode in 96..105) {
            val numpad = Toolkit.getProperty("AWT.numpad", "NumPad")
            val c = (keyCode - 96 + 48).toChar()

            return "$numpad $c"
        }

        return KeyEvent.getKeyText(keyCode)
    }
}
