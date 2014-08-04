package net.whydah.token.data.helper;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: totto
 * Date: Nov 4, 2010
 * Time: 1:48:25 PM
 */
public class CompanyRoles  implements Serializable {
    private String companyName;
    private String companyNumber;
    private Map<String,String> roleMap;


    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }
    public void setRoleMap(Map<String,String> roleMap) {
        this.roleMap = roleMap;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }


    public String getCompanyName() {
        return companyName;
    }
    public String getCompanyNumber() {
        return companyNumber;
    }
    public Map<String, String> getRoleMap() {
        return roleMap;
    }


    @Override
    public String toString() {
        return "CompanyRoles{" +
                "companyNumber='" + companyNumber + '\'' +
                ", companyName='" + companyName + '\'' +
                ", roleMap=" + roleMap +
                '}';
    }
}
