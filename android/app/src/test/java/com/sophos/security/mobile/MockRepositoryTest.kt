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
}
