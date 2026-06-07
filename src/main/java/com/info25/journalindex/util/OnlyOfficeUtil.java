package com.info25.journalindex.util;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.services.ConfigService;

@Service
public class OnlyOfficeUtil {
    String OO_SERVER_URL;
    String OO_SECRET;
    String ONLYOFFICE_SECRET;

    public OnlyOfficeUtil(ConfigService configService) {
        OO_SERVER_URL = configService.getConfigOption("ooServerUrl");
        OO_SECRET = configService.getConfigOption("ooServerSecret");
        ONLYOFFICE_SECRET = configService.getConfigOption("onlyOfficeSecret");
    }

    public String calculateTokenPackage(long sessionId, int fileId, OOFile ooFile) {
        ObjectMapper om = new ObjectMapper();

        String url = OO_SERVER_URL + "/wopi/files/" + sessionId + "/contents?access_token=" + OO_SECRET;

        ObjectNode on = om.createObjectNode();
        on.set("document", om.createObjectNode()
                .put("fileType", ooFile.getFileExtension())
                .put("key", Long.toString(sessionId))
                .put("title", "file")
                .put("url", url));
        on.set("editorConfig", om.createObjectNode()
                .put("callbackUrl", OO_SERVER_URL + "/onlyoffice-callback-handler/" + sessionId + "?access_token=" + OO_SECRET)
                .set("customization", om.createObjectNode()
                    .put("forcesave", true)));

        return calculateTokenPackageGivenON(on);
    }

    public String calculateTokenPackageGivenON(ObjectNode on) {
        Map<String, Object> claims = null;
        try {
            claims = new ObjectMapper().treeToValue(on, Map.class);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Algorithm algorithm = Algorithm.HMAC256(ONLYOFFICE_SECRET);
        String token = JWT.create()
                .withPayload(claims)
                .sign(algorithm);

        
        on.put("token", token);

        return on.toString();
    }
}
