package net.whydah.token.data.user;

public class ApplicationRoleEntry {
    private String applicationid;
    private String applicationname;
    private String organizationname;
    private String rolename;
    private String rolevalue;

    public void setApplicationid(String applicationid) {
        this.applicationid = applicationid;
    }
    public void setApplicationname(String applicationname) {
        this.applicationname = applicationname;
    }
    public void setOrganizationname(String organizationname) {
        this.organizationname = organizationname;
    }
    public void setRolename(String rolename) {
        this.rolename = rolename;
    }
    public void setRolevalue(String rolevalue) {
        this.rolevalue = rolevalue;
    }

    public String getApplicationid() {
        return applicationid;
    }
    public String getApplicationname() {
        return applicationname;
    }
    public String getOrganizationname() {
        return organizationname;
    }
    public String getRolename() {
        return rolename;
    }
    public String getRolevalue() {
        return rolevalue;
    }

    @Override
    public String toString() {
        return "ApplicationRoleEntry{" +
                "applicationid='" + applicationid + '\'' +
                ", applicationname='" + applicationname + '\'' +
                ", organizationname='" + organizationname + '\'' +
                ", rolename='" + rolename + '\'' +
                ", rolevalue='" + rolevalue + '\'' +
                '}';
    }
}
