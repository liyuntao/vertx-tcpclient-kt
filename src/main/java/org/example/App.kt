package org.example

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quote.model.QuotePush
import java.nio.ByteBuffer
import java.nio.ByteOrder


const val targetIp = "127.0.0.1"
const val targetPort = 17996
const val loginUser = "mock_test_user"
const val loginPwd = "mock_test_pwd"
const val HEAD_LEN = 4

fun main() {
    val vertxOptions = VertxOptions()
        .setPreferNativeTransport(true)
    val vertx = Vertx.vertx(vertxOptions)
    vertx.deployVerticle(TcpClient())
}

class TcpClient : AbstractVerticle() {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    override fun start() {
        log.info("TCP client started")
        vertx.createNetClient()
            .connect(targetPort, targetIp) { netSocketAsyncRes ->
                if (netSocketAsyncRes.failed()) {
                    log.error("tcp not establish. Errrr")
                }
                val socket = netSocketAsyncRes.result()
                val wrappedSocket = LengthPrefixedStream(socket)
                wrappedSocket.handler(IngressMsgConsumer())
                log.info("tcp handler registered!")

                val qotClient = SimpleQuoteClient(socket)
                qotClient.doLogin()

                socket.closeHandler { id ->
                    log.info("The socket has been closed for: {}", id)
                }
            }
    }
}

class IngressMsgConsumer : Handler<Buffer> {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    override fun handle(event: Buffer) {
        log.info("received bizMsg after parsing")
        val pbMsg = BusinessProtoSerde.decode(event)
        log.info("{}", pbMsg)
    }
}

class SimpleQuoteClient(private val tcpSo: NetSocket) {
    fun doLogin() {
        val logon = QuotePush.Data
            .newBuilder()
            .setDataType(QuotePush.QuoteType.QUOTE_TYPE_PUSH_LOGIN_REQ_VALUE)
            .setLoginReq(
                QuotePush.LoginReq.newBuilder()
                    .setUser(loginUser)
                    .setPassword(loginPwd)
                    .build()
            )
            .build()
        encodeThenSend(logon)
    }

    private fun encodeThenSend(pushData: QuotePush.Data) {
        val byteBuffer = BusinessProtoSerde.encode(pushData)
        tcpSo.write(Buffer.buffer(byteBuffer.array())) {
            println("after write. isSucc=" + it.succeeded())
        }
    }
}

object BusinessProtoSerde {
    fun encode(pushData: QuotePush.Data): ByteBuffer {
        val pbMsgBytes = pushData.toByteArray()
        return ByteBuffer.allocate(HEAD_LEN + pbMsgBytes.size).apply {
            this.order(ByteOrder.BIG_ENDIAN)
            this.putInt(pbMsgBytes.size)
            this.put(pbMsgBytes)
        }
    }

    fun decode(buffer: Buffer): QuotePush.Data = QuotePush.Data.parseFrom(buffer.bytes)
}
