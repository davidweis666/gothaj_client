package systems.vajzii.irc.handler;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class IrcClientHandler extends ChannelDuplexHandler {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private final IrcClient client;

    public IrcClientHandler(WebSocketClientHandshaker handshaker, IrcClient client) {
        this.handshaker = handshaker;
        this.client = client;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Netty Channel Inactive");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (!handshaker.isHandshakeComplete()) {
            try {
                if (msg instanceof FullHttpResponse) {
                    handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                    System.out.println("IRC Connection established!");
                    handshakeFuture.setSuccess();

                    client.setConnected(true);

                }
            } catch (WebSocketHandshakeException e) {
                System.out.println("IRC Connection invalidated!");
                handshakeFuture.setFailure(e);
            } finally {
                ReferenceCountUtil.release(msg);
            }
            return;
        }

        if (msg instanceof FullHttpResponse response) {
            try {
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse (status=" + response.getStatus() +
                                ", content=" + response.content().toString(io.netty.util.CharsetUtil.UTF_8) + ")");
            } finally {
                response.release();
            }
        }

        if (msg instanceof WebSocketFrame frame) {
            try {
                if (frame instanceof BinaryWebSocketFrame) {
                    ByteBuf content = frame.content();

                    byte[] rawData = new byte[content.readableBytes()];
                    content.readBytes(rawData);

                    byte[] packetData = client.getEncryption().decrypt(rawData);

                    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(packetData))) {
                        int id = in.read();
                        Class<? extends IrcPacket> clazz = client.getPacketManager().getPacketByID(id);
                        if (clazz != null) {
                            IrcPacket packet = clazz.newInstance();
                            packet.read(in);
                            packet.handle(client);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                } else if (frame instanceof TextWebSocketFrame) {
                } else if (frame instanceof CloseWebSocketFrame) {
                    ch.close();
                }
            } finally {
                frame.release();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof IrcPacket packet) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.write(client.getPacketManager().getIDbyPacket(packet));
                packet.write(out);
                out.flush();
            }

            byte[] rawData = baos.toByteArray();

            byte[] encryptedData = client.getEncryption().encrypt(rawData);

            ByteBuf buf = ctx.alloc().buffer(encryptedData.length);
            buf.writeBytes(encryptedData);

            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buf);
            ctx.write(frame, promise);

        } else {
            super.write(ctx, msg, promise);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace(System.err);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new PingWebSocketFrame());
            } else if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            }
        }
    }

}
