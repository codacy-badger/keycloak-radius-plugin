package ua.zaskarius.keycloak.plugins.radsec.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ResponseCtx;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.util.List;

@ChannelHandler.Sharable
public class RadSecCodec extends MessageToMessageCodec<ByteBuf, ResponseCtx> {
    private final PacketEncoder packetEncoder;
    private final SecretProvider secretProvider;

    public RadSecCodec(
            PacketEncoder packetEncoder,
            SecretProvider secretProvider
    ) {
        super();
        this.packetEncoder = packetEncoder;
        this.secretProvider = secretProvider;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out)
            throws Exception {
        RadiusPacket response = msg.getResponse();
        final RadiusPacket packet = response
                .encodeResponse(msg.getEndpoint().getSecret(),
                        msg.getRequest().getAuthenticator());
        ByteBuf byteBuf = packetEncoder.toByteBuf(packet);
        out.add(byteBuf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
            throws Exception {
        final RadiusPacket radiusPacket = packetEncoder.fromByteBuf(msg);
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx
                .channel().remoteAddress();
        String sharedSecret = secretProvider.getSharedSecret(remoteAddress);
        radiusPacket.verify(sharedSecret, new byte[16]);
        out.add(new RequestCtx(radiusPacket,
                new RadiusEndpoint(remoteAddress, sharedSecret)));
    }

}
