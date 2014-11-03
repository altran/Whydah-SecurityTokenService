package net.whydah.token.application;

import net.whydah.token.user.CompanyRoles;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ApplicationData implements Serializable{
    private final Map<String, CompanyRoles> companiesAndRolesMap = new HashMap<>();
    private String applicationName;
    private String applicationID;



    public String getApplicationName() {
        return applicationName;
    }
    public String getApplicationID() {
        return applicationID;
    }
    public Map<String, CompanyRoles> getCompaniesAndRolesMap() {
        return companiesAndRolesMap;
    }


    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }
    public void addCompanyWithRoles(String company,CompanyRoles companiesAndRolesMap) {
        this.companiesAndRolesMap.put(company,companiesAndRolesMap);
    }
    public Collection<CompanyRoles> getCompanyRoles() {
        return companiesAndRolesMap.values();
    }

    @Override
    public String toString() {
        return "ApplicationData{" +
                "applicationName='" + applicationName + '\'' +
                ", applicationID='" + applicationID + '\'' +
                ", companiesAndRolesMap=" + companiesAndRolesMap +
                '}';
    }
}
