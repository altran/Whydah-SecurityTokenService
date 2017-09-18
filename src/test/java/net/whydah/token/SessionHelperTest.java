package net.whydah.token;

import net.whydah.sso.application.helpers.ApplicationHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.token.application.SessionHelper;
import net.whydah.token.config.AppConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class SessionHelperTest {
	

    @BeforeClass
    public static void init() {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.TEST);
        System.setProperty(AppConfig.IAM_CONFIG_KEY, "src/test/testconfig.properties");
    }

   
	@Test
	public void testApplicationLiefSpan(){
		//TODO: test should be updated when implementing the SessionHelper
		List<Application> applications = ApplicationMapper.fromJsonList(ApplicationHelper.getDummyAppllicationListJson());
        Assert.assertTrue(SessionHelper.getApplicationLifeSpan(applications.get(0)) == 86400);

		
	}

}
