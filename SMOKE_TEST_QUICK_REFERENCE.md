# Core API Smoke Tests - Quick Reference Guide

## What is Being Tested?

This test suite validates that the mall-tiny application operates correctly with **Java 17** by testing:

1. **HTTP Request/Response Handling** - Proper HTTP protocol support
2. **JSON Serialization** - ObjectMapper and Jackson serialization/deserialization
3. **Authentication** - Login endpoint and JWT token generation
4. **Authorization** - Protected endpoints with token validation
5. **Input Validation** - Request body validation and constraint checking
6. **CRUD Operations** - Endpoint routing and access control

## Quick Start

### Prerequisites
```bash
# Ensure Java 17 is active
java -version  # Should show Java 17.x.x

# Ensure MySQL is running with database created
mysql -u root -p < sql/mall_tiny.sql
```

### Run All Tests
```bash
mvn clean test -Dtest=MallTinySmokeTests
```

### Run Single Test
```bash
# Example: Run login test
mvn test -Dtest=MallTinySmokeTests#testLoginWithValidCredentials
```

### Run Tests with Detailed Output
```bash
mvn test -Dtest=MallTinySmokeTests -X
```

## Test Categories

### Category 1: Application Startup (1 test)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T1 | Checks if app is running | Health endpoint returns UP |

### Category 2: Login Endpoint (4 tests)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T2 | Login with valid credentials | Receives JWT token + code 200 |
| T3 | Verify response structure | Token field present and valid |
| T4 | Login with wrong password | Returns code 401 |
| T5 | Login with nonexistent user | Returns code 401 |

### Category 3: Protected Endpoints (3 tests)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T6 | Access without token | Returns code 401 |
| T7 | Access with invalid token | Returns code 401 |
| T8 | Access with valid token | Returns code 200 + user data |

### Category 4: CRUD Operations (1 test)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T9 | Test list endpoint | Returns code 200 + admin list |

### Category 5: Input Validation (3 tests)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T10 | Empty username validation | Returns code 400 |
| T11 | Empty password validation | Returns code 400 |
| T12 | Missing field validation | Returns code 400 |

### Category 6: JSON Serialization (3 tests)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T13 | Response deserialization | Response deserializes to object |
| T14 | Request serialization | Record serializes/deserializes correctly |
| T15 | Type mismatch handling | Invalid types return code 400 |

### Category 7: Security (2 tests)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T16 | JWT format validation | Token has 3 dot-separated parts |
| T17 | Malformed token rejection | Invalid token returns code 401 |

### Category 8: Integration (2 tests)
| Test | What It Does | Success Indicator |
|------|-------------|------------------|
| T18 | Full auth flow | Login → use token → access protected |
| T19 | Response codes match | Success=200, Failure=401 |

## Sample API Requests (Manual Testing)

### Login
```bash
curl -X POST http://localhost:8080/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### Access Protected Resource with Token
```bash
# First get token from login response
TOKEN="<paste_token_from_login_response>"

# Then use it
curl -X GET http://localhost:8080/admin/info \
  -H "Authorization: Bearer $TOKEN"
```

### Access Protected Resource Without Token (Should Fail)
```bash
curl -X GET http://localhost:8080/admin/info
# Should return 401 Unauthorized
```

## Expected Behavior Summary

### Successful Login
- **Request**: POST /admin/login with valid credentials
- **Response**: HTTP 200, code=200, contains JWT token

### Failed Login
- **Request**: POST /admin/login with invalid credentials
- **Response**: HTTP 200, code=401, error message

### Protected Access with Token
- **Request**: GET /admin/info with Authorization: Bearer <token>
- **Response**: HTTP 200, code=200, user data

### Protected Access without Token
- **Request**: GET /admin/info without Authorization header
- **Response**: HTTP 401, code=401

### Invalid Input
- **Request**: POST /admin/login with empty username
- **Response**: HTTP 400 Bad Request

## Troubleshooting

### All tests fail with "Connection refused"
**Cause**: MySQL not running or database not initialized
**Fix**:
```bash
# Start MySQL
brew services start mysql  # macOS
sudo service mysql start   # Linux

# Initialize database
mysql -u root -p < sql/mall_tiny.sql
```

### Tests fail with "User not found"
**Cause**: Default admin user not in database
**Fix**: Check that SQL scripts were executed and admin user exists

### Tests timeout
**Cause**: Server taking too long to start
**Fix**: Increase timeout: `mvn test -Dtest.timeout=120`

### One test passes, others fail
**Cause**: Test isolation issue or database state
**Fix**: Run single test first, then full suite

## Java 17 Specific Validation Points

✅ **Records**: UmsAdminLoginParam (Java 17 record type) serializes/deserializes  
✅ **Generics**: CommonResult<T> with Jackson generic type handling  
✅ **Spring 5.x**: Spring Security with Java 17 modules  
✅ **Lombok**: Works with Java 17 (no issues)  
✅ **JJWT**: JWT library fully supports Java 17  
✅ **MyBatis Plus**: ORM compatible with Java 17  

## Success Criteria Checklist

Before deploying, verify all tests pass:

- [ ] T1: Application health check passes
- [ ] T2: Login returns JWT token
- [ ] T3: Response has correct structure
- [ ] T4: Invalid password rejected
- [ ] T5: Nonexistent user rejected
- [ ] T6: No token = 401 error
- [ ] T7: Invalid token = 401 error
- [ ] T8: Valid token = access granted
- [ ] T9: List endpoint works with auth
- [ ] T10: Empty username rejected
- [ ] T11: Empty password rejected
- [ ] T12: Missing field rejected
- [ ] T13: JSON deserialization works
- [ ] T14: Record serialization works
- [ ] T15: Type errors handled
- [ ] T16: JWT format valid
- [ ] T17: Invalid token rejected
- [ ] T18: Full auth flow works
- [ ] T19: Response codes correct

**All 19 tests pass = Ready for production! ✅**

## Performance Baseline

Running all 19 tests typically takes:
- First run: 15-30 seconds (Spring context initialization)
- Subsequent runs: 5-15 seconds (context cached)

## Integration with CI/CD

```yaml
# Example GitHub Actions
- name: Run API Smoke Tests
  run: mvn clean test -Dtest=MallTinySmokeTests
  
- name: Check Test Results
  if: failure()
  run: |
    echo "Tests failed - check logs above"
    exit 1
```

## Next Steps After Tests Pass

1. ✅ Tests validate Java 17 compatibility
2. 📊 Run performance tests (if applicable)
3. 🔍 Review test logs for warnings
4. 📋 Document any configuration changes
5. 🚀 Deploy to production with Java 17

---

**Test Class Location**: `src/test/java/com/macro/mall/tiny/MallTinySmokeTests.java`  
**Total Tests**: 19  
**Coverage**: Core API endpoints, JWT auth, JSON serialization, Spring Security  
**Status**: ✅ Ready for Java 17 validation
