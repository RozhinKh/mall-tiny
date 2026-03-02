package com.macro.mall.tiny;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.macro.mall.tiny.modules.ums.mapper.UmsAdminMapper;
import com.macro.mall.tiny.modules.ums.mapper.UmsRoleMapper;
import com.macro.mall.tiny.modules.ums.model.UmsAdmin;
import com.macro.mall.tiny.modules.ums.model.UmsRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Operations and Transactions Tests for Java 17 Compatibility
 * Tests MyBatis-Plus ORM operations, transaction handling, connection pooling via Druid,
 * and core CRUD functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DisplayName("Database Operations and Transactions Tests (Java 17)")
public class DatabaseOperationsTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UmsAdminMapper umsAdminMapper;

    @Autowired
    private UmsRoleMapper umsRoleMapper;

    // ==================== Connection Pool Tests ====================

    @Test
    @DisplayName("D1: Verify Druid connection pool initializes successfully")
    void testConnectionPoolInitialization() {
        assertNotNull(dataSource, "DataSource should not be null");
        assertTrue(dataSource instanceof DruidDataSource, "DataSource should be DruidDataSource");
        
        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        assertTrue(druidDataSource.isEnable(), "Connection pool should be enabled");
        
        System.out.println("[✓] D1 PASSED: Druid connection pool initialized successfully");
    }

    @Test
    @DisplayName("D2: Verify connection pool can acquire a connection")
    void testConnectionPoolAcquisition() throws Exception {
        assertNotNull(dataSource, "DataSource should not be null");
        var connection = dataSource.getConnection();
        assertNotNull(connection, "Should be able to acquire a connection");
        assertFalse(connection.isClosed(), "Connection should be open");
        
        // Close the connection
        connection.close();
        assertTrue(connection.isClosed(), "Connection should be closed after closing");
        
        System.out.println("[✓] D2 PASSED: Connection pool acquisition and release works correctly");
    }

    @Test
    @DisplayName("D3: Verify Druid connection pool health check")
    void testConnectionPoolHealth() {
        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        
        // Get pool statistics
        int activeCount = druidDataSource.getActiveCount();
        int poolingCount = druidDataSource.getPoolingCount();
        
        assertTrue(activeCount >= 0, "Active connection count should be non-negative");
        assertTrue(poolingCount >= 0, "Pooling connection count should be non-negative");
        
        System.out.println("[✓] D3 PASSED: Connection pool health check passed - Active: " + 
                         activeCount + ", Pooling: " + poolingCount);
    }

    // ==================== SELECT (READ) Operations Tests ====================

    @Test
    @DisplayName("D4: Test SELECT operation - Query all records")
    void testSelectAllRecords() {
        List<UmsAdmin> admins = umsAdminMapper.selectList(null);
        
        assertNotNull(admins, "Query result should not be null");
        assertFalse(admins.isEmpty(), "Should have admin records in database");
        assertTrue(admins.size() > 0, "Should retrieve multiple records");
        
        System.out.println("[✓] D4 PASSED: SELECT query returned " + admins.size() + " records");
    }

    @Test
    @DisplayName("D5: Test SELECT operation - Query by ID")
    void testSelectById() {
        // Query existing admin by ID (from SQL file, ID 3 is 'admin')
        UmsAdmin admin = umsAdminMapper.selectById(3L);
        
        assertNotNull(admin, "Should retrieve admin with ID 3");
        assertEquals(3L, admin.getId(), "Admin ID should be 3");
        assertEquals("admin", admin.getUsername(), "Username should be 'admin'");
        assertNotNull(admin.getPassword(), "Password should not be null");
        
        System.out.println("[✓] D5 PASSED: SELECT by ID returned correct record: " + admin.getUsername());
    }

    @Test
    @DisplayName("D6: Test SELECT operation - Query with conditions (QueryWrapper)")
    void testSelectWithConditions() {
        QueryWrapper<UmsAdmin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", "admin");
        
        UmsAdmin admin = umsAdminMapper.selectOne(queryWrapper);
        
        assertNotNull(admin, "Should find admin with username='admin'");
        assertEquals("admin", admin.getUsername(), "Username should match");
        assertEquals(3L, admin.getId(), "Should get correct admin ID");
        
        System.out.println("[✓] D6 PASSED: Conditional SELECT query returned correct record");
    }

    @Test
    @DisplayName("D7: Test SELECT operation - Query with multiple conditions")
    void testSelectWithMultipleConditions() {
        QueryWrapper<UmsAdmin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", "admin")
                    .eq("status", 1);
        
        UmsAdmin admin = umsAdminMapper.selectOne(queryWrapper);
        
        assertNotNull(admin, "Should find active admin with username='admin'");
        assertEquals("admin", admin.getUsername(), "Username should match");
        assertEquals(1, admin.getStatus(), "Status should be 1 (active)");
        
        System.out.println("[✓] D7 PASSED: Multi-condition SELECT query works correctly");
    }

    // ==================== INSERT Operations Tests ====================

    @Test
    @DisplayName("D8: Test INSERT operation - Create new admin record")
    @Transactional
    void testInsertNewRecord() {
        UmsAdmin newAdmin = new UmsAdmin();
        newAdmin.setUsername("testdb_user_" + System.currentTimeMillis());
        newAdmin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe"); // bcrypt hashed password
        newAdmin.setEmail("testdb@example.com");
        newAdmin.setNickName("Test DB User");
        newAdmin.setCreateTime(new Date());
        newAdmin.setStatus(1);
        
        int insertResult = umsAdminMapper.insert(newAdmin);
        
        assertTrue(insertResult > 0, "Insert should return positive result");
        assertNotNull(newAdmin.getId(), "Generated ID should be assigned to entity");
        assertTrue(newAdmin.getId() > 0, "ID should be auto-generated");
        
        // Verify inserted record exists
        UmsAdmin inserted = umsAdminMapper.selectById(newAdmin.getId());
        assertNotNull(inserted, "Inserted record should be retrievable");
        assertEquals(newAdmin.getUsername(), inserted.getUsername(), "Username should match");
        
        System.out.println("[✓] D8 PASSED: INSERT operation created record with ID: " + newAdmin.getId());
    }

    @Test
    @DisplayName("D9: Test INSERT operation - Batch insert multiple records")
    @Transactional
    void testBatchInsert() {
        long timestamp = System.currentTimeMillis();
        
        UmsAdmin admin1 = new UmsAdmin();
        admin1.setUsername("batch_user_1_" + timestamp);
        admin1.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin1.setEmail("batch1@example.com");
        admin1.setNickName("Batch User 1");
        admin1.setCreateTime(new Date());
        admin1.setStatus(1);
        
        UmsAdmin admin2 = new UmsAdmin();
        admin2.setUsername("batch_user_2_" + timestamp);
        admin2.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin2.setEmail("batch2@example.com");
        admin2.setNickName("Batch User 2");
        admin2.setCreateTime(new Date());
        admin2.setStatus(1);
        
        int result1 = umsAdminMapper.insert(admin1);
        int result2 = umsAdminMapper.insert(admin2);
        
        assertTrue(result1 > 0, "First insert should succeed");
        assertTrue(result2 > 0, "Second insert should succeed");
        
        // Verify both records exist
        UmsAdmin inserted1 = umsAdminMapper.selectById(admin1.getId());
        UmsAdmin inserted2 = umsAdminMapper.selectById(admin2.getId());
        assertNotNull(inserted1, "First inserted record should exist");
        assertNotNull(inserted2, "Second inserted record should exist");
        
        System.out.println("[✓] D9 PASSED: Batch INSERT created records with IDs: " + 
                         admin1.getId() + ", " + admin2.getId());
    }

    // ==================== UPDATE Operations Tests ====================

    @Test
    @DisplayName("D10: Test UPDATE operation - Update existing record")
    @Transactional
    void testUpdateRecord() {
        // Create a record first
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("update_test_" + System.currentTimeMillis());
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setEmail("update@example.com");
        admin.setNickName("Original Nick");
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        umsAdminMapper.insert(admin);
        Long adminId = admin.getId();
        
        // Update the record
        admin.setNickName("Updated Nick");
        admin.setEmail("updated@example.com");
        
        int updateResult = umsAdminMapper.updateById(admin);
        
        assertTrue(updateResult > 0, "Update should return positive result");
        
        // Verify update
        UmsAdmin updated = umsAdminMapper.selectById(adminId);
        assertNotNull(updated, "Updated record should exist");
        assertEquals("Updated Nick", updated.getNickName(), "NickName should be updated");
        assertEquals("updated@example.com", updated.getEmail(), "Email should be updated");
        
        System.out.println("[✓] D10 PASSED: UPDATE operation modified record successfully");
    }

    @Test
    @DisplayName("D11: Test UPDATE operation - Update with QueryWrapper")
    @Transactional
    void testUpdateWithConditions() {
        // Create a record first
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("update_cond_" + System.currentTimeMillis());
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setEmail("updatecond@example.com");
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        umsAdminMapper.insert(admin);
        
        // Update via QueryWrapper
        QueryWrapper<UmsAdmin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", admin.getId());
        
        UmsAdmin updateData = new UmsAdmin();
        updateData.setStatus(0); // Disable the account
        
        int updateResult = umsAdminMapper.update(updateData, queryWrapper);
        
        assertTrue(updateResult > 0, "Update should return positive result");
        
        // Verify update
        UmsAdmin updated = umsAdminMapper.selectById(admin.getId());
        assertEquals(0, updated.getStatus(), "Status should be updated to 0");
        
        System.out.println("[✓] D11 PASSED: UPDATE with conditions executed successfully");
    }

    // ==================== DELETE Operations Tests ====================

    @Test
    @DisplayName("D12: Test DELETE operation - Delete record by ID")
    @Transactional
    void testDeleteById() {
        // Create a record first
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("delete_test_" + System.currentTimeMillis());
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setEmail("delete@example.com");
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        umsAdminMapper.insert(admin);
        Long adminId = admin.getId();
        
        // Verify record exists
        UmsAdmin before = umsAdminMapper.selectById(adminId);
        assertNotNull(before, "Record should exist before deletion");
        
        // Delete the record
        int deleteResult = umsAdminMapper.deleteById(adminId);
        
        assertTrue(deleteResult > 0, "Delete should return positive result");
        
        // Verify record is deleted
        UmsAdmin after = umsAdminMapper.selectById(adminId);
        assertNull(after, "Record should not exist after deletion");
        
        System.out.println("[✓] D12 PASSED: DELETE operation removed record successfully");
    }

    @Test
    @DisplayName("D13: Test DELETE operation - Delete with QueryWrapper")
    @Transactional
    void testDeleteWithConditions() {
        // Create a record first
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("delete_cond_" + System.currentTimeMillis());
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setEmail("deletecond@example.com");
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        umsAdminMapper.insert(admin);
        Long adminId = admin.getId();
        
        // Delete via QueryWrapper
        QueryWrapper<UmsAdmin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", adminId);
        
        int deleteResult = umsAdminMapper.delete(queryWrapper);
        
        assertTrue(deleteResult > 0, "Delete should return positive result");
        
        // Verify deletion
        UmsAdmin deleted = umsAdminMapper.selectById(adminId);
        assertNull(deleted, "Record should not exist after deletion");
        
        System.out.println("[✓] D13 PASSED: DELETE with conditions executed successfully");
    }

    // ==================== Data Integrity Tests ====================

    @Test
    @DisplayName("D14: Test data integrity - Verify inserted values match retrieved values")
    @Transactional
    void testDataIntegrity() {
        String username = "integrity_test_" + System.currentTimeMillis();
        String email = "integrity@example.com";
        String nickName = "Integrity Test";
        
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername(username);
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setEmail(email);
        admin.setNickName(nickName);
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        umsAdminMapper.insert(admin);
        
        // Retrieve and verify
        UmsAdmin retrieved = umsAdminMapper.selectById(admin.getId());
        
        assertEquals(username, retrieved.getUsername(), "Username should match exactly");
        assertEquals(email, retrieved.getEmail(), "Email should match exactly");
        assertEquals(nickName, retrieved.getNickName(), "NickName should match exactly");
        assertEquals(1, retrieved.getStatus(), "Status should match exactly");
        
        System.out.println("[✓] D14 PASSED: Data integrity verified - all values match");
    }

    @Test
    @DisplayName("D15: Test data integrity - Updates persist correctly")
    @Transactional
    void testUpdateDataIntegrity() {
        // Create
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("update_integrity_" + System.currentTimeMillis());
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setEmail("original@example.com");
        admin.setNickName("Original");
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        umsAdminMapper.insert(admin);
        
        // Update
        admin.setEmail("updated@example.com");
        admin.setNickName("Updated");
        admin.setStatus(0);
        
        umsAdminMapper.updateById(admin);
        
        // Verify
        UmsAdmin retrieved = umsAdminMapper.selectById(admin.getId());
        assertEquals("updated@example.com", retrieved.getEmail(), "Updated email should persist");
        assertEquals("Updated", retrieved.getNickName(), "Updated nickname should persist");
        assertEquals(0, retrieved.getStatus(), "Updated status should persist");
        
        System.out.println("[✓] D15 PASSED: Update data integrity verified");
    }

    // ==================== MyBatis-Plus Specific Tests ====================

    @Test
    @DisplayName("D16: Test MyBatis-Plus reflection introspection (Java 17 compatibility)")
    void testMyBatisPlusReflection() {
        // This test verifies that MyBatis-Plus can successfully introspect entities under Java 17
        List<UmsAdmin> admins = umsAdminMapper.selectList(null);
        
        assertNotNull(admins, "Query should succeed with MyBatis-Plus reflection");
        
        // Check that all fields are properly mapped
        for (UmsAdmin admin : admins) {
            assertNotNull(admin.getId(), "ID field should be mapped");
            assertNotNull(admin.getUsername(), "Username field should be mapped");
            // Note: other fields might be null if not set in DB
        }
        
        System.out.println("[✓] D16 PASSED: MyBatis-Plus reflection works correctly on Java 17");
    }

    @Test
    @DisplayName("D17: Test MyBatis-Plus auto-increment ID generation (Java 17 compatibility)")
    @Transactional
    void testAutoIncrementId() {
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("autoinc_test_" + System.currentTimeMillis());
        admin.setPassword("$2a$10$N5YvOKNjFLcYfW2sKZ.eCO7ZPX2fHvDSxPX5T8X5Xdx9L8bX7mYVe");
        admin.setCreateTime(new Date());
        admin.setStatus(1);
        
        // ID should be null before insert
        assertNull(admin.getId(), "ID should be null before insert");
        
        umsAdminMapper.insert(admin);
        
        // ID should be assigned after insert
        assertNotNull(admin.getId(), "ID should be assigned after insert");
        assertTrue(admin.getId() > 0, "ID should be positive");
        
        System.out.println("[✓] D17 PASSED: MyBatis-Plus auto-increment ID works correctly");
    }

    @Test
    @DisplayName("D18: Test multiple mapper classes work correctly (Java 17 compatibility)")
    void testMultipleMappers() {
        // Test UmsAdminMapper
        List<UmsAdmin> admins = umsAdminMapper.selectList(null);
        assertNotNull(admins, "UmsAdminMapper should work");
        
        // Test UmsRoleMapper
        List<UmsRole> roles = umsRoleMapper.selectList(null);
        assertNotNull(roles, "UmsRoleMapper should work");
        
        assertTrue(admins.size() > 0, "Should have admin records");
        assertTrue(roles.size() > 0, "Should have role records");
        
        System.out.println("[✓] D18 PASSED: Multiple mapper classes work correctly");
    }

}
