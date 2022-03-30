package ca.exp.soundboard.rewrite.soundboard

import ca.exp.soundboard.rewrite.gui.SoundboardFrame
import ca.exp.soundboard.rewrite.soundboard.Utils.decrementModSpeedDown
import ca.exp.soundboard.rewrite.soundboard.Utils.incrementModSpeedUp
import ca.exp.soundboard.rewrite.soundboard.Utils.isOverlapSameClipWhilePlaying
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener

class GlobalKeyMacroListener(
    private var soundboardFrame: SoundboardFrame
) : NativeKeyListener {

    private var pressedKeys: ArrayList<Int> = ArrayList()

    val isSpeedModKeyHeld: Boolean
        get() {
            val localIterator: Iterator<*> = pressedKeys.iterator()
            while (localIterator.hasNext()) {
                val key = (localIterator.next() as Int).toInt()
                if (key == Utils.modifiedSpeedKey) {
                    return true
                }
            }
            return false
        }

    val pressedNativeKeys: ArrayList<Int>
        get() {
            val array: ArrayList<Int> = arrayListOf()
            for (i in pressedKeys) {
                array.add(i)
            }

            return array
        }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        val pressed = e.keyCode

        Utils.submitNativeKeyPressTime(NativeKeyEvent.getKeyText(pressed), e.getWhen())
        var alreadyPressed = false
        val localIterator: Iterator<*> = pressedKeys.iterator()

        while (localIterator.hasNext()) {
            val i = (localIterator.next() as Int).toInt()
            if (pressed == i) {
                alreadyPressed = true
                break
            }
        }

        if (!alreadyPressed) {
            pressedKeys.add(Integer.valueOf(pressed))
        }

        when (pressed) {
            Utils.stopKey -> Utils.stopAllClips()
            Utils.modspeedupKey -> incrementModSpeedUp()
            Utils.modspeeddownKey -> decrementModSpeedDown()
            Utils.overlapSwitchKey -> {
                val overlap = isOverlapSameClipWhilePlaying()
                Utils.overlapSameClipWhilePlaying = !overlap
            }
        }

        checkMacros()
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        val released = e.keyCode
        for (i in pressedKeys.indices) {
            if (released == pressedKeys[i]) {
                pressedKeys.removeAt(i)
            }
        }
    }

    override fun nativeKeyTyped(arg0: NativeKeyEvent) {}

    private fun checkMacros() {
        var modspeed = false

        if (isSpeedModKeyHeld) {
            modspeed = true
        }

        val potential: ArrayList<SoundboardEntry?> = arrayListOf()

        for (entry in SoundboardFrame.soundboard.soundboardEntries) {
            val actKeys = entry.activationKeys
            if (actKeys!!.isNotEmpty() && entry.matchesPressed(pressedKeys)) {
                potential.add(entry)
            }
        }

        if (potential.size == 1) {
            potential[0]!!.play(soundboardFrame.audioManager, modspeed)
        } else {
            var highest = 0
            var potentialCopy = ArrayList(potential)

            for (p in potentialCopy) {
                val matches = p!!.matchesHowManyPressed(pressedKeys)

                if (matches > highest) {
                    highest = matches
                } else if (matches < highest) {
                    potential.remove(p)
                }
            }

            potentialCopy = ArrayList(potential)

            for (p in potentialCopy) {
                val matches = p!!.matchesHowManyPressed(pressedKeys)

                if (matches < highest) {
                    potential.remove(p)
                }
            }

            for (p in potential) {
                p!!.play(soundboardFrame.audioManager, modspeed)
            }
        }
    }
}
