package ua.zaskarius.keycloak.plugins.radius.radius.handlers;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import ua.zaskarius.keycloak.plugins.radius.RadiusHelper;
import ua.zaskarius.keycloak.plugins.radius.configuration.RadiusConfigHelper;
import ua.zaskarius.keycloak.plugins.radius.event.log.EventLoggerUtils;
import ua.zaskarius.keycloak.plugins.radius.models.RadiusServerSettings;
import ua.zaskarius.keycloak.plugins.radius.models.RadiusUserInfo;
import ua.zaskarius.keycloak.plugins.radius.radius.handlers.clientconnection.RadiusClientConnection;
import ua.zaskarius.keycloak.plugins.radius.radius.handlers.protocols.AuthProtocol;
import ua.zaskarius.keycloak.plugins.radius.radius.handlers.session.KeycloakSessionUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.tinyradius.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.packet.PacketType.ACCESS_REJECT;
import static ua.zaskarius.keycloak.plugins.radius.client.RadiusLoginProtocolFactory.RADIUS_PROTOCOL;
import static ua.zaskarius.keycloak.plugins.radius.mappers.RadiusPasswordMapper.RADIUS_SESSION_PASSWORD;

public class KeycloakSecretProvider implements IKeycloakSecretProvider {

    private static final Logger LOGGER = Logger.getLogger(KeycloakSecretProvider.class);


    private String getSharedByIp(RadiusServerSettings radiusSettings,
                                 String hostAddress
    ) {
        String secret = radiusSettings.getAccessMap().get(hostAddress);
        if (secret != null) {
            LOGGER.info("RADIUS " + hostAddress + " connected");
        }
        return secret;
    }

    @Override
    public String getSharedSecret(InetSocketAddress address) {
        InetAddress inetAddress = address
                .getAddress();
        if (inetAddress == null) {
            return null;
        }
        String hostAddress = inetAddress
                .getHostAddress();
        RadiusServerSettings radiusSettings = RadiusConfigHelper.getConfig().getRadiusSettings();
        if (radiusSettings.getAccessMap() != null) {
            String secret = getSharedByIp(radiusSettings, hostAddress);
            if (secret != null) {
                return secret;
            }
        }
        String settingsSecret = radiusSettings.getSecret();
        if (settingsSecret == null || settingsSecret.isEmpty()) {
            LOGGER.warn("RADIUS " + address.getAddress().getHostAddress() + " disconnected");
        }
        return settingsSecret;
    }


    private List<String> getSessionPasswords(
            KeycloakSession keycloakSession,
            RealmModel realmModel,
            UserModel userModel
    ) {
        List<String> passwords = new ArrayList<>();
        if (userModel.isEnabled()) {
            List<UserSessionModel> userSessions = keycloakSession.sessions()
                    .getUserSessions(realmModel, userModel);
            for (UserSessionModel userSession : userSessions) {
                String sessionNote = userSession.getNote(RADIUS_SESSION_PASSWORD);
                if (sessionNote != null) {
                    passwords.addAll(Arrays.asList(sessionNote.split(",")));
                }
            }
        }
        return passwords;
    }

    private RadiusUserInfo create(
            KeycloakSession keycloakSession,
            UserModel userModel,
            AuthProtocol protocol,
            RealmModel realmModel, ClientModel client, RadiusClientConnection clientConnection) {
        RadiusUserInfo radiusUserInfo = new RadiusUserInfo();
        radiusUserInfo.setRealmModel(realmModel);
        List<String> passwords = new ArrayList<>(
                getSessionPasswords(keycloakSession, realmModel, userModel));
        String currentPassword = RadiusHelper.getCurrentPassword(
                keycloakSession, realmModel, userModel
        );
        if (currentPassword != null && !currentPassword.isEmpty()) {
            passwords.add(currentPassword);
        }
        radiusUserInfo.setPasswords(passwords);
        radiusUserInfo.setUserModel(userModel);
        radiusUserInfo.setProtocol(protocol);
        radiusUserInfo.setRadiusSecret(getSharedSecret(clientConnection
                .getInetSocketAddress()));
        radiusUserInfo.setClientModel(client);
        radiusUserInfo.setClientConnection(clientConnection);
        return radiusUserInfo;
    }

