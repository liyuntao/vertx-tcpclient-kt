package org.example

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser
import io.vertx.core.streams.ReadStream
import org.slf4j.LoggerFactory


class LengthPrefixedStream(private val rawStream: ReadStream<Buffer>) : ReadStream<Buffer> {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private var nextExpectToken = FrameToken.LEN // parser state
    private val recordParser = RecordParser.newFixed(HEAD_LEN, rawStream)

    override fun handler(bizHandler: Handler<Buffer>): ReadStream<Buffer> {
        recordParser.handler { buffer: Buffer ->
            log.debug("received Raw msg size=${buffer.bytes.size}")
            when (nextExpectToken) {
                FrameToken.LEN -> {
                    val bodySize = buffer.getInt(0)
                    log.debug("body size=$bodySize")
                    recordParser.fixedSizeMode(bodySize)
                    nextExpectToken = FrameToken.BODY
                }
                FrameToken.BODY -> {
                    recordParser.fixedSizeMode(HEAD_LEN)
                    nextExpectToken = FrameToken.LEN
                    log.debug("trigger handle => size=${buffer.bytes.size}")
                    bizHandler.handle(buffer)
                }
            }
        }
        return this
    }

    override fun pause(): ReadStream<Buffer> = this.apply { rawStream.pause() }
    override fun resume(): ReadStream<Buffer> = this.apply {  rawStream.resume() }
    override fun fetch(amount: Long): ReadStream<Buffer> = this.apply {  rawStream.fetch(amount) }
    override fun endHandler(endHandler: Handler<Void>?): ReadStream<Buffer> =
        this.apply {  rawStream.endHandler(endHandler) }
    override fun exceptionHandler(handler: Handler<Throwable>): ReadStream<Buffer> =
        this.apply {  rawStream.exceptionHandler(handler) }

    enum class FrameToken {
        LEN, BODY
    }
}