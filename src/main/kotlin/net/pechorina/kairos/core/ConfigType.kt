package net.pechorina.kairos.core

enum class ConfigType(@field:Transient val default: Any) {
    BOOL(java.lang.Boolean.FALSE),
    INT(0),
    LONG(0L),
    NUMBER(0),
    STRING(""),
    DATE(""),
    LIST(emptyList<Any>()),
    MAP(emptyMap<Any, Any>()),
    OBJECT(Object()),
    CHAR(' '),
    TEXT("")
}
