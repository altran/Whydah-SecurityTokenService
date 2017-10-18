package net.whydah.sts.application.authentication.commands;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommandForBooleanType;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CommandCheckApplicationCredentialInUAS extends BaseHttpPostHystrixCommandForBooleanType {


    private ApplicationCredential appCredential;
    private String stsApplicationTokenId;

    public CommandCheckApplicationCredentialInUAS(URI uasServiceUri, String stsApplicationTokenId, ApplicationCredential appCredential) {
        super(uasServiceUri, null, stsApplicationTokenId, "UASApplicationAuthGroup", 3000);
        this.appCredential = appCredential;
        this.stsApplicationTokenId = stsApplicationTokenId;
        if (uasServiceUri == null || appCredential == null) {
            log.error(TAG + " initialized with null-values - will fail. tokenServiceUri:{}, appCredential:{} ", uasServiceUri, appCredential);
        }

    }


    @Override
    protected String getTargetPath() {
        return stsApplicationTokenId + "/application/auth";
    }

    @Override
    protected Boolean dealWithFailedResponse(String responseBody, int statusCode) {
        if (statusCode == 204) {
            return true;
        }
        return false;
    }

    @Override
    protected Map<String, String> getFormParameters() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("appCredentialXml", ApplicationCredentialMapper.toXML(appCredential));
        return data;
    }


}

