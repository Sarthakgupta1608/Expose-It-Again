package com.example.exposeit.api;

import com.example.exposeit.testinfra.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UnauthenticatedScanApiTest
 *
 * Scans all registered route mappings in the application dynamically via Spring's
 * RequestMappingHandlerMapping. For every endpoint that is not explicitly public (such as
 * auth endpoints or health checks), performs a request via MockMvc without any authorization
 * cookies/headers and asserts that the application security context rejects it with 401 Unauthorized.
 */
class UnauthenticatedScanApiTest extends BaseApiTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void scanAndVerifyUnauthenticatedAccessRejection() {
        handlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
            Set<String> patterns = mappingInfo.getPatternValues();
            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

            for (String path : patterns) {
                if (path.startsWith("/api/auth/") || path.equals("/actuator/health") || path.startsWith("/error")) {
                    continue;
                }

                if (methods.isEmpty()) {
                    verifyUnauthorized(path, "GET");
                } else {
                    for (RequestMethod method : methods) {
                        verifyUnauthorized(path, method.name());
                    }
                }
            }
        });
    }

    private void verifyUnauthorized(String path, String method) {
        System.out.println("Verifying path requires authentication: " + method + " " + path);
        String sanitizedPath = path.replaceAll("\\{[^}]+\\}", "dummy-id");

        try {
            switch (method.toUpperCase()) {
                case "GET":
                    mockMvc.perform(get(sanitizedPath)).andExpect(status().isUnauthorized());
                    break;
                case "POST":
                    mockMvc.perform(post(sanitizedPath).contentType("application/json").content("{}"))
                            .andExpect(status().isUnauthorized());
                    break;
                case "PUT":
                    mockMvc.perform(put(sanitizedPath).contentType("application/json").content("{}"))
                            .andExpect(status().isUnauthorized());
                    break;
                case "DELETE":
                    mockMvc.perform(delete(sanitizedPath)).andExpect(status().isUnauthorized());
                    break;
                case "PATCH":
                    mockMvc.perform(patch(sanitizedPath).contentType("application/json").content("{}"))
                            .andExpect(status().isUnauthorized());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            fail("Failed while verifying unauthorized access for path " + method + " " + path + ": " + e.getMessage());
        }
    }
}
