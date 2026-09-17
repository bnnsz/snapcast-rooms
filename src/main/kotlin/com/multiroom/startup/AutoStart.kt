package com.multiroom.startup

/**
 * Registers the app to launch at logon.
 *
 * Uses the per-user Run key so no elevation is needed. A Windows service is not
 * an option: services run in session 0, which has no access to audio endpoints,
 * so a snapclient started there would play into nothing.
 */
object AutoStart {
    private const val KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""
    private const val VALUE = "SnapcastRooms"

    fun enable(executablePath: String) {
        runReg("add", KEY, "/v", VALUE, "/t", "REG_SZ", "/d", "\"$executablePath\"", "/f")
    }

    fun disable() {
        runReg("delete", KEY, "/v", VALUE, "/f")
    }

    fun isEnabled(): Boolean =
        runReg("query", KEY, "/v", VALUE) == 0

    private fun runReg(vararg args: String): Int =
        ProcessBuilder(listOf("reg") + args)
            .redirectErrorStream(true)
            .start()
            .waitFor()
}
