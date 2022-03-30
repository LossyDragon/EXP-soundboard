package ca.exp.soundboard.rewrite.gui

import ca.exp.soundboard.rewrite.soundboard.AudioManager
import ca.exp.soundboard.rewrite.soundboard.Utils
import java.awt.Color
import javax.swing.*

class AudioLevelsFrame : JFrame() {

    private val primarySlider: JSlider
    private val secondarySlider: JSlider
    private val micinjectorSlider: JSlider

    init {
        title = "Audio Gain Controls"
        isResizable = false
        defaultCloseOperation = 2
        iconImage = SoundboardFrame.icon

        val jSeparator = JSeparator().apply { foreground = Color.BLACK }
        val lblMicInjectorGain = JLabel("Mic Injector Gain:")
        val lblPrimaryOutputGain = JLabel("Primary Output Gain:")
        val lblSecondaryOutputGain = JLabel("Secondary Output Gain:")
        val separator = JSeparator().apply { foreground = Color.BLACK }

        val micInjectorGain = Utils.micInjectorGain.toInt()
        val primaryGain = AudioManager.getFirstOutputGain().toInt()
        val secondaryGain = AudioManager.getSecondOutputGain().toInt()

        primarySlider = JSlider().apply {
            majorTickSpacing = 6
            maximum = 6
            minimum = -66
            minorTickSpacing = 1
            paintLabels = true
            paintTicks = true
            snapToTicks = true
            value = 0
            addChangeListener {
                if (!valueIsAdjusting) {
                    val gain = value.toFloat()
                    AudioManager.setFirstOutputGain(gain)
                }
            }
        }

        secondarySlider = JSlider().apply {
            majorTickSpacing = 6
            maximum = 6
            minimum = -66
            minorTickSpacing = 1
            paintLabels = true
            paintTicks = true
            snapToTicks = true
            value = 0
            addChangeListener {
                if (!valueIsAdjusting) {
                    val gain = value.toFloat()
                    AudioManager.setSecondOutputGain(gain)
                }
            }
        }

        micinjectorSlider = JSlider().apply {
            majorTickSpacing = 6
            maximum = 6
            minimum = -66
            minorTickSpacing = 1
            paintLabels = true
            paintTicks = true
            snapToTicks = true
            value = 0
            addChangeListener {
                if (!valueIsAdjusting) {
                    val gain = value.toFloat()
                    Utils.micInjectorGain = gain
                }
            }
        }

        val groupLayout = GroupLayout(contentPane)
        groupLayout.setHorizontalGroup(
            groupLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    groupLayout
                        .createSequentialGroup()
                        .addGroup(
                            groupLayout
                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    groupLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                            groupLayout
                                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addComponent(separator, -1, 424, 32767)
                                                .addComponent(primarySlider, -1, 424, 32767)
                                                .addComponent(lblPrimaryOutputGain)
                                                .addComponent(lblSecondaryOutputGain)
                                                .addComponent(secondarySlider, -2, 424, -2)
                                        )
                                )
                                .addGroup(
                                    groupLayout
                                        .createSequentialGroup()
                                        .addGap(11)
                                        .addComponent(jSeparator, -1, 423, 32767)
                                )
                                .addGroup(
                                    groupLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(lblMicInjectorGain)
                                )
                                .addGroup(
                                    groupLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(micinjectorSlider, -2, 424, -2)
                                )
                        )
                        .addContainerGap()
                )
        )
        groupLayout.setVerticalGroup(
            groupLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblPrimaryOutputGain)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(primarySlider, -2, -1, -2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(separator, -2, 2, -2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblSecondaryOutputGain)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(secondarySlider, -2, 45, -2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator, -2, 2, -2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMicInjectorGain)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(micinjectorSlider, -2, 45, -2)
                        .addContainerGap(38, 32767)
                )
        )

        contentPane.layout = groupLayout
        primarySlider.value = primaryGain
        secondarySlider.value = secondaryGain
        micinjectorSlider.value = micInjectorGain

        pack()
        isVisible = true
    }

    override fun dispose() {
        super.dispose()
        instance = null
    }

    companion object {
        private const val serialVersionUID = 464347549019590824L

        private var instance: AudioLevelsFrame? = null

        val instanceOf: AudioLevelsFrame
            get() {
                if (instance == null) {
                    instance = AudioLevelsFrame()
                } else {
                    instance!!.isVisible = true
                    instance!!.requestFocus()
                }
                return instance!!
            }
    }
}
