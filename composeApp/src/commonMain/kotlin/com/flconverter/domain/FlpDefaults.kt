package com.flconverter.domain

internal object FlpDefaults {
    const val PPQ = 96
    private const val SAMPLER_CHANNEL = 0
    private const val PATTERN_COLOR = 5656904L
    private const val UNSET = 0xFFFFFFFFL

    val NOTE = hexBytes("00000000 0040 0300 30000000 5600 0000 7800 4000 40 64 80 80")

    val ITEM = hexBytes(
        "00000000 0050 0150 00480000 F001 0000 7800 4000 40648080 00000000 00480000 01000000 " +
            "00000000 00000000 00000000 00000000 0000803F 00000000"
    )

    private val HEADER = listOf(
        199 to "32342E322E322E3435393700",
        159 to "F5110000",
        169 to "05000000",
        28 to "01",
        37 to "01",
        200 to "20002000200020000000",
        156 to "C0D40100",
        67 to "0200",
        9 to "01",
        11 to "00",
        80 to "0000",
        17 to "04",
        18 to "04",
        35 to "01",
        23 to "00",
        44 to "01",
        30 to "01",
        10 to "00",
        194 to "0000",
        206 to "0000",
        207 to "0000",
        167 to "00000000",
        202 to "0000",
        195 to "0000",
        237 to "1D9E10CD7A56E6400000C8778E8CB93F",
        231 to "41007500640069006F000000",
        231 to "55006E0073006F0072007400650064000000",
        146 to "FFFFFFFF",
        216 to ""
    )

    fun baseEvents(channelNames: List<String>): List<FlpEvent> {
        val events = ArrayList<FlpEvent>()
        HEADER.forEach { (id, hex) -> events.add(FlpEvent(id, hexBytes(hex))) }

        events.add(FlpEvent.word(FlpSongReader.NEW_PATTERN, 1))
        events.add(FlpEvent(FlpSongReader.NOTES, NOTE))

        channelNames.forEachIndexed { index, name ->
            events.add(FlpEvent.word(FlpSongReader.NEW_CHANNEL, index))
            events.add(FlpEvent(21, byteArrayOf(SAMPLER_CHANNEL.toByte())))
            events.add(FlpEvent.text(FlpSongReader.CHANNEL_NAME, name))
        }

        events.add(FlpEvent.word(FlpSongReader.NEW_PATTERN, 1))
        events.add(FlpEvent.text(193, "Pattern 1"))
        events.add(FlpEvent.dword(150, PATTERN_COLOR))
        events.add(FlpEvent.dword(157, UNSET))
        events.add(FlpEvent.dword(158, UNSET))
        events.add(FlpEvent.dword(164, 0L))

        events.add(FlpEvent(FlpSongReader.PLAYLIST, ITEM))
        return events
    }
}
