package net.whydah.token.user;

public class UserCredential {

    public UserCredential(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    private final String userName;
    private final String password;

    public String toXML(){
        return "<?xml IMPLEMENTATION_VERSION=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
        "<usercredential>\n" +
        "    <params>\n" +
        "        <username>"+userName+"</username>\n" +
        "        <password>"+password+"</password>\n" +
        "    </params> \n" +
        "</usercredential>\n" ;
    }


}
