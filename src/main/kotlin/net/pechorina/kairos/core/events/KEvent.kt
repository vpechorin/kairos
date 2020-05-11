package net.pechorina.kairos.core.events

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.types.mapStatusCodeToStatus
import net.pechorina.kairos.core.types.mapStringToContentType
import net.pechorina.kairos.core.utils.CoersionUtils
import org.apache.avro.Schema
import java.time.Instant
import java.util.UUID

open class KEvent(val id: String = UUID.randomUUID().toString(),
                  val timestamp: Long = Instant.now().toEpochMilli(),
                  var path: List<String> = arrayListOf(),
                  var headers: Map<String, String> = hashMapOf(),
                  val body: Buffer) {

    init {
        setContentType(ContentType.BINARY)
    }

    constructor() : this(body = Buffer.buffer()) {
        setContentType(ContentType.BINARY)
    }

    constructor(body: String) : this(body = Buffer.buffer(body)) {
        setContentType(ContentType.TEXT)
    }

    constructor(body: Int) : this(body = Buffer.buffer().appendString(body.toString())) {
        setContentType(ContentType.INT)
    }

    constructor(body: Long) : this(body = Buffer.buffer().appendString(body.toString())) {
        setContentType(ContentType.LONG)
    }

    constructor(body: Boolean) : this(body = Buffer.buffer().appendString(body.toString())) {
        setContentType(ContentType.BOOL)
    }

    constructor(body: JsonObject) : this(body = body.toBuffer()) {
        setContentType(ContentType.JSON)
    }

    constructor(body: JsonArray) : this(body = body.toBuffer()) {
        setContentType(ContentType.JSONA)
    }

    fun getHeader(key: String): String? {
        return headers[key]
    }

    fun addPath(newId: String): KEvent {
        this.path += newId
        return this
    }

    fun extendPath(sourceEvent: KEvent, newId: String): KEvent {
        this.path = sourceEvent.path + newId
        return this
    }

    fun addHeader(key: String, value: String): KEvent {
        this.headers = this.headers + mapOf(key to value)
        return this
    }

    fun addHeaders(headers: Map<String, String>): KEvent {
        this.headers = this.headers + headers
        return this
    }

    fun setReplyTo(sourceEvent: KEvent): KEvent {
        setReplyTo(sourceEvent.id)
        return this
    }

    fun setReplyTo(id: String): KEvent {
        addHeader(REPLY_TO_HEADER, id)
        return this
    }

    @JsonIgnore
    fun getPayloadAsString(): String {
        return asContentType(body, ContentType.TEXT) as String
    }

    @JsonIgnore
    fun getPayloadAsJsonObject(): JsonObject {
        return asContentType(body, ContentType.JSON) as JsonObject
    }

    @JsonIgnore
    fun getPayloadAsJsonArray(): JsonArray {
        return asContentType(body, ContentType.JSONA) as JsonArray
    }

    @JsonIgnore
    fun getPayloadAsBoolean(): Boolean {
        return asContentType(body, ContentType.BOOL) as Boolean
    }

    @JsonIgnore
    fun getPayloadAsInt(): Int {
        return asContentType(body, ContentType.INT) as Int
    }

    @JsonIgnore
    fun getPayloadAsLong(): Long {
        return asContentType(body, ContentType.LONG) as Long
    }

    @JsonIgnore
    fun getPayloadAsBuffer(): Buffer {
        return body
    }

    @JsonIgnore
    fun getPayloadAsBytes(): Array<Byte> {
        return body.bytes.toTypedArray()
    }

    @JsonIgnore
    fun getPayload(): Any {
        return asContentType(body, getContentType());
    }

    @JsonIgnore
    fun setContentType(contentType: ContentType): KEvent {
        if (headers.containsKey(CONTENT_TYPE_HEADER)) {
            headers = headers.filterNot { it.key == CONTENT_TYPE_HEADER } + mapOf(CONTENT_TYPE_HEADER to contentType.mediaType)
        } else {
            addHeader(CONTENT_TYPE_HEADER, contentType.mediaType)
        }
        return this;
    }

    @JsonIgnore
    fun getContentType(): ContentType {
        return mapStringToContentType(getHeader(CONTENT_TYPE_HEADER), ContentType.TEXT)
    }

    fun setStatus(status: Status): KEvent {
        addHeader(STATUS_HEADER, status.statusCode.toString())
        return this
    }

    @JsonIgnore
    fun getStatus(): Status {
        return mapStatusCodeToStatus(CoersionUtils.getInteger(getHeader(STATUS_HEADER)), Status.OK)
    }

    fun setFilePath(path: String): KEvent {
        addHeader(FILE_PATH_HEADER, path)
        return this
    }

    @JsonIgnore
    fun getFilePath(): String? {
        return getHeader(FILE_PATH_HEADER)
    }

    @JsonIgnore
    fun makeChild(id: String): KEvent {
        return KEvent(body = body)
                .setContentType(getContentType())
                .extendPath(this, id)
    }

    override fun toString(): String {
        return "KEvent(id='$id', timestamp=$timestamp, path=$path, headers=$headers, body=$body)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KEvent

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        val REPLY_TO_HEADER = "replyTo"
        val CONTENT_TYPE_HEADER = "contentType"
        val STATUS_HEADER = "status"
        val FILE_PATH_HEADER = "filePath"

        fun asContentType(buffer: Buffer, contentType: ContentType): Any {
            return when (contentType) {
                ContentType.JSON -> JsonObject(buffer)
                ContentType.JSONA -> JsonArray(buffer)
                ContentType.BOOL -> CoersionUtils.getBoolean(buffer.getString(0, buffer.length()), false)
                ContentType.TEXT -> buffer.getString(0, buffer.length())
                ContentType.INT -> CoersionUtils.getInteger(buffer.getString(0, buffer.length()), 0)
                ContentType.LONG -> CoersionUtils.getLong(buffer.getString(0, buffer.length()), 0)
                ContentType.XML -> buffer.getString(0, buffer.length())
                ContentType.XHTML -> buffer.getString(0, buffer.length())
                ContentType.BINARY -> buffer
                else -> buffer
            }
        }

        fun makeKEvent(buffer: Buffer, contentType: ContentType): KEvent {
            var event = when (contentType) {
                ContentType.JSON -> KEvent(JsonObject(buffer))
                ContentType.JSONA -> KEvent(JsonArray(buffer))
                ContentType.BOOL -> KEvent(CoersionUtils.getBoolean(buffer.getString(0, buffer.length()), false))
                ContentType.TEXT -> KEvent(buffer.getString(0, buffer.length()))
                ContentType.INT -> KEvent(CoersionUtils.getInteger(buffer.getString(0, buffer.length()), 0))
                ContentType.LONG -> KEvent(CoersionUtils.getLong(buffer.getString(0, buffer.length()), 0))
                ContentType.XML -> KEvent(buffer.getString(0, buffer.length()))
                ContentType.XHTML -> KEvent(buffer.getString(0, buffer.length()))
                ContentType.BINARY -> KEvent(body = buffer)
                else -> KEvent(body = buffer)
            }

            return event.setContentType(contentType)
        }
    }
}