    private UserModel getUserModel(KeycloakSession localSession,
                                   String username, RealmModel realm) {
        UserModel user = localSession.users().getUserByUsername(username, realm);
        if (user == null) {
            user = localSession.users().getUserByEmail(username, realm);
        }
        return user;
    }

    private ClientModel getClient(InetSocketAddress address,
                                  KeycloakSession session,
                                  RealmModel realmModel) {
        List<ClientModel> clients = realmModel.getClients();
        for (ClientModel client : clients) {
            if (Objects.equals(client.getProtocol(), RADIUS_PROTOCOL)) {
                return client;
            }
        }
        EventBuilder event = EventLoggerUtils
                .createEvent(session, realmModel,
                        new RadiusClientConnection(address));
        LOGGER.error("Client with radius protocol does not found");
        event.event(EventType.LOGIN_ERROR).detail(
                EventLoggerUtils.RADIUS_MESSAGE, "Client with radius protocol does not found")
                .error("Client with radius protocol does not found");
        return null;
    }

    private boolean init(InetSocketAddress address,
                         String username,
                         AuthProtocol protocol,
                         KeycloakSession threadSession,
                         RealmModel realm,
                         ClientModel client) {
        RadiusClientConnection clientConnection = new RadiusClientConnection(address);
        EventBuilder event = EventLoggerUtils
                .createEvent(threadSession, realm, client, clientConnection);
        UserModel user = getUserModel(threadSession, username, realm);
        if (user != null && user.isEnabled()) {
            RadiusUserInfo radiusUserInfo = create(
                    threadSession, user, protocol, realm, client, clientConnection
            );
            KeycloakSessionUtils.addRadiusUserInfo(threadSession, radiusUserInfo);
            return true;
        } else {
            event.event(EventType.LOGIN_ERROR).detail(
                    EventLoggerUtils.RADIUS_MESSAGE, "USER DOES NOT EXIST")
                    .error("Login to RADIUS" + " fail for user " + username
                            + ", user disabled or does not exists");
        }
        return false;
    }

    @Override
    public boolean init(InetSocketAddress address,
                        String username,
                        AuthProtocol protocol,
                        KeycloakSession threadSession) {
        boolean successInit = false;
        RealmModel realm = protocol.getRealm();
        if (realm != null) {
            ClientModel client = getClient(address, threadSession, realm);
            if (client != null) {
                successInit = init(address, username, protocol, threadSession, realm, client);
            }
        }
        return successInit;
    }


    @Override
    public void afterAuth(int action,
                          KeycloakSession threadSession) {

        RadiusUserInfo radiusInfo = KeycloakSessionUtils
                .getRadiusUserInfo(threadSession);
        if (radiusInfo != null) {
            RealmModel realm = radiusInfo.getRealmModel();
            EventBuilder event = EventLoggerUtils
                    .createEvent(threadSession, realm,
                            radiusInfo.getClientModel(),
                            radiusInfo.getClientConnection());
            UserModel user = radiusInfo.getUserModel();
            if (action == ACCESS_ACCEPT) {
                event.user(user);
                event.event(EventType.LOGIN).detail("RADIUS", "success Login to RADIUS" +
                        " for user " + user.getUsername()).success();
            } else if (action == ACCESS_REJECT) {
                event.user(user);
                event.event(EventType.LOGIN_ERROR).detail("RADIUS", "Login to RADIUS" +
                        " fail for user " + user.getUsername()
                        + ", please check password and try again").error("RADIUS ERROR");
            }
        }

    }
}
