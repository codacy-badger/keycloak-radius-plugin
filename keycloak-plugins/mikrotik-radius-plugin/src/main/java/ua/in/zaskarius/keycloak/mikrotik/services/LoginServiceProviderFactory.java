package ua.in.zaskarius.keycloak.mikrotik.services;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.packet.AccessRequest;
import ua.zaskarius.keycloak.plugins.radius.providers.IRadiusServiceProvider;
import ua.zaskarius.keycloak.plugins.radius.providers.IRadiusServiceProviderFactory;

import static org.tinyradius.packet.AccessRequest.AUTH_MS_CHAP_V2;
import static ua.in.zaskarius.keycloak.mikrotik.MikrotikConstantUtils.MIKROTIK_SERVICE_ATTRIBUTE;

public class LoginServiceProviderFactory
        implements IRadiusServiceProviderFactory, IRadiusServiceProvider {

    public static final String MIKROTIK_LOGIN_SERVICE = "mikrotik-login-service";

    @Override
    public IRadiusServiceProvider create(KeycloakSession session) {
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
        return MIKROTIK_LOGIN_SERVICE;
    }

    @Override
    public String attrbuteName() {
        return MIKROTIK_SERVICE_ATTRIBUTE;
    }

    @Override
    public String serviceName() {
        return "login";
    }

    @Override
    public boolean checkService(AccessRequest accessRequest) {
        RadiusAttribute serviceTypeAttribute = accessRequest.getAttribute("Service-Type");
        return serviceTypeAttribute != null && "Login-User"
                .equalsIgnoreCase(serviceTypeAttribute.getValueString())
                && AUTH_MS_CHAP_V2.equalsIgnoreCase(accessRequest.getAuthProtocol());
    }
}
