package com.multiroom.rpc

import com.multiroom.model.ServerClient
import com.multiroom.model.ServerGroup
import com.multiroom.model.ServerStream
import com.multiroom.model.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger

/** Reads and controls Snapserver over its websocket JSON-RPC endpoint. */
class SnapcastControl(
    private val host: String,
    private val port: Int = 1780,
) : WebSocket.Listener {

    private val json = Json { ignoreUnknownKeys = true }
    private val nextId = AtomicInteger(1)
    private val buffer = StringBuilder()
    private var socket: WebSocket? = null

    private val groupsState = MutableStateFlow<List<ServerGroup>>(emptyList())

    /** Groups currently known to the server. */
    val groups: StateFlow<List<ServerGroup>> = groupsState

    private val clientsState = MutableStateFlow<List<ServerClient>>(emptyList())

    /** Every client, flattened out of [groups]. */
    val clients: StateFlow<List<ServerClient>> = clientsState

    private val streamsState = MutableStateFlow<List<ServerStream>>(emptyList())

    /** Streams the server offers. */
    val streams: StateFlow<List<ServerStream>> = streamsState

    private val connectedState = MutableStateFlow(false)

    /** Whether the websocket is open. */
    val connected: StateFlow<Boolean> = connectedState

    /**
     * Opens the websocket and asks for the full server status.
     *
     * Blocks until the handshake completes.
     *
     * @throws java.util.concurrent.CompletionException when the server is unreachable
     */
    fun connect() {
        socket = HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .buildAsync(URI.create("ws://$host:$port/jsonrpc"), this)
            .join()
        connectedState.value = true
        send("Server.GetStatus")
    }

    fun close() {
        socket?.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        socket = null
        connectedState.value = false
    }

    override fun onClose(ws: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
        socket = null
        connectedState.value = false
        return null
    }

    override fun onError(ws: WebSocket, error: Throwable) {
        socket = null
        connectedState.value = false
    }

    /**
     * Sets a client's volume, applying it to [clients] before sending it.
     *
     * @param percent 0-100
     * @param muted null keeps the current mute state
     */
    fun setVolume(clientId: String, percent: Int, muted: Boolean? = null) {
        val resolved = muted ?: clients.value.firstOrNull { it.id == clientId }?.muted ?: false
        updateClient(clientId) { it.copy(volumePercent = percent, muted = resolved) }
        send("Client.SetVolume", buildJsonObject {
            put("id", clientId)
            putJsonObject("volume") { put("muted", resolved); put("percent", percent) }
        })
    }

    /** Sets a client's display name, which is separate from its id. */
    fun setName(clientId: String, name: String) {
        updateClient(clientId) { it.copy(configuredName = name, displayName = name) }
        send("Client.SetName", buildJsonObject { put("id", clientId); put("name", name) })
    }

    /** Sets a client's delay in milliseconds. */
    fun setLatency(clientId: String, latencyMs: Int) =
        send("Client.SetLatency", buildJsonObject { put("id", clientId); put("latency", latencyMs) })

    /** Replaces a group's members. */
    fun setGroupClients(groupId: String, clientIds: List<String>) =
        send("Group.SetClients", buildJsonObject {
            put("id", groupId)
            put("clients", buildJsonArray { clientIds.forEach { add(it) } })
        })

    /** Adds a client to a group, keeping the members already in it. */
    fun moveClientToGroup(clientId: String, targetGroupId: String) {
        val target = groups.value.firstOrNull { it.id == targetGroupId } ?: return
        val members = target.clients.map { it.id }.toMutableList()
        if (clientId !in members) members += clientId
        setGroupClients(targetGroupId, members)
    }

    /** Points a group at a stream. */
    fun setGroupStream(groupId: String, streamId: String) =
        send("Group.SetStream", buildJsonObject {
            put("id", groupId)
            put("stream_id", streamId)
        })

    /** Mutes or unmutes a whole group. */
    fun setGroupMute(groupId: String, muted: Boolean) {
        updateGroup(groupId) { it.copy(muted = muted) }
        send("Group.SetMute", buildJsonObject {
            put("id", groupId)
            put("mute", muted)
        })
    }

    fun removeClient(clientId: String) =
        send("Server.DeleteClient", buildJsonObject { put("id", clientId) })

    private fun send(method: String, params: JsonObject? = null) {
        val request = buildJsonObject {
            put("id", nextId.getAndIncrement())
            put("jsonrpc", "2.0")
            put("method", method)
            params?.let { put("params", it) }
        }
        socket?.sendText(request.toString(), true)
    }

    override fun onText(ws: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        buffer.append(data)
        if (last) {
            runCatching { handle(json.parseToJsonElement(buffer.toString()).jsonObject) }
            buffer.clear()
        }
        ws.request(1)
        return null
    }

    /** Applies a status reply or a change notification. */
    private fun handle(message: JsonObject) {
        val server = message["result"]?.jsonObject?.get("server")?.jsonObject
            ?: message["params"]?.jsonObject?.get("server")?.jsonObject
        if (server != null) {
            applyServer(server)
            return
        }

        val method = message["method"]?.jsonPrimitive?.contentOrNull ?: return
        val params = message["params"]?.jsonObject ?: return
        val id = params["id"]?.jsonPrimitive?.contentOrNull

        when (method) {
            "Client.OnVolumeChanged" -> {
                val volume = params["volume"]?.jsonObject ?: return
                val percent = volume["percent"]?.jsonPrimitive?.int ?: return
                val muted = volume["muted"]?.jsonPrimitive?.booleanOrNull ?: false
                updateClient(id) { it.copy(volumePercent = percent, muted = muted) }
            }

            "Client.OnNameChanged" -> {
                val name = params["name"]?.jsonPrimitive?.contentOrNull ?: return
                updateClient(id) {
                    it.copy(configuredName = name, displayName = name.ifEmpty { it.id })
                }
            }

            "Group.OnStreamChanged" -> {
                val streamId = params["stream_id"]?.jsonPrimitive?.contentOrNull ?: return
                updateGroup(id) { it.copy(streamId = streamId) }
            }

            "Group.OnMute" -> {
                val muted = params["mute"]?.jsonPrimitive?.booleanOrNull ?: return
                updateGroup(id) { it.copy(muted = muted) }
            }

            "Stream.OnProperties" -> {
                val properties = params["properties"]?.jsonObject ?: return
                updateStream(id) { it.copy(track = trackOf(properties)) }
            }

            "Stream.OnUpdate" -> {
                val stream = params["stream"]?.jsonObject ?: return
                val parsed = parseStream(stream)
                streamsState.value = streamsState.value
                    .filterNot { it.id == parsed.id }
                    .plus(parsed)
                    .sortedBy { it.id }
            }

            else -> send("Server.GetStatus")
        }
    }

    private fun applyServer(server: JsonObject) {
        val parsed = parseGroups(server)
        groupsState.value = parsed
        clientsState.value = parsed.flatMap { it.clients }
        server["streams"]?.let { streamsState.value = parseStreams(it.jsonArray) }
    }

    private fun updateClient(id: String?, transform: (ServerClient) -> ServerClient) {
        if (id == null) return
        val updated = groupsState.value.map { group ->
            group.copy(clients = group.clients.map { if (it.id == id) transform(it) else it })
        }
        groupsState.value = updated
        clientsState.value = updated.flatMap { it.clients }
    }

    private fun updateGroup(id: String?, transform: (ServerGroup) -> ServerGroup) {
        if (id == null) return
        groupsState.value = groupsState.value.map { if (it.id == id) transform(it) else it }
    }

    private fun updateStream(id: String?, transform: (ServerStream) -> ServerStream) {
        if (id == null) return
        streamsState.value = streamsState.value.map { if (it.id == id) transform(it) else it }
    }

    private fun parseGroups(server: JsonObject): List<ServerGroup> =
        server["groups"]?.jsonArray.orEmpty().map { element ->
            val group = element.jsonObject
            val id = group["id"]?.jsonPrimitive?.content.orEmpty()
            ServerGroup(
                id = id,
                name = group["name"]?.jsonPrimitive?.content.orEmpty()
                    .ifEmpty { "Group " + id.take(8) },
                streamId = group["stream_id"]?.jsonPrimitive?.content.orEmpty(),
                muted = group["muted"]?.jsonPrimitive?.booleanOrNull ?: false,
                clients = group["clients"]?.jsonArray.orEmpty().map { clientElement ->
                    val client = clientElement.jsonObject
                    val config = client["config"]!!.jsonObject
                    val volume = config["volume"]!!.jsonObject
                    val clientId = client["id"]!!.jsonPrimitive.content
                    val configured = config["name"]?.jsonPrimitive?.content.orEmpty()
                    ServerClient(
                        id = clientId,
                        displayName = configured.ifEmpty { clientId },
                        configuredName = configured,
                        hostName = client["host"]?.jsonObject
                            ?.get("name")?.jsonPrimitive?.content.orEmpty(),
                        connected = client["connected"]?.jsonPrimitive?.booleanOrNull ?: false,
                        volumePercent = volume["percent"]!!.jsonPrimitive.int,
                        muted = volume["muted"]?.jsonPrimitive?.booleanOrNull ?: false,
                        groupId = id,
                    )
                },
            )
        }

    private fun parseStreams(streams: JsonArray): List<ServerStream> =
        streams.map { parseStream(it.jsonObject) }

    private fun parseStream(stream: JsonObject) = ServerStream(
        id = stream["id"]?.jsonPrimitive?.content.orEmpty(),
        status = stream["status"]?.jsonPrimitive?.content.orEmpty(),
        track = stream["properties"]?.jsonObject?.let(::trackOf),
    )

    private fun trackOf(properties: JsonObject): TrackInfo? =
        properties["metadata"]?.jsonObject?.let { metadata ->
            parseTrack(metadata, properties["position"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
        }

    private fun parseTrack(metadata: JsonObject, positionSeconds: Double): TrackInfo? {
        val title = metadata["title"]?.jsonPrimitive?.content.orEmpty()
        if (title.isEmpty()) return null
        return TrackInfo(
            title = title,
            artist = metadata["artist"]?.jsonArray.orEmpty()
                .mapNotNull { it.jsonPrimitive.contentOrNull }
                .joinToString(", "),
            album = metadata["album"]?.jsonPrimitive?.content.orEmpty(),
            artUrl = metadata["artUrl"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            durationSeconds = metadata["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            positionSeconds = positionSeconds,
        )
    }
}
