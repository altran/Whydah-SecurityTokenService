package net.whydah.token.application;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.UUID;

import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.systemtestbase.SystemTestBaseConfig;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.session.baseclasses.ApplicationModelUtil;
import net.whydah.sso.user.helpers.UserXpathHelper;
import net.whydah.token.config.AppConfig;
import net.whydah.token.user.UserTokenFactory;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationFullTokenTest {

	private static final Logger log = LoggerFactory.getLogger(ApplicationFullTokenTest.class);
	   
	static SystemTestBaseConfig config;

	@BeforeClass
	public static void setup() throws Exception {
		config = new SystemTestBaseConfig();
	}

	//@Ignore
	@Test
	public void testValidFullTokenApplications() {

		if (config.isSystemTestEnabled()) {

			String myAppTokenXml = new CommandLogonApplication(config.tokenServiceUri, config.appCredential).execute();
			String myApplicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
			assertTrue(myApplicationTokenID != null && myApplicationTokenID.length() > 5);
			String userticket = UUID.randomUUID().toString();
			String userToken = new CommandLogonUserByUserCredential(config.tokenServiceUri, myApplicationTokenID, myAppTokenXml, config.userCredential, userticket).execute();
			String userTokenId = UserXpathHelper.getUserTokenId(userToken);
			assertTrue(userTokenId != null && userTokenId.length() > 5);
			
			//Initialize properties for this integration test
			System.setProperty("IAM_MODE", "TEST");
			
			//update full token application list
			List<Application> updateList = ApplicationModelUtil.getApplicationList(); //NOTHING NOW
			assertTrue(updateList==null || updateList.size()==0); //NOTHING NOW
			AppConfig.setFullTokenApplications("");//MAKE SURE NOTHING NOW IN THE LIST
			AppConfig.updateFullTokenApplicationList(config.userAdminServiceUri, myApplicationTokenID, userTokenId);
			//check if the list has been updated
			updateList = ApplicationModelUtil.getApplicationList();//APPLICATIONS WERE ADDED
			assertTrue(updateList.size()>0);//SOMETHING IN THE LIST
			boolean haveOneFullTokenApplication = false;
			for (Application application : updateList) {
               if(application.isFullTokenApplication()){
            	   haveOneFullTokenApplication = true;
            	   break;
               }
            }
			if(haveOneFullTokenApplication){
				assertTrue(AppConfig.getFullTokenApplications().length()>0); //THIS SHOULD INDICATE THAT WE HAVE ATLEAST ONE FULL TOKEN APPLICATION
			}
			
			
			//assume the testing application with a full token one 
			for (Application application : updateList) {
                if(application.getId().equals(config.appCredential.getApplicationID())){
                	application.getSecurity().setUserTokenFilter("false");
                }
            }
			//expectation: should return a full user token
			assertTrue(UserTokenFactory.shouldReturnFullUserToken(config.appCredential.getApplicationID()));
			
			
			
			//assume that we set back as a normal application without having a full token 
			for (Application application : updateList) {
                if(application.getId().equals(config.appCredential.getApplicationID())){
                	application.getSecurity().setUserTokenFilter("true");
                }
            }
			//expectation: should not return a full user token
			assertFalse(UserTokenFactory.shouldReturnFullUserToken(config.appCredential.getApplicationID()));

		}
	}
	
	



}
