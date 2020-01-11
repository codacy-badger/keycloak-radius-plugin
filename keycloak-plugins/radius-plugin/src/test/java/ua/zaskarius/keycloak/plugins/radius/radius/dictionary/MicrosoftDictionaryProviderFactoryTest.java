package ua.zaskarius.keycloak.plugins.radius.radius.dictionary;

import org.testng.annotations.Test;
import ua.zaskarius.keycloak.plugins.radius.test.AbstractRadiusTest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class MicrosoftDictionaryProviderFactoryTest extends AbstractRadiusTest {
    private MicrosoftDictionaryProviderFactory dictionaryProviderFactory =
            new MicrosoftDictionaryProviderFactory();
    @Test
    public void testMethods(){
        dictionaryProviderFactory.close();
        dictionaryProviderFactory.init(null);
        dictionaryProviderFactory.postInit(null);
        assertNotNull(dictionaryProviderFactory.create(session));
        assertNotNull(dictionaryProviderFactory.getDictionaryParser());
        assertEquals(dictionaryProviderFactory.getId(),"Microsoft-Dictionary");
        assertEquals(dictionaryProviderFactory.getResources().size(),1);

    }
}