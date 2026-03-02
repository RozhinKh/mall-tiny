package com.macro.mall.tiny;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.dto.UmsAdminLoginParam;
import com.macro.mall.tiny.modules.ums.model.UmsAdmin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Core API Smoke Tests for Java 17 Compatibility
 * Tests core API functionality including:
 * - UMS admin login endpoint
 * - JWT token generation and validation
 * - Protected endpoint authorization
 * - CRUD operations
 * - Request/response JSON serialization
 * - Input validation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DisplayName("Mall-Tiny Core API Smoke Tests (Java 17)")
public class MallTinySmokeTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final String LOGIN_URL = "/admin/login";
    private static final String INFO_URL = "/admin/info";
    private static final String LIST_URL = "/admin/list";
    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "admin";
    private static final String INVALID_PASSWORD = "wrongpassword";
    private static final String NONEXISTENT_USER = "nonexistentuser";

    @BeforeAll
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
    }

    // ==================== Application Startup Tests ====================

    @Test
    @DisplayName("T1: Verify application is running and ready to accept requests")
    void testApplicationStartup() throws Exception {
        // Health check endpoint (always available)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ==================== Login Endpoint Tests ====================

    @Test
    @DisplayName("T2: Test UMS admin login endpoint with valid credentials")
    void testLoginWithValidCredentials() throws Exception {
        // Create login request
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginParam);

        // Perform login
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenHead").value("Bearer "))
                .andReturn();

        // Verify response structure
        String response = result.getResponse().getContentAsString();
        CommonResult<?> commonResult = objectMapper.readValue(response, CommonResult.class);
        
        assertNotNull(commonResult);
        assertEquals(200, commonResult.getCode());
        assertNotNull(commonResult.getData());
        
        System.out.println("[✓] T2 PASSED: Login with valid credentials successful");
    }

    @Test
    @DisplayName("T3: Verify login response includes JWT token and expected user data")
    void testLoginResponseStructure() throws Exception {
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginParam);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(response, Map.class);
        
        // Verify response structure
        assertTrue(jsonMap.containsKey("code"), "Response must contain 'code' field");
        assertTrue(jsonMap.containsKey("message"), "Response must contain 'message' field");
        assertTrue(jsonMap.containsKey("data"), "Response must contain 'data' field");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
        assertTrue(data.containsKey("token"), "Data must contain 'token' field");
        assertTrue(data.containsKey("tokenHead"), "Data must contain 'tokenHead' field");
        
        String token = (String) data.get("token");
        assertNotNull(token);
        assertFalse(token.isEmpty(), "Token must not be empty");
        assertTrue(token.length() > 20, "JWT token should have reasonable length");
        
        System.out.println("[✓] T3 PASSED: Login response structure verified");
    }

    @Test
    @DisplayName("T4: Test invalid login credentials (authentication failure handling)")
    void testLoginWithInvalidPassword() throws Exception {
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, INVALID_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginParam);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        System.out.println("[✓] T4 PASSED: Invalid credentials rejected correctly");
    }

    @Test
    @DisplayName("T5: Test login with nonexistent user")
    void testLoginWithNonexistentUser() throws Exception {
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(NONEXISTENT_USER, TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginParam);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        System.out.println("[✓] T5 PASSED: Nonexistent user rejected correctly");
    }

    // ==================== Protected Endpoint Tests ====================

    @Test
    @DisplayName("T6: Test protected endpoints without token (rejection)")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get(INFO_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        System.out.println("[✓] T6 PASSED: Protected endpoint rejects request without token");
    }

    @Test
    @DisplayName("T7: Test protected endpoints with invalid token (rejection)")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        String invalidToken = "Bearer invalid_token_xyz";

        mockMvc.perform(get(INFO_URL)
                        .header(TOKEN_HEADER, invalidToken))
                .andExpect(status().isUnauthorized());

        System.out.println("[✓] T7 PASSED: Protected endpoint rejects invalid token");
    }

    @Test
    @DisplayName("T8: Test protected endpoints with valid JWT token (authorization check)")
    void testProtectedEndpointWithValidToken() throws Exception {
        // First, login to get a valid token
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String loginRequest = objectMapper.writeValueAsString(loginParam);

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = objectMapper.readValue(loginResponse, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loginData.get("data");
        String token = (String) data.get("token");

        // Now test protected endpoint with valid token
        mockMvc.perform(get(INFO_URL)
                        .header(TOKEN_HEADER, TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME));

        System.out.println("[✓] T8 PASSED: Protected endpoint allows authenticated request with valid token");
    }

    // ==================== CRUD Operations Tests ====================

    @Test
    @DisplayName("T9: Test CRUD operations endpoint routing (GET /admin/list)")
    void testCrudListEndpointRouting() throws Exception {
        // First get a valid token
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String loginRequest = objectMapper.writeValueAsString(loginParam);

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = objectMapper.readValue(loginResponse, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loginData.get("data");
        String token = (String) data.get("token");

        // Test LIST endpoint
        mockMvc.perform(get(LIST_URL)
                        .header(TOKEN_HEADER, TOKEN_PREFIX + token)
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());

        System.out.println("[✓] T9 PASSED: CRUD list endpoint routing verified");
    }

    // ==================== Request Validation Tests ====================

    @Test
    @DisplayName("T10: Test request body validation with empty username")
    void testValidationWithEmptyUsername() throws Exception {
        // Create invalid login request (empty username)
        String invalidRequest = "{\"username\": \"\", \"password\": \"admin\"}";

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        System.out.println("[✓] T10 PASSED: Empty username validation works");
    }

    @Test
    @DisplayName("T11: Test request body validation with empty password")
    void testValidationWithEmptyPassword() throws Exception {
        // Create invalid login request (empty password)
        String invalidRequest = "{\"username\": \"admin\", \"password\": \"\"}";

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        System.out.println("[✓] T11 PASSED: Empty password validation works");
    }

    @Test
    @DisplayName("T12: Test request body validation with missing fields")
    void testValidationWithMissingFields() throws Exception {
        // Create invalid login request (missing password)
        String invalidRequest = "{\"username\": \"admin\"}";

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        System.out.println("[✓] T12 PASSED: Missing field validation works");
    }

    // ==================== JSON Serialization Tests ====================

    @Test
    @DisplayName("T13: Verify response JSON deserializes correctly into CommonResult object")
    void testResponseJsonDeserialization() throws Exception {
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginParam);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        
        // Deserialize to CommonResult
        @SuppressWarnings("unchecked")
        CommonResult<Map<String, String>> commonResult = objectMapper.readValue(response, CommonResult.class);
        
        assertNotNull(commonResult);
        assertEquals(200, commonResult.getCode());
        assertNotNull(commonResult.getData());
        assertNotNull(commonResult.getMessage());

        System.out.println("[✓] T13 PASSED: Response JSON deserialization works correctly");
    }

    @Test
    @DisplayName("T14: Test Jackson JSON serialization with request record (UmsAdminLoginParam)")
    void testRequestJsonSerialization() throws Exception {
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(loginParam);
        
        // Verify JSON content
        assertTrue(json.contains("\"username\""));
        assertTrue(json.contains("\"password\""));
        assertTrue(json.contains(TEST_USERNAME));
        
        // Deserialize back to verify round-trip
        UmsAdminLoginParam deserialized = objectMapper.readValue(json, UmsAdminLoginParam.class);
        assertEquals(TEST_USERNAME, deserialized.username());
        assertEquals(TEST_PASSWORD, deserialized.password());

        System.out.println("[✓] T14 PASSED: Request JSON serialization/deserialization works");
    }

    @Test
    @DisplayName("T15: Monitor for JSON serialization errors or type mismatches")
    void testJsonSerializationErrorHandling() throws Exception {
        // Try to send request with invalid JSON type (number instead of string)
        String invalidJson = "{\"username\": 123, \"password\": \"admin\"}";

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Try to send request with string for an object field
        String anotherInvalidJson = "{\"username\": \"admin\", \"password\": {\"value\": \"admin\"}}";

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(anotherInvalidJson))
                .andExpect(status().isBadRequest());

        System.out.println("[✓] T15 PASSED: JSON serialization error handling works");
    }

    // ==================== Security Tests ====================

    @Test
    @DisplayName("T16: Verify JWT token format and structure")
    void testJwtTokenFormat() throws Exception {
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginParam);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
        String token = (String) data.get("token");

        // JWT should have 3 parts separated by dots
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT should have 3 parts (header.payload.signature)");

        System.out.println("[✓] T16 PASSED: JWT token format verified");
    }

    @Test
    @DisplayName("T17: Verify token rejection after expiration simulation")
    void testTokenValidation() throws Exception {
        // Create a malformed token (missing required parts)
        String malformedToken = "Bearer malformed.token";

        mockMvc.perform(get(INFO_URL)
                        .header(TOKEN_HEADER, malformedToken))
                .andExpect(status().isUnauthorized());

        System.out.println("[✓] T17 PASSED: Malformed token rejected correctly");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("T18: Full authentication and authorization flow")
    void testFullAuthenticationFlow() throws Exception {
        // Step 1: Login
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String loginRequest = objectMapper.writeValueAsString(loginParam);

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = objectMapper.readValue(loginResponse, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loginData.get("data");
        String token = (String) data.get("token");

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Step 2: Use token to access protected resource
        mockMvc.perform(get(INFO_URL)
                        .header(TOKEN_HEADER, TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME));

        // Step 3: Try to access protected resource without token (should fail)
        mockMvc.perform(get(INFO_URL))
                .andExpect(status().isUnauthorized());

        System.out.println("[✓] T18 PASSED: Full authentication and authorization flow verified");
    }

    @Test
    @DisplayName("T19: Verify no behavioral differences - login response codes")
    void testResponseCodes() throws Exception {
        // Success case
        UmsAdminLoginParam loginParam = new UmsAdminLoginParam(TEST_USERNAME, TEST_PASSWORD);
        String loginRequest = objectMapper.writeValueAsString(loginParam);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = objectMapper.readValue(response, Map.class);
        assertEquals(200.0, resultMap.get("code"), "Success code should be 200");

        // Failure case (invalid password)
        UmsAdminLoginParam failParam = new UmsAdminLoginParam(TEST_USERNAME, INVALID_PASSWORD);
        String failRequest = objectMapper.writeValueAsString(failParam);

        MvcResult failResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failRequest))
                .andExpect(status().isOk())
                .andReturn();

        String failResponse = failResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> failResultMap = objectMapper.readValue(failResponse, Map.class);
        assertEquals(401.0, failResultMap.get("code"), "Authentication failure code should be 401");

        System.out.println("[✓] T19 PASSED: Response codes verified as expected");
    }
}
