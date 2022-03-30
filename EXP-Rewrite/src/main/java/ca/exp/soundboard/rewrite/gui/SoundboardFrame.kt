package ca.exp.soundboard.rewrite.gui

import ca.exp.soundboard.rewrite.converter.ConverterFrame
import ca.exp.soundboard.rewrite.soundboard.*
import com.apple.eawt.Application
import com.google.gson.Gson
import net.miginfocom.swing.MigLayout
import org.jnativehook.GlobalScreen
import java.awt.Color
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Image
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import java.util.*
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.table.DefaultTableModel
import kotlin.system.exitProcess

class SoundboardFrame : JFrame() {

    private lateinit var thisFrameInstance: SoundboardFrame
    private val autoPptCheckBox: JCheckBox
    private val menuBar: JMenuBar = JMenuBar()
    private val primarySpeakerComboBox: JComboBox<String?>
    private val secondarySpeakerComboBox: JComboBox<String?>
    private val table: JTable
    private val useMicInjectorCheckBox: JCheckBox
    private val useSecondaryCheckBox: JCheckBox
    private var currentSoundboardFile: File? = null

    @JvmField
    var audioManager: AudioManager

    private val selectedEntryIndex: Int
        get() = table.getValueAt(table.selectedRow, 2) as Int

    private val settingsMenu: Unit
        get() = SettingsFrame.instanceOf.setLocationRelativeTo(this)

    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exit()
            }
        })

        try {
            var arrayOfLookAndFeelInfo: Array<UIManager.LookAndFeelInfo>
            val j = (UIManager.getInstalledLookAndFeels().also { arrayOfLookAndFeelInfo = it }).size
            for (i in 0 until j) {
                val info = arrayOfLookAndFeelInfo[i]
                if (("Nimbus" == info.name)) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                    break
                }
            }
        } catch (ex: Exception) {
            when (ex) {
                is IllegalAccessException,
                is InstantiationException,
                is UnsupportedLookAndFeelException,
                is ClassNotFoundException ->
                    Logger.getLogger(SoundboardFrame::class.java.name).log(Level.SEVERE, null, ex)
            }
        }

        audioManager = AudioManager()
        filechooser = JFileChooser()
        soundboard = Soundboard()

        icon = ImageIcon(SoundboardFrame::class.java.classLoader.getResource("EXP logo.png")).image
        iconImage = icon

        title = appTitle
        defaultCloseOperation = 3

        macInit()

        secondarySpeakerComboBox = JComboBox<String?>().apply {
            addItemListener { e ->
                if (e.stateChange == 1) {
                    val name = selectedItem as String
                    audioManager.setSecondaryOutputMixer(name)
                }
            }
        }

        primarySpeakerComboBox = JComboBox<String?>().apply {
            addItemListener { e ->
                if (e.stateChange == 1) {
                    val name = selectedItem as String
                    audioManager.setPrimaryOutputMixer(name)
                }
            }
        }

        useSecondaryCheckBox = JCheckBox("Use").apply {
            addActionListener {
                audioManager.setUseSecondary(this.isSelected)
            }
        }

        useMicInjectorCheckBox = JCheckBox("Use Mic Injector (see Option -> Settings)").apply {
            addActionListener {
                useMicInjector = this.isSelected
                updateMicInjector()
            }
        }

        autoPptCheckBox = JCheckBox("Auto-hold PTT key(s)").apply {
            addActionListener {
                val selected = this.isSelected
                Utils.isAutoPTThold = selected
            }
        }

        table = JTable().apply {
            setSelectionMode(0)
            autoCreateRowSorter = true
            model = object : DefaultTableModel(arrayOf(arrayOfNulls(2)), arrayOf("Sound Clip", "HotKeys")) {
                var columnTypes: Array<Class<*>> = arrayOf(String::class.java, String::class.java)
                var columnEditables: BooleanArray = BooleanArray(2)
                override fun getColumnClass(columnIndex: Int): Class<*> {
                    return columnTypes[columnIndex]
                }

                override fun isCellEditable(row: Int, column: Int): Boolean {
                    return columnEditables[column]
                }
            }
        }

        contentPane.layout = MigLayout(
            "gapx  2:4:5, gapy 2:4:5, fillx",
            "[][][][6px][20px][6px][2px][10px][53px][6px][][24px][2px][43px]",
            "[grow,fill][23px][14px][20px][14px][23px][2px][23px]"
        )

        val btnStop = JButton("Stop All").apply {
            addActionListener {
                Utils.stopAllClips()
            }
        }

        val btnAdd = JButton("Add").apply {
            addActionListener {
                SoundboardEntryEditor(thisFrameInstance)
            }
        }

        val lblstOutputeg = JLabel("1st Output (e.g. your speakers)").apply {
            foreground = Color.DARK_GRAY
        }

        val lblndOutputeg = JLabel("2nd Output (e.g. virtual audio cable \"input\") (optional)").apply {
            foreground = Color.DARK_GRAY
        }

        val scrollPane = JScrollPane().apply {
            setViewportView(table)
        }

        val buttonRemove = JButton("Remove").apply {
            addActionListener {
                val selected = table.selectedRow
                if (selected > -1) {
                    var index = selectedEntryIndex
                    soundboard.removeEntry(index)
                    updateSoundboardTable()
                    if (index >= table.rowCount) {
                        index--
                    }
                    if (index >= 0) {
                        table.setRowSelectionInterval(index, index)
                    }
                }
            }
        }

        val buttonEdit = JButton("Edit").apply {
            addActionListener {
                val selected = table.selectedRow
                if (selected > -1) {
                    val index = selectedEntryIndex
                    println("index $index")
                    val entry = soundboard.getEntry(index)
                    SoundboardEntryEditor(thisFrameInstance, entry!!)
                }
            }
        }

        val buttonPlay = JButton("Play").apply {
            addActionListener {
                val selected = table.selectedRow
                if (selected > -1) {
                    val index = selectedEntryIndex
                    val entry = soundboard.getEntry(index)
                    if (macroListener.isSpeedModKeyHeld) {
                        entry?.play(audioManager, true)
                    } else {
                        entry?.play(audioManager, false)
                    }
                }
            }
        }

        contentPane.run {
            add(JSeparator(), "cell 0 6 14 1,growx,aligny top")
            add(autoPptCheckBox, "cell 8 7 6 1,alignx right,aligny top")
            add(btnAdd, "cell 0 1,alignx left,aligny top")
            add(btnStop, "cell 11 1 3 1,alignx right,aligny top")
            add(buttonEdit, "cell 2 1,alignx left,aligny top")
            add(buttonPlay, "cell 10 1,alignx right,aligny top")
            add(buttonRemove, "cell 1 1,alignx left,aligny top")
            add(lblndOutputeg, "cell 0 4 7 1,alignx left,aligny top")
            add(lblstOutputeg, "cell 0 2 7 1,alignx left,aligny top")
            add(primarySpeakerComboBox, "cell 0 3 14 1,growx,aligny top")
            add(scrollPane, "cell 0 0 14 1,grow")
            add(secondarySpeakerComboBox, "cell 0 5 12 1,growx,aligny center")
            add(useMicInjectorCheckBox, "cell 0 7 7 1,alignx left,aligny top")
            add(useSecondaryCheckBox, "cell 13 5,alignx left,aligny top")
        }

        val mnFile = JMenu("File").apply {
            add(
                JMenuItem("New").apply {
                    addActionListener { fileNew() }
                }
            )
            add(
                JMenuItem("Open").apply {
                    addActionListener { fileOpen() }
                }
            )
            add(JSeparator())
            add(
                JMenuItem("Save").apply {
                    accelerator = KeyStroke.getKeyStroke(83, 2)
                    addActionListener { fileSave() }
                }
            )
            add(
                JMenuItem("Save As...").apply {
                    addActionListener { fileSaveAs() }
                }
            )
            add(JSeparator())
            add(
                JMenuItem("Sourceforge Page").apply {
                    addActionListener {
                        Desktop.getDesktop().browse(URI("https://sourceforge.net/projects/expsoundboard/"))
                    }
                }
            )
            add(JSeparator())
            add(JMenuItem("Quit").apply { addActionListener { exit() } })
        }

        val mnOption = JMenu("Option").apply {
            add(
                JMenuItem("Settings").apply {
                    addActionListener { settingsMenu }
                }
            )
            add(
                JMenuItem("Audio Levels").apply {
                    addActionListener { AudioLevelsFrame.instanceOf.setLocationRelativeTo(thisFrameInstance) }
                }
            )
            add(JSeparator())
            add(
                JMenuItem("Audio Converter").apply {
                    addActionListener {
                        val osName = System.getProperty("os.name")
                        if (!osName.lowercase().contains("mac")) {
                            ConverterFrame()
                        } else {
                            JOptionPane.showMessageDialog(
                                null,
                                "Audio Converter currently not supported on Mac OS X",
                                "Feature not supported",
                                1
                            )
                        }
                    }
                }
            )
        }

        jMenuBar = menuBar
        menuBar.add(mnFile)
        menuBar.add(mnOption)

        minimumSize = Dimension(400, 500)
        updateSpeakerComboBoxes()
        pack()
        thisFrameInstance = this
        macroListener = GlobalKeyMacroListener(this)
        GlobalScreen.addNativeKeyListener(macroListener)
        setLocationRelativeTo(null)
        loadPrefs()
    }

    fun updateSoundboardTable() {
        val entryArray = soundboard.entriesAsObjectArrayForTable
        val tableItems = arrayOf("Sound Clip", "HotKeys", "File Locations", "Index")
        table.model = object : DefaultTableModel(entryArray, tableItems) {
            var columnTypes = arrayOf(String::class.java, String::class.java, String::class.java, Integer.TYPE)
            var columnEditables: BooleanArray = BooleanArray(4)

            override fun getColumnClass(columnIndex: Int): Class<*> {
                return columnTypes[columnIndex]
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return columnEditables[column]
            }
        }

        val columModel = table.columnModel
        columModel.getColumn(3).minWidth = 0
        columModel.getColumn(3).maxWidth = 0
        columModel.getColumn(3).width = 0
        table.removeColumn(columModel.getColumn(2))
    }

    private fun fileNew() {
        Utils.stopAllClips()
        saveReminder()
        currentSoundboardFile = null
        soundboard = Soundboard()
        updateSoundboardTable()
        title = appTitle
    }

    private fun fileOpen() {
        Utils.stopAllClips()
        saveReminder()

        // filechooser.setFileFilter(new JsonFileFilter(null));
        filechooser.fileFilter = JsonFileFilter()

        val session = filechooser.showOpenDialog(null)
        if (session == 0) {
            val jsonfile = filechooser.selectedFile
            open(jsonfile)
        }
    }

    private fun fileSave() {
        if (currentSoundboardFile != null) {
            soundboard.saveAsJsonFile(currentSoundboardFile!!)
        } else {
            fileSaveAs()
        }
    }

    private fun fileSaveAs() {
        val fc = JFileChooser()
        // fc.setFileFilter(new JsonFileFilter(null));
        fc.fileFilter = JsonFileFilter()
        if (currentSoundboardFile != null) {
            fc.selectedFile = currentSoundboardFile
        }
        val session = fc.showSaveDialog(null)
        if (session == 0) {
            val file = fc.selectedFile
            currentSoundboardFile = soundboard.saveAsJsonFile(file)
            title = appTitle + currentSoundboardFile!!.name
        }
    }

    private fun open(jsonfile: File) {
        if (jsonfile.exists()) {
            val sb = Soundboard.loadFromJsonFile(jsonfile)
            soundboard = sb
            updateSoundboardTable()
            currentSoundboardFile = jsonfile
            title = appTitle + currentSoundboardFile!!.name
        }
    }

    private fun updateSpeakerComboBoxes() {
        val outputmixerStringArray = Utils.getMixerNames(audioManager.standardDataLineInfo)
        var arrayOfString1: Array<String?>
        val j = (outputmixerStringArray.also { arrayOfString1 = it }).size
        for (i in 0 until j) {
            val speaker = arrayOfString1[i]
            primarySpeakerComboBox.addItem(speaker)
            secondarySpeakerComboBox.addItem(speaker)
        }
    }

    private fun updateMicInjector() {
        useMicInjectorCheckBox.isSelected = useMicInjector
        if (useMicInjector) {
            Utils.startMicInjector(micInjectorInputMixerName, micInjectorOutputMixerName)
        } else {
            Utils.stopMicInjector()
        }
    }

    private fun savePrefs() {
        Utils.prefs.run {
            put("firstSpeaker", primarySpeakerComboBox.selectedItem as String)
            put("secondSpeaker", secondarySpeakerComboBox.selectedItem as String)
            putBoolean("useSecondSpeaker", useSecondaryCheckBox.isSelected)
            if (currentSoundboardFile != null) {
                put("lastSoundboardUsed", currentSoundboardFile!!.absolutePath)
            }
            put("autoPTTkeys", Utils.pTTkeys.toString())
            put("micInjectorInput", micInjectorInputMixerName)
            put("micInjectorOutput", micInjectorOutputMixerName)
            putBoolean("OverlapClipsWhilePlaying", Utils.isOverlapSameClipWhilePlaying())
            putBoolean("autoPPTenabled", Utils.isAutoPTThold)
            putBoolean("micInjectorEnabled", useMicInjector)
            putFloat("micInjectorOutputGain", Utils.micInjectorGain)
            putFloat("modplaybackspeed", Utils.getModifiedPlaybackSpeed())
            putFloat("primaryOutputGain", AudioManager.getFirstOutputGain())
            putFloat("secondaryOutputGain", AudioManager.getSecondOutputGain())
            putInt("OverlapClipsKey", Utils.overlapSwitchKey)
            putInt("modSpeedDecKey", Utils.modspeeddownKey)
            putInt("modSpeedIncKey", Utils.modspeedupKey)
            putInt("slowSoundKey", Utils.modifiedSpeedKey)
            putInt("stopAllKey", Utils.stopKey)
        }
    }

    private fun loadPrefs() {
        val prefs = Utils.prefs

        val useSecond = prefs.getBoolean("useSecondSpeaker", false)

        useSecondaryCheckBox.isSelected = useSecond
        audioManager.setUseSecondary(useSecond)

        val firstspeaker = prefs["firstSpeaker", null]
        if (firstspeaker != null) {
            primarySpeakerComboBox.selectedItem = firstspeaker
            audioManager.setPrimaryOutputMixer(firstspeaker)
        }

        val secondspeaker = prefs["secondSpeaker", null]
        if (secondspeaker != null) {
            secondarySpeakerComboBox.selectedItem = secondspeaker
            audioManager.setSecondaryOutputMixer(secondspeaker)
        }

        val lastfile = prefs["lastSoundboardUsed", null]
        if (lastfile != null) {
            open(File(lastfile))
        }

        val modSpeed = prefs.getFloat("modplaybackspeed", 0.5f)
        Utils.setModifiedPlaybackSpeed(modSpeed)

        val slowkey = prefs.getInt("slowSoundKey", 35)
        Utils.modifiedSpeedKey = slowkey

        val stopkey = prefs.getInt("stopAllKey", 19)
        Utils.stopKey = stopkey

        val incKey = prefs.getInt("modSpeedIncKey", 39)
        Utils.modspeedupKey = incKey

        val decKey = prefs.getInt("modSpeedDecKey", 37)
        Utils.modspeeddownKey = decKey

        val firstOutputGain = prefs.getFloat("primaryOutputGain", 0.0f)
        val secondOutputGain = prefs.getFloat("secondaryOutputGain", 0.0f)
        val micinjectorOutputGain = prefs.getFloat("micInjectorOutputGain", 0.0f)

        AudioManager.setFirstOutputGain(firstOutputGain)
        AudioManager.setSecondOutputGain(secondOutputGain)

        Utils.micInjectorGain = micinjectorOutputGain

        micInjectorInputMixerName = prefs["micInjectorInput", ""]
        micInjectorOutputMixerName = prefs["micInjectorOutput", ""]
        useMicInjector = prefs.getBoolean("micInjectorEnabled", false)

        updateMicInjector()

        val useautoptt = prefs.getBoolean("autoPPTenabled", false)
        autoPptCheckBox.isSelected = useautoptt

        Utils.isAutoPTThold = useautoptt

        val autopttkeys = prefs["autoPTTkeys", null]
        if (autopttkeys != null) {
            val keys = Utils.stringToIntArrayList(autopttkeys)
            Utils.pTTkeys = keys
        }

        Utils.overlapSameClipWhilePlaying = prefs.getBoolean("OverlapClipsWhilePlaying", true)

        val overlapKey = prefs.getInt("OverlapClipsKey", 36)
        Utils.overlapSwitchKey = overlapKey
    }

    private fun exit() {
        Utils.stopAllClips()

        saveReminder()
        savePrefs()
        dispose()

        Utils.deregisterGlobalKeyLibrary()

        exitProcess(0)
    }

    private fun saveReminder() {
        if (currentSoundboardFile != null) {
            if (currentSoundboardFile!!.exists()) {
                val gson = Gson()
                val savedFile = Soundboard.loadFromJsonFile(currentSoundboardFile!!)
                val savedjson = gson.toJson(savedFile)
                val currentjson = gson.toJson(soundboard)

                if (savedjson != currentjson) {
                    val option = JOptionPane.showConfirmDialog(
                        null,
                        "SoundboardStage has changed. Do you want to save?",
                        "Save Reminder",
                        0
                    )

                    if (option == 0) {
                        soundboard.saveAsJsonFile(currentSoundboardFile!!)
                    }
                }
            }
        } else if (soundboard.soundboardEntries.size > 0) {
            val option = JOptionPane.showConfirmDialog(
                null,
                "SoundboardStage has not been saved. Do you want to save?",
                "Save Reminder",
                0
            )

            if (option == 0) {
                fileSave()
            }
        }
    }

    private fun macInit() {
        if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")) {
            Application.getApplication().apply {
                dockIconImage = icon
            }
            System.setProperty("apple.laf.useScreenMenuBar", "true")
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "EXP SoundboardStage")
        }
    }

    inner class JsonFileFilter : FileFilter() {
        override fun accept(f: File): Boolean {
            return if (f.isDirectory) {
                true
            } else f.name.lowercase(Locale.getDefault()).endsWith(".json")
        }

        override fun getDescription(): String {
            return ".json SoundboardStage save file"
        }
    }

    companion object {
        private const val VERSION = "0.5"

        //        private const val OVERLAPSWITCHKEYKEY = "OverlapClipsKey"
        private const val appTitle = "EXP SoundboardStage vers. $VERSION | "
//        private const val autoPPTKeysKey = "autoPTTkeys"
//        private const val autoPPTenabledKey = "autoPPTenabled"
//        private const val firstSpeakerKey = "firstSpeaker"
//        private const val lastSoundboardFileKey = "lastSoundboardUsed"
//        private const val micInjectorEnabledKey = "micInjectorEnabled"
//        private const val micInjectorInputKey = "micInjectorInput"
//        private const val micInjectorOutputGainKey = "micInjectorOutputGain"
//        private const val micInjectorOutputKey = "micInjectorOutput"
//        private const val modPlaybackSpeedKey = "modplaybackspeed"
//        private const val modPlaybackSpeedKeyKey = "slowSoundKey"
//        private const val modSpeedDecKeyKey = "modSpeedDecKey"
//        private const val modSpeedIncKeyKey = "modSpeedIncKey"
//        private const val overlapClipsKey = "OverlapClipsWhilePlaying"
//        private const val primaryOutputGainKey = "primaryOutputGain"
//        private const val secondSpeakerKey = "secondSpeaker"
//        private const val secondaryOutputGainKey = "secondaryOutputGain"
//        private const val serialVersionUID = 8934802095461138592L
//        private const val stopallKeyKey = "stopAllKey"
//        private const val updateCheckKey = "updateCheckOnLaunch"
//        private const val useSecondaryKey = "useSecondSpeaker"

        lateinit var icon: Image

        lateinit var soundboard: Soundboard

        lateinit var macroListener: GlobalKeyMacroListener

        lateinit var filechooser: JFileChooser

        @JvmField
        var micInjectorInputMixerName = ""

        @JvmField
        var micInjectorOutputMixerName = ""

        @JvmField
        var useMicInjector = false

        @JvmStatic
        fun main(args: Array<String>) {
            // https://stackoverflow.com/a/30562956/13225929
            LogManager.getLogManager().reset()
            val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
            logger.level = Level.OFF

            Utils.initGlobalKeyLibrary()
            // Utils.startMp3Decoder();

            SoundboardFrame().isVisible = true
        }
    }
}
