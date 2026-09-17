package com.multiroom.data

import com.multiroom.model.Player
import com.multiroom.model.ServerClient
import java.util.UUID

/**
 * Brings players already registered with the server into the app's own records.
 *
 * Only clients the server reports as running on this machine are considered.
 */
object PlayerSync {

    private val InstanceSuffix = Regex("""^(.*)#(\d+)$""")

    /** This machine, as Snapserver reports it. */
    val localHost: String =
        System.getenv("COMPUTERNAME") ?: runCatching {
            java.net.InetAddress.getLocalHost().hostName
        }.getOrDefault("")

    /**
     * @param known players the app already holds
     * @param clients everything the server currently reports
     * @param ignored client ids the user removed, which must not come back
     * @return players to add, empty when there is nothing new
     */
    fun discover(
        known: List<Player>,
        clients: List<ServerClient>,
        ignored: Set<String> = emptySet(),
    ): List<Player> {
        if (localHost.isBlank()) return emptyList()

        val knownNames = known.map { it.name }.toSet()
        val usedInstances = known.map { it.instance }.toMutableSet()

        return clients
            .filter { it.hostName.equals(localHost, ignoreCase = true) }
            .filterNot { it.id in ignored }
            .mapNotNull { client ->
                val (name, instance) = split(client.id)
                if (name in knownNames) return@mapNotNull null
                val free = generateSequence(instance) { it + 1 }.first { it !in usedInstances }
                usedInstances += free
                Player(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    instance = free,
                    // The server does not report which endpoint a client opened.
                    deviceId = "default",
                    autoStart = false,
                )
            }
    }

    /**
     * Recovers a player's name and instance number from a Snapserver client id.
     *
     * @param clientId the id as the server reports it, e.g. "Space One#2"
     * @returns the host id and the instance it was launched with
     */
    fun split(clientId: String): Pair<String, Int> {
        val match = InstanceSuffix.matchEntire(clientId) ?: return clientId to 1
        val name = match.groupValues[1]
        val instance = match.groupValues[2].toIntOrNull() ?: return clientId to 1
        return if (name.isEmpty() || instance < 1) clientId to 1 else name to instance
    }
}
