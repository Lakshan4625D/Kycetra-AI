
package com.lakshan.kycetra_ai;

import com.lakshan.kycetra_ai.controller.AuthorizationTestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthorizationTestController.class)
class AuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCanAccessUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/authorization/user"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotAccessAnalystEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/authorization/analyst"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/authorization/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessProtectedEndpoint()
            throws Exception {
        mockMvc.perform(get("/api/test/authorization/user"))
                .andExpect(status().isUnauthorized());
    }
}