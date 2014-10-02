package net.whydah.token.data.user;

public class ApplicationRoleEntry {
    private String applicationid;

    public String getApplicationname() {
        return applicationname;
    }

    public void setApplicationname(String applicationname) {
        this.applicationname = applicationname;
    }

    private String applicationname;
    private String organizationname;
    private String rolename;
    private String rolevalue;

    public String getApplicationid() {
        return applicationid;
    }

    public void setApplicationid(String applicationid) {
        this.applicationid = applicationid;
    }

    public String getOrganizationname() {
        return organizationname;
    }

    public void setOrganizationname(String organizationname) {
        this.organizationname = organizationname;
    }

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }

    public String getRolevalue() {
        return rolevalue;
    }

    public void setRolevalue(String rolevalue) {
        this.rolevalue = rolevalue;
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
