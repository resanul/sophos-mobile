package com.sophos.security.mobile

import com.sophos.security.mobile.core.model.CaseStatus
import com.sophos.security.mobile.core.model.HealthState
import com.sophos.security.mobile.core.network.MockSecurityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MockRepositoryTest {

    @Test
    fun testDashboardSummaryCalculation() {
        val summary = MockSecurityRepository.getDashboardSummary()
        assertTrue(summary.totalEndpoints > 0)
        assertEquals(summary.totalEndpoints, summary.healthyEndpoints + summary.suspiciousEndpoints + summary.badEndpoints)
        assertTrue(summary.activeCriticalAlerts >= 0)
        assertTrue(summary.activeEndpoints >= 0)
        assertTrue(summary.activeEndpoints <= summary.totalEndpoints)
    }

    @Test
    fun testEndpointOnlineAndTypeCounts() = runTest {
        val endpoints = MockSecurityRepository.getEndpoints().first()
        val total = endpoints.size
        val servers = endpoints.count { it.isServer }
        val workstations = endpoints.count { !it.isServer }
        val online = endpoints.count { it.online }

        assertEquals(total, servers + workstations)
        assertTrue(online in 0..total)
    }

    @Test
    fun testEndpointIsolationAndRestoration() = runTest {
        val endpointsInitial = MockSecurityRepository.getEndpoints().first()
        val target = endpointsInitial.first { !it.isolated }

        // Test Isolation
        val isolatedResult = MockSecurityRepository.isolateEndpoint(target.id, "Automated unit test containment")
        assertTrue(isolatedResult)

        val endpointsAfterIsolation = MockSecurityRepository.getEndpoints().first()
        val updatedTarget = endpointsAfterIsolation.first { it.id == target.id }
        assertTrue(updatedTarget.isolated)
        assertEquals(HealthState.BAD, updatedTarget.health)

        // Test Restoration
        val restoreResult = MockSecurityRepository.restoreEndpoint(target.id, "Automated unit test recovery")
        assertTrue(restoreResult)

        val endpointsAfterRestoration = MockSecurityRepository.getEndpoints().first()
        val restoredTarget = endpointsAfterRestoration.first { it.id == target.id }
        assertFalse(restoredTarget.isolated)
        assertEquals(HealthState.GOOD, restoredTarget.health)
    }

    @Test
    fun testCaseStatusTransition() = runTest {
        val casesInitial = MockSecurityRepository.getCases().first()
        val testCase = casesInitial.first()

        val updated = MockSecurityRepository.updateCaseStatus(testCase.id, CaseStatus.CONTAINED)
        assertTrue(updated)

        val casesAfter = MockSecurityRepository.getCases().first()
        val modifiedCase = casesAfter.first { it.id == testCase.id }
        assertEquals(CaseStatus.CONTAINED, modifiedCase.status)
    }

    @Test
    fun testCachedDataSerialization() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val testCached = com.sophos.security.mobile.core.model.SophosCachedData(
            tenantId = "test-tenant-123",
            dataRegionUrl = "https://api-us03.central.sophos.com",
            cachedAtTimestamp = 1700000000L
        )
        val encoded = json.encodeToString(com.sophos.security.mobile.core.model.SophosCachedData.serializer(), testCached)
        val decoded = json.decodeFromString(com.sophos.security.mobile.core.model.SophosCachedData.serializer(), encoded)
        assertEquals("test-tenant-123", decoded.tenantId)
        assertEquals("https://api-us03.central.sophos.com", decoded.dataRegionUrl)
    }

    @Test
    fun testEndpointFilterState() {
        MockSecurityRepository.setEndpointFilter("ONLINE")
        assertEquals("ONLINE", MockSecurityRepository.endpointFilter.value)
        MockSecurityRepository.setEndpointFilter("ALL")
        assertEquals("ALL", MockSecurityRepository.endpointFilter.value)
    }
}
