package net.whydah.token.application;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommandForBooleanType;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CommandCheckApplicationCredentialInUAS extends BaseHttpPostHystrixCommandForBooleanType {


    private ApplicationCredential appCredential;

    public CommandCheckApplicationCredentialInUAS(URI uasServiceUri, ApplicationToken stsApplicationToken, ApplicationCredential appCredential) {
        super(uasServiceUri, ApplicationTokenMapper.toXML(stsApplicationToken), stsApplicationToken.getApplicationTokenId(), "UASApplicationAuthGroup", 3000);
        this.appCredential = appCredential;
        if (uasServiceUri == null || appCredential == null) {
            log.error(TAG + " initialized with null-values - will fail. tokenServiceUri:{}, appCredential:{} ", uasServiceUri, appCredential);
        }

    }


    @Override
    protected String getTargetPath() {
        return "application/auth";
    }

    @Override
    protected Map<String, String> getFormParameters() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("appCredentialXml", ApplicationCredentialMapper.toXML(appCredential));
        return data;
    }


}

