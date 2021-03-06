package ua.zaskarius.keycloak.plugins.radius.providers;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static ua.zaskarius.keycloak.plugins.radius.providers.AccountingRadiusHandlerSPI.ACCOUNT_RADIUS_SPI;

public class AccountRadiusHandlerSpiTest {
    private AccountingRadiusHandlerSPI radiusProviderSpi =
            new AccountingRadiusHandlerSPI();

    @Test
    public void testMethods() {
        assertEquals(radiusProviderSpi.getProviderClass(),
                IRadiusAccountHandlerProvider.class);
        assertEquals(radiusProviderSpi.getProviderFactoryClass(),
                IRadiusAccountHandlerProviderFactory.class);
        assertEquals(radiusProviderSpi.getName(), ACCOUNT_RADIUS_SPI);
        assertFalse(radiusProviderSpi.isInternal());
    }
}
