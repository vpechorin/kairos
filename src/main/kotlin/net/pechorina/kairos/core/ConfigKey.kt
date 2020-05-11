package net.pechorina.kairos.core

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class ConfigKey(@field:Transient @get:JsonValue
                     val key: String,
                     @field:Transient val type: ConfigType,
                     @field:Transient private val validationExpression: String,
                     @field:Transient private val description: String) {

    Http("http", ConfigType.STRING, "", "Http section"),
    HttpClient("httpclient", ConfigType.STRING, "", "Http Client section"),
    WebClientOptions("webclientoptions", ConfigType.STRING, "", "WebClientOptions"),
    WebClientBasicAuth("basicauth", ConfigType.STRING, "", "WebClient basic authentication"),
    Username("username", ConfigType.STRING, "", "Authentication: username"),
    Password("password", ConfigType.STRING, "", "Authentication: password"),
    Token("token", ConfigType.STRING, "", "Authentication: token"),
    WebClientJWTAuth("jwt", ConfigType.STRING, "", "WebClient JWT authentication"),
    Host("host", ConfigType.STRING, "", "Host"),
    Port("port", ConfigType.INT, "", "Http server port"),
    AuthEnabled("auth", ConfigType.BOOL, "", "Authentication switch for http"),

    Routes("routes", ConfigType.LIST, "", "Http server routes section"),
    States("states", ConfigType.LIST, "", "State machine states section"),
    Events("events", ConfigType.LIST, "", "State machine events section"),
    Transitions("transitions", ConfigType.LIST, "", "State machine transitions section"),

    Timer("timer", ConfigType.LIST, "", "Timer section"),

    ScriptContent("script.content", ConfigType.TEXT, "", "Script content"),
    ClassName("className", ConfigType.STRING, "", "Class name including the package name"),
    ResponseSchema("responseSchema", ConfigType.STRING, "", "Response schema name"),
    ResponseClassName("responseClassName", ConfigType.STRING, "", "Response class name"),
    Validate("validate", ConfigType.BOOL, "", "Switch validator on/off"),

    FS("fs", ConfigType.STRING, "", "FS section"),
    Path("path", ConfigType.STRING, "", "File directory"),
    Extension("ext", ConfigType.STRING, "", "File extension"),
    Serializer("serializer", ConfigType.STRING, "", "Serializer class name"),
    Deserializer("deserializer", ConfigType.STRING, "", "Deserializer class name"),

    HttpMethod("method", ConfigType.STRING, "", "Http Method"),
    ContentType("contentType", ConfigType.STRING, "", "Content-Type"),
    AcceptType("acceptType", ConfigType.STRING, "", "Accept-Type"),
    InputType("inputType", ConfigType.STRING, "", "InputType"),
    OutputType("outputType", ConfigType.STRING, "", "OutputType (ContentType)"),

    Interval("interval", ConfigType.LONG, "", "Interval for periodic events, Ms"),

    Shell("sh", ConfigType.STRING, "", "Shell section"),
    Execute("execute", ConfigType.STRING, "", "Shell to run - sh, bash, cmd.exe"),
    Cmd("cmd", ConfigType.STRING, "", "Command to run within shell"),
    WorkDir("workdir", ConfigType.STRING, "", "Working directory"),
    PoolSize("poolsize", ConfigType.INT, "", "Thread pool size"),

    Groovy("groovy", ConfigType.STRING, "", "Groovy section"),

    Timeout("timeout", ConfigType.LONG, "", "Timeout, Ms"),

    LocalMap("localdata", ConfigType.STRING, "", "LocalMap section"),
    LocalMapBlock("localdata.block", ConfigType.STRING, "", "LocalMap block name"),
    StoreName("store", ConfigType.STRING, "", "Store Name"),

    AuthN("authN", ConfigType.STRING, "", "Authentication section"),
    JWTAuthServiceAddress("jwtAuthServiceAddress", ConfigType.STRING, "", "Forward JWT auth requests to address"),
    UsernamePasswordAuthServiceAddress("usernamePasswordServiceAddress", ConfigType.STRING, "", "Forward Username/Password auth requests to address"),
    Authorities("authorities", ConfigType.STRING, "", "Authorities section"),
    Credentials("credentials", ConfigType.STRING, "", "User credentials section"),
    UserAuthorities("user.authorities", ConfigType.STRING, "", "User authorities section"),

    RequestMapperEx("requestMapperEx", ConfigType.STRING, "", "Groovy expression to map request to target"),
    RequestMapperOutputType("requestMapperOutputType", ConfigType.STRING, "", "Request mapper OutputType (ContentType)"),
    ResponseMapperEx("responseMapperEx", ConfigType.STRING, "", "Groovy expression to map response to target"),
    Expression("expression", ConfigType.STRING, "", "Expression to map input to output"),
    Transformation("transformation", ConfigType.STRING, "", "Transformation to map input to string key"),
    KeyTransformation("keyTransformation", ConfigType.STRING, "", "Key transformation to map input to string key"),
    ValueTransformation("valueTransformation", ConfigType.STRING, "", "Value transformation to map input to string value"),

    RequestLane("requestLane", ConfigType.STRING, "", "Output lane"),
    ResponseLane("responseLane", ConfigType.STRING, "", "Input lane"),
    SuccessLane("successLane", ConfigType.STRING, "", "Output lane"),
    FailureLane("failureLane", ConfigType.STRING, "", "Output lane"),
    Error401Unauthorized("error401Lane", ConfigType.STRING, "", "Output error 401 lane"),
    Error403Forbidden("error403Lane", ConfigType.STRING, "", "Output error 403 lane"),
    Error404NotFound("error404Lane", ConfigType.STRING, "", "Output error 404 lane"),
    Error400Lane("error400Lane", ConfigType.STRING, "", "Output error 40x lane"),
    Error500Lane("error500Lane", ConfigType.STRING, "", "Output error 50x lane"),
    StoreLane("storeLane", ConfigType.STRING, "", "IO lane to connecto to Store"),

    StartCount("startCount", ConfigType.LONG, "", "Initial value"),
    Increment("increment", ConfigType.LONG, "", "Increment value"),

    ConnectTimeout("connectTimeout", ConfigType.INT, "", "WebClientOptions: connectTimeout"),
    DecoderInitialBufferSize("decoderInitialBufferSize", ConfigType.INT, "", "WebClientOptions: decoderInitialBufferSize"),
    DefaultHost("defaultHost", ConfigType.STRING, "", "WebClientOptions: defaultHost"),
    DefaultPort("defaultPort", ConfigType.INT, "", "WebClientOptions: defaultPort"),
    EnabledCipherSuites("enabledCipherSuites", ConfigType.STRING, "", "WebClientOptions: enabledCipherSuites"),
    EnabledSecureTransportProtocols("enabledSecureTransportProtocols", ConfigType.STRING, "", "WebClientOptions: enabledSecureTransportProtocols"),
    FollowRedirects("followRedirects", ConfigType.BOOL, "", "WebClientOptions: followRedirects"),
    ForceSni("forceSni", ConfigType.BOOL, "", "WebClientOptions: forceSni"),
    Http2ClearTextUpgrade("http2ClearTextUpgrade", ConfigType.BOOL, "", "WebClientOptions: http2ClearTextUpgrade"),
    Http2ConnectionWindowSize("http2ConnectionWindowSize", ConfigType.INT, "", "WebClientOptions: http2ConnectionWindowSize"),
    Http2KeepAliveTimeout("http2KeepAliveTimeout", ConfigType.INT, "", "WebClientOptions: http2KeepAliveTimeout"),
    Http2MaxPoolSize("http2MaxPoolSize", ConfigType.INT, "", "WebClientOptions: http2MaxPoolSize"),
    Http2MultiplexingLimit("http2MultiplexingLimit", ConfigType.INT, "", "WebClientOptions: http2MultiplexingLimit"),
    IdleTimeout("idleTimeout", ConfigType.INT, "", "WebClientOptions: idleTimeout"),
    IdleTimeoutUnit("idleTimeoutUnit", ConfigType.LONG, "", "WebClientOptions: idleTimeoutUnit"),
    KeepAlive("keepAlive", ConfigType.BOOL, "", "WebClientOptions: keepAlive"),
    KeepAliveTimeout("keepAliveTimeout", ConfigType.INT, "", "WebClientOptions: keepAliveTimeout"),
    KeyStoreOptions("keyStoreOptions", ConfigType.STRING, "", "WebClientOptions: keyStoreOptions"),
    LocalAddress("localAddress", ConfigType.STRING, "", "WebClientOptions: localAddress"),
    LogActivity("logActivity", ConfigType.BOOL, "", "WebClientOptions: logActivity"),
    MaxChunkSize("maxChunkSize", ConfigType.INT, "", "WebClientOptions: maxChunkSize"),
    MaxHeaderSize("maxHeaderSize", ConfigType.INT, "", "WebClientOptions: maxHeaderSize"),
    MaxInitialLineLength("maxInitialLineLength", ConfigType.INT, "", "WebClientOptions: maxInitialLineLength"),
    MaxPoolSize("maxPoolSize", ConfigType.INT, "", "WebClientOptions: maxPoolSize"),
    MaxRedirects("maxRedirects", ConfigType.INT, "", "WebClientOptions: maxRedirects"),
    MaxWaitQueueSize("maxWaitQueueSize", ConfigType.INT, "", "WebClientOptions: maxWaitQueueSize"),
    MaxWebsocketFrameSize("maxWebsocketFrameSize", ConfigType.INT, "", "WebClientOptions: maxWebsocketFrameSize"),
    MaxWebsocketMessageSize("maxWebsocketMessageSize", ConfigType.INT, "", "WebClientOptions: maxWebsocketMessageSize"),
    MetricsName("metricsName", ConfigType.STRING, "", "WebClientOptions: metricsName"),
    OpenSslEngineOptions("openSslEngineOptions", ConfigType.STRING, "", "WebClientOptions: openSslEngineOptions"),
    PemKeyCertOptions("pemKeyCertOptions", ConfigType.STRING, "", "WebClientOptions: pemKeyCertOptions"),
    PemTrustOptions("pemTrustOptions", ConfigType.STRING, "", "WebClientOptions: pemTrustOptions"),
    PfxKeyCertOptions("pfxKeyCertOptions", ConfigType.STRING, "", "WebClientOptions: pfxKeyCertOptions"),
    PfxTrustOptions("pfxTrustOptions", ConfigType.STRING, "", "WebClientOptions: pfxTrustOptions"),
    Pipelining("pipelining", ConfigType.BOOL, "", "WebClientOptions: pipelining"),
    PipeliningLimit("pipeliningLimit", ConfigType.INT, "", "WebClientOptions: pipeliningLimit"),
    PoolCleanerPeriod("poolCleanerPeriod", ConfigType.INT, "", "WebClientOptions: poolCleanerPeriod"),
    ProtocolVersion("protocolVersion", ConfigType.STRING, "", "WebClientOptions: protocolVersion"),
    ProxyOptions("proxyOptions", ConfigType.STRING, "", "WebClientOptions: proxyOptions"),
    ReceiveBufferSize("receiveBufferSize", ConfigType.INT, "", "WebClientOptions: receiveBufferSize"),
    ReuseAddress("reuseAddress", ConfigType.BOOL, "", "WebClientOptions: reuseAddress"),
    ReusePort("reusePort", ConfigType.BOOL, "", "WebClientOptions: reusePort"),
    SendBufferSize("sendBufferSize", ConfigType.INT, "", "WebClientOptions: sendBufferSize"),
    SendUnmaskedFrames("sendUnmaskedFrames", ConfigType.BOOL, "", "WebClientOptions: sendUnmaskedFrames"),
    SoLinger("soLinger", ConfigType.INT, "", "WebClientOptions: soLinger"),
    Ssl("ssl", ConfigType.BOOL, "", "WebClientOptions: ssl"),
    TcpCork("tcpCork", ConfigType.BOOL, "", "WebClientOptions: tcpCork"),
    TcpFastOpen("tcpFastOpen", ConfigType.BOOL, "", "WebClientOptions: tcpFastOpen"),
    TcpKeepAlive("tcpKeepAlive", ConfigType.BOOL, "", "WebClientOptions: tcpKeepAlive"),
    TcpNoDelay("tcpNoDelay", ConfigType.BOOL, "", "WebClientOptions: tcpNoDelay"),
    TcpQuickAck("tcpQuickAck", ConfigType.BOOL, "", "WebClientOptions: tcpQuickAck"),
    TrafficClass("trafficClass", ConfigType.INT, "", "WebClientOptions: trafficClass"),
    TrustAll("trustAll", ConfigType.BOOL, "", "WebClientOptions: trustAll"),
    TrustStoreOptions("trustStoreOptions", ConfigType.STRING, "", "WebClientOptions: trustStoreOptions"),
    TryUseCompression("tryUseCompression", ConfigType.BOOL, "", "WebClientOptions: tryUseCompression"),
    TryUsePerFrameWebsocketCompression("tryUsePerFrameWebsocketCompression", ConfigType.BOOL, "", "WebClientOptions: tryUsePerFrameWebsocketCompression"),
    TryUsePerMessageWebsocketCompression("tryUsePerMessageWebsocketCompression", ConfigType.BOOL, "", "WebClientOptions: tryUsePerMessageWebsocketCompression"),
    UseAlpn("useAlpn", ConfigType.BOOL, "", "WebClientOptions: useAlpn"),
    UsePooledBuffers("usePooledBuffers", ConfigType.BOOL, "", "WebClientOptions: usePooledBuffers"),
    UserAgent("userAgent", ConfigType.STRING, "", "WebClientOptions: userAgent"),
    UserAgentEnabled("userAgentEnabled", ConfigType.BOOL, "", "WebClientOptions: userAgentEnabled"),
    VerifyHost("verifyHost", ConfigType.BOOL, "", "WebClientOptions: verifyHost"),
    WebsocketCompressionAllowClientNoContext("websocketCompressionAllowClientNoContext", ConfigType.BOOL, "", "WebClientOptions: websocketCompressionAllowClientNoContext"),
    WebsocketCompressionLevel("websocketCompressionLevel", ConfigType.INT, "", "WebClientOptions: websocketCompressionLevel"),
    WebsocketCompressionRequestServerNoContext("websocketCompressionRequestServerNoContext", ConfigType.BOOL, "", "WebClientOptions: websocketCompressionRequestServerNoContext");

    companion object {
        @JsonCreator
        fun fromKey(key: String): ConfigKey? {
            return ConfigKey.values()
                    .filter { v -> v.key.equals(key, ignoreCase = true) }
                    .first()
        }
    }
}
