package ua.zaskarius.keycloak.plugins.radius.transaction;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AuthenticationManager;
import org.testng.annotations.Test;
import ua.zaskarius.keycloak.plugins.radius.test.AbstractRadiusTest;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class KeycloakRadiusUtilsTest extends AbstractRadiusTest {
    private static KeycloakHelper keycloakHelper = new KeycloakStaticHelper();
    @Test
    public void testActiveTran() {
        Boolean res = KeycloakRadiusUtils.runJobInTransaction(keycloakSessionFactory, session -> true);
        assertTrue(res);
        verify(keycloakTransactionManager).commit();
    }

    @Test
    public void testActiveTran2() {
        Boolean res = KeycloakRadiusUtils.runJobInTransaction(session, session -> true);
        assertTrue(res);
        verify(keycloakTransactionManager).commit();
    }

    @Test
    public void testNotActiveTran() {
        when(keycloakTransactionManager.isActive()).thenReturn(false);
        Boolean res = KeycloakRadiusUtils.runJobInTransaction(keycloakSessionFactory,
                session -> true);
        assertNull(res);
        verify(keycloakTransactionManager, never()).commit();
        verify(keycloakTransactionManager, never()).rollback();
    }

    @Test
    public void testRollBackOnlyTran() {
        when(keycloakTransactionManager.getRollbackOnly()).thenReturn(true);
        Boolean res = KeycloakRadiusUtils.runJobInTransaction(keycloakSessionFactory,
                session -> true);
        assertNull(res);
        verify(keycloakTransactionManager).rollback();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testException() {
        //when(keycloakTransactionManager.getRollbackOnly()).thenReturn(true);
        Boolean res = KeycloakRadiusUtils.runJobInTransaction(keycloakSessionFactory,
                new KeycloakSessionTaskWithReturn<Boolean>() {
                    @Override
                    public Boolean run(KeycloakSession session) {
                        throw new IllegalStateException("test");
                    }
                });
        assertNull(res);
        verify(keycloakTransactionManager, never()).rollback();
        verify(keycloakTransactionManager).rollback();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExceptionNotActive() {
        when(keycloakTransactionManager.isActive()).thenReturn(false);
        Boolean res = KeycloakRadiusUtils.runJobInTransaction(keycloakSessionFactory,
                session -> {
                    throw new IllegalStateException("test");
                });
        assertNull(res);
        verify(keycloakTransactionManager, never()).rollback();
    }

    @Test
    public void testGetKeycloakHelper() {
        KeycloakHelper keycloakHelper = KeycloakRadiusUtils.getKeycloakHelper();
        assertNotNull(keycloakHelper);
        AuthenticationManager.AuthResult authResult = keycloakHelper.getAuthResult(session);
        assertNotNull(authResult);
        assertEquals(authResult.getUser(), userModel);
        assertEquals(authResult.getSession(), userSessionModel);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetKeycloakHelperReal() {
        assertNotNull(keycloakHelper);
        AuthenticationManager.AuthResult authResult = keycloakHelper.getAuthResult(session);
        assertNotNull(authResult);
        assertEquals(authResult.getUser(), userModel);
        assertEquals(authResult.getSession(), userSessionModel);
    }
}
