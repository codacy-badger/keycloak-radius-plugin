package ua.zaskarius.keycloak.plugins.radsec.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.handler.RequestHandler;
import ua.zaskarius.keycloak.plugins.radius.providers.IRadiusAccountHandlerProvider;
import ua.zaskarius.keycloak.plugins.radius.providers.IRadiusAuthHandlerProvider;
import ua.zaskarius.keycloak.plugins.radsec.providers.IRadiusRadSecHandlerProvider;
import ua.zaskarius.keycloak.plugins.radsec.providers.IRadiusRadSecHandlerProviderFactory;

@ChannelHandler.Sharable
public class RadSecHandler
        extends RequestHandler
        implements IRadiusRadSecHandlerProviderFactory,
        IRadiusRadSecHandlerProvider {

    public static final String DEFAULT_RADSEC_RADIUS_PROVIDER = "default-radsec-radius-provider";

    private KeycloakSessionFactory sessionFactory;

    @Override
    protected Class<RadiusPacket> acceptedPacketType() {
        return RadiusPacket.class;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            RadiusPacket request = msg.getRequest();
            if (request.getClass().equals(AccessRequest.class)) {
                IRadiusAuthHandlerProvider provider = session
                        .getProvider(IRadiusAuthHandlerProvider.class);
                provider.getChannelHandler(session);
                provider.directRead(ctx, msg);
            } else if (request.getClass().equals(AccountingRequest.class)) {
                IRadiusAccountHandlerProvider provider = session
                        .getProvider(IRadiusAccountHandlerProvider.class);
                provider.getChannelHandler(session);
                provider.directRead(ctx, msg);
            } else {
                throw new IllegalStateException(request + " (" + request
                        .getClass() + ") does not have Handler");
            }
        });
    }

    @Override
    public IRadiusRadSecHandlerProvider create(KeycloakSession session) {
        this.sessionFactory = session.getKeycloakSessionFactory();
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return DEFAULT_RADSEC_RADIUS_PROVIDER;
    }

    @Override
    public ChannelHandler getChannelHandler(KeycloakSession session) {
        return (ChannelHandler) create(session);
    }
}
