package net.pechorina.kairos.core.events

import org.apache.avro.Schema

object KEventSchemaProvider {
    var _schema: Schema? = null

    val schema: Schema
        get() {
            if (_schema == null) {
                val json = this.javaClass.getResource("/schemas/kevent.avsc").readText()
                this._schema = Schema.Parser().parse(json)
            }
            return _schema ?: throw AssertionError("Set to null by another thread")
        }
}