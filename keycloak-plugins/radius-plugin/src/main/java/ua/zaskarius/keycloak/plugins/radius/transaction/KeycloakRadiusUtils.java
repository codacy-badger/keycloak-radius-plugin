package ua.zaskarius.keycloak.plugins.radius.transaction;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;

public final class KeycloakRadiusUtils {

    private static KeycloakHelper keycloakHelper = new KeycloakStaticHelper();

    private KeycloakRadiusUtils() {
    }

    public static <T> T runJobInTransaction(KeycloakSessionFactory factory,
                                            KeycloakSessionTaskWithReturn<T> task) {
        KeycloakSession session = factory.create();
        KeycloakTransaction tx = session.getTransactionManager();
        try {
            tx.begin();
            T response = task.run(session);
            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                    return response;
                }
            }
            return null;
        } catch (RuntimeException re) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            session.close();
        }
    }

    public static <T> T runJobInTransaction(KeycloakSession session,
                                            KeycloakSessionTaskWithReturn<T> task) {
        return runJobInTransaction(session.getKeycloakSessionFactory(), task);
    }

    public static KeycloakHelper getKeycloakHelper() {
        return keycloakHelper;
    }

    public static void setKeycloakHelper(KeycloakHelper keycloakHelper) {
        KeycloakRadiusUtils.keycloakHelper = keycloakHelper;
    }
}
