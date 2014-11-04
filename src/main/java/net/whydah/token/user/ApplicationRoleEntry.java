package net.whydah.token.user;

public class ApplicationRoleEntry {
    private String applicationId;
    private String applicationName;
    private String organizationName;
    private String roleName;
    private String roleValue;

    public ApplicationRoleEntry() {
    }

    public ApplicationRoleEntry(String applicationId, String applicationName, String organizationName, String roleName, String roleValue) {
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.organizationName = organizationName;
        this.roleName = roleName;
        this.roleValue = roleValue;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    public void setRoleValue(String roleValue) {
        this.roleValue = roleValue;
    }

    public String getApplicationId() {
        return applicationId;
    }
    public String getApplicationName() {
        return applicationName;
    }
    public String getOrganizationName() {
        return organizationName;
    }
    public String getRoleName() {
        return roleName;
    }
    public String getRoleValue() {
        return roleValue;
    }

    @Override
    public String toString() {
        return "ApplicationRoleEntry{" +
                "applicationId='" + applicationId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", roleName='" + roleName + '\'' +
                ", roleValue='" + roleValue + '\'' +
                '}';
    }
}
