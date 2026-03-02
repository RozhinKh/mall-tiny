# Smoke Test Implementation Details

## Test Class: MallTinySmokeTests

**Location**: `src/test/java/com/macro/mall/tiny/MallTinySmokeTests.java`  
**Lines of Code**: ~550  
**Test Methods**: 19  
**Framework**: JUnit 5 + Spring Boot Test + MockMvc  
**Scope**: Integration tests (not unit tests)

## Class Structure

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DisplayName("Mall-Tiny Core API Smoke Tests (Java 17)")
public class MallTinySmokeTests { ... }
```

### Key Annotations Explained

| Annotation | Purpose | Value |
|-----------|---------|-------|
| `@SpringBootTest` | Starts full Spring context for integration testing | N/A |
| `webEnvironment = RANDOM_PORT` | Runs embedded Tomcat on random port | Avoids port conflicts |
| `@ActiveProfiles("dev")` | Uses dev configuration profile | Loads application-dev.yml |
| `@DisplayName` | Human-readable test names in reports | Shows full context |

## Test Setup

### Initialization Method
```java
@BeforeAll
void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .build();
}
```

**Purpose**: Creates MockMvc instance for making HTTP requests  
**Timing**: Runs once before all tests in the class  
**Scope**: Shared across all test methods

### Key Class Properties

```java
@Autowired private WebApplicationContext webApplicationContext;  // Spring context
@Autowired private ObjectMapper objectMapper;                    // Jackson JSON handler
private MockMvc mockMvc;                                         // HTTP request simulator
```

## Test Constants

```java
private static final String LOGIN_URL = "/admin/login";
private static final String INFO_URL = "/admin/info";
private static final String LIST_URL = "/admin/list";
private static final String TOKEN_HEADER = "Authorization";
private static final String TOKEN_PREFIX = "Bearer ";
private static final String TEST_USERNAME = "admin";
private static final String TEST_PASSWORD = "admin";
private static final String INVALID_PASSWORD = "wrongpassword";
private static final String NONEXISTENT_USER = "nonexistentuser";
```

**Purpose**: Centralized test data configuration  
**Usage**: Ensures consistency across tests  
**Maintainability**: Easy to update credentials if needed

## Test Patterns Used

### Pattern 1: Basic Endpoint Test
```java
@Test
@DisplayName("T1: Verify application is running")
void testApplicationStartup() throws Exception {
    mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
}
```

**Components**:
- `mockMvc.perform()` - Executes HTTP request
- `get()` - HTTP GET method
- `.andExpect()` - Assertion chain
- `status().isOk()` - Expects HTTP 200
- `jsonPath()` - JSON path assertion (like XPath for JSON)

### Pattern 2: Login and Token Extraction
```java
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
```

**Process**:
1. Create request object (UmsAdminLoginParam)
2. Serialize to JSON using ObjectMapper
3. Send POST request with JSON body
4. Capture complete response
5. Deserialize to Map
6. Extract token from nested data structure

**Java 17 Specific**: Uses record type (UmsAdminLoginParam) which was added in Java 16

### Pattern 3: Protected Endpoint with Token
```java
mockMvc.perform(get(INFO_URL)
        .header(TOKEN_HEADER, TOKEN_PREFIX + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
```

**Key Elements**:
- `.header()` - Adds HTTP header (Authorization)
- Token format: `"Bearer " + token`
- Expects Spring Security to validate and accept token

### Pattern 4: Validation Testing
```java
String invalidRequest = "{\"username\": \"\", \"password\": \"admin\"}";

mockMvc.perform(post(LOGIN_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidRequest))
        .andExpect(status().isBadRequest());
```

**Purpose**: Ensures Spring Validation rejects invalid input  
**Constraint Used**: `@NotEmpty` on UmsAdminLoginParam  
**Response**: HTTP 400 Bad Request

## Java 17 Specific Implementation Details

### Java 17 Record Handling

**DTO Definition** (Java 17 record):
```java
public record UmsAdminLoginParam(
    @NotEmpty String username,
    @NotEmpty String password
) {}
```

**Test Implications**:
- Records are immutable by default ✅
- Jackson 2.13+ (in Spring Boot 2.7.5) fully supports records ✅
- No need for @Data or Lombok getters/setters ✅
- Canonical constructor automatically generated ✅

**Serialization Test**:
```java
UmsAdminLoginParam param = new UmsAdminLoginParam("admin", "admin");
String json = objectMapper.writeValueAsString(param);  // Record serializes naturally
UmsAdminLoginParam deserialized = objectMapper.readValue(json, UmsAdminLoginParam.class);
// Record canonical constructor is used for deserialization
```

### Generic Type Handling
```java
// Challenge: Jackson must handle generic CommonResult<T>
@SuppressWarnings("unchecked")
CommonResult<Map<String, String>> result = 
    objectMapper.readValue(response, CommonResult.class);

// Java 17: Type erasure still applies, but Jackson handles gracefully
// No Java 17 specific issues with generics
```

### JSON Path Assertions
```java
// Using JSONPath for complex JSON assertions (XPath-like syntax for JSON)
.andExpect(jsonPath("$.data.token").isNotEmpty())
.andExpect(jsonPath("$.data.tokenHead").value("Bearer "))
.andExpect(jsonPath("$.code").value(200))
```

**JSONPath Features Used**:
- `$` - Root of JSON document
- `.fieldName` - Access object property
- `[0]` - Array index (if used)
- `isEmpty()` - Check empty value
- `value()` - Assert exact value
- `isNotEmpty()` - Assert non-empty

## Spring Boot Test Infrastructure

### Test Database Setup
```
@SpringBootTest loads:
- application.yml (main configuration)
- application-dev.yml (dev profile specific)
- TestDatabaseAutoConfiguration (for test database)
- Embedded server on random port
```

### Key Spring Boot Test Features
- **Auto-configuration**: Spring loads minimal required beans
- **Embedded Server**: Tomcat starts on random port
- **Test Isolation**: Each test class gets fresh context (unless shared)
- **Property Override**: Test properties override application.yml

### Dependency Injection in Tests
```java
@Autowired private WebApplicationContext webApplicationContext;
@Autowired private ObjectMapper objectMapper;
```

**How it works**:
1. Spring Boot Test creates application context
2. Beans are created and registered
3. Test class is instantiated
4. @Autowired fields are injected
5. Test methods can use injected beans

## Security Testing Specifics

### JWT Token Validation
```java
// Test validates:
// 1. Token is generated (not null)
// 2. Token has valid JWT format (3 parts with dots)
// 3. Token can be used in subsequent requests
// 4. Invalid tokens are rejected

String[] parts = token.split("\\.");
assertEquals(3, parts.length);  // JWT always has 3 parts
```

### Spring Security Filter Chain
```
Request Flow:
1. MockMvc sends request
2. DispatcherServlet processes
3. Spring Security filter chain intercepts:
   - JwtAuthenticationTokenFilter (checks Authorization header)
   - FilterSecurityInterceptor (checks authorization)
4. Request reaches controller
5. Response returned
```

## Error Handling Verification

### Validation Errors
```java
// @NotEmpty constraint on username
String invalidRequest = "{\"username\": \"\", \"password\": \"admin\"}";
// Result: HTTP 400 (handled by Spring Validation)
```

### Authentication Errors
```java
// Invalid credentials
// Result: HTTP 200 with code=401 (custom handling in endpoint)
```

### Type Errors
```java
// Wrong type for field (number instead of string)
String invalidJson = "{\"username\": 123, \"password\": \"admin\"}";
// Result: HTTP 400 (Jackson type mismatch)
```

## Assertions and Matchers

### MockMvc Assertions
```java
.andExpect(status().isOk())                    // HTTP 200
.andExpect(status().isUnauthorized())          // HTTP 401
.andExpect(status().isBadRequest())            // HTTP 400
.andExpect(content().contentType(MediaType.APPLICATION_JSON))
.andExpect(header().exists("Authorization"))
```

### JSONPath Matchers
```java
.andExpect(jsonPath("$.code").value(200))     // Exact value match
.andExpect(jsonPath("$.token").isNotEmpty())  // Non-empty assertion
.andExpect(jsonPath("$.data").exists())       // Field exists
.andExpect(jsonPath("$.data.username").value("admin"))
```

### Custom Assertions
```java
@SuppressWarnings("unchecked")
Map<String, Object> jsonMap = objectMapper.readValue(response, Map.class);

assertTrue(jsonMap.containsKey("code"));      // Field exists
assertNotNull(jsonMap.get("data"));           // Not null
assertEquals(200.0, jsonMap.get("code"));     // Exact value
assertFalse(token.isEmpty());                 // Not empty
```

## Test Execution Flow

### Sequential Execution
```
Test Class Initialization
  ↓
@BeforeAll setUp() runs once
  ↓
Test 1 execution
  ↓
Test 2 execution
  ↓
... (Tests 3-19)
  ↓
Test 19 execution
  ↓
Class cleanup (if any)
```

### Per-Test Isolation
- Each test method is independent
- No shared state between tests
- New MockMvc instance shared (stateless)
- No database transactions (unless explicitly used)

## Debugging Tests

### Enable Detailed Output
```bash
# Verbose logging
mvn test -Dtest=MallTinySmokeTests -X

# Print requests/responses
mockMvc.perform(...).andDo(print());

# Print specific parts
System.out.println(result.getResponse().getContentAsString());
```

### Common Issues and Solutions

**Issue**: Test timeout  
**Solution**: Check if server is starting (might need to increase timeout)

**Issue**: Token null  
**Solution**: Check login endpoint is working, verify test credentials

**Issue**: 404 Not Found  
**Solution**: Verify endpoint paths, check Spring routing configuration

**Issue**: Type deserialization errors  
**Solution**: Ensure ObjectMapper can handle the type (use @SuppressWarnings if needed)

## Performance Considerations

### Test Startup
- Spring context initialization: ~3-8 seconds (first test)
- MockMvc setup: <100ms
- Subsequent tests: ~100-500ms each

### Optimization Tips
- Use @WebMvcTest if testing only controllers (lighter weight)
- Avoid database operations in unit tests
- Cache test data where possible
- Run tests in parallel with `mvn -T 1C test`

## Extending the Tests

### Adding New Test
```java
@Test
@DisplayName("Descriptive test name for reporting")
void testNewFeature() throws Exception {
    // 1. Setup test data
    String requestBody = objectMapper.writeValueAsString(testData);
    
    // 2. Execute request
    MvcResult result = mockMvc.perform(post("/api/endpoint")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();
    
    // 3. Verify response
    String response = result.getResponse().getContentAsString();
    // ... assertions
}
```

### Adding Test Category Comment
```java
// ==================== Category Name ====================
// This section tests X functionality...
@Test void testX() { ... }
@Test void testY() { ... }
```

## Test Maintenance

### When to Update Tests
1. **API Contract Changes**: Update endpoint URLs, request/response structures
2. **Security Changes**: Update token format expectations
3. **Validation Changes**: Update constraint expectations
4. **Error Code Changes**: Update expected response codes

### Test Documentation
- Each test has `@DisplayName` for clarity
- Inline comments explain complex assertions
- Test methods follow naming pattern: `test<Feature><Condition>()`

## Compatibility Matrix

| Component | Java 1.8 | Java 11 | Java 17 | Status |
|-----------|----------|---------|---------|--------|
| JUnit 5 | Yes | Yes | Yes | ✅ Works |
| Spring Boot 2.7.5 | Yes | Yes | Yes | ✅ Works |
| Jackson 2.13 | Yes | Yes | Yes | ✅ Works |
| Java Records | No | No | Yes | ✅ New |
| JWT (JJWT 0.9.1) | Yes | Yes | Yes | ✅ Works |
| Spring Security 5.7 | Yes | Yes | Yes | ✅ Works |

## Conclusion

This test suite provides comprehensive validation of API functionality with Java 17, using:
- ✅ Modern JUnit 5 testing framework
- ✅ Spring Boot integration test infrastructure
- ✅ MockMvc for HTTP simulation
- ✅ Jackson for JSON processing
- ✅ Java 17 records in request DTOs
- ✅ Proper test organization and documentation

**All tests are Java 17 compatible and follow Spring Boot best practices.**
