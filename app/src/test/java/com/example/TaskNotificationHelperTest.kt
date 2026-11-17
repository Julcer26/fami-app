package com.example

import com.example.database.Chore
import com.example.notifications.TaskNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskNotificationHelperTest {

    @Test
    fun normalizeAlias_stripsDomainAccentsAndCase() {
        assertEquals("jose", TaskNotificationHelper.normalizeAlias("José@somosfami.app"))
        assertEquals("maria", TaskNotificationHelper.normalizeAlias("  María "))
    }

    @Test
    fun isAssignedToAll_recognizesFamilyAliases() {
        assertTrue(TaskNotificationHelper.isAssignedToAll("Todos"))
        assertTrue(TaskNotificationHelper.isAssignedToAll("ALL"))
        assertTrue(TaskNotificationHelper.isAssignedToAll("Familia"))
        assertFalse(TaskNotificationHelper.isAssignedToAll("Ana"))
    }

    @Test
    fun isChoreTargetedToUser_filtersPrivateCompletedAndSelfCreated() {
        val assigned = Chore(
            id = "c1",
            title = "Comprar pan",
            assignedTo = "Ana",
            createdBy = "Julio",
            isPrivate = false,
            isCompleted = false
        )
        assertTrue(TaskNotificationHelper.isChoreTargetedToUser(assigned, "Ana"))
        assertFalse(TaskNotificationHelper.isChoreTargetedToUser(assigned, "Julio"))
        assertFalse(TaskNotificationHelper.isChoreTargetedToUser(assigned.copy(isPrivate = true), "Ana"))
        assertFalse(TaskNotificationHelper.isChoreTargetedToUser(assigned.copy(isCompleted = true), "Ana"))
        assertTrue(TaskNotificationHelper.isChoreTargetedToUser(assigned.copy(assignedTo = "Todos"), "Carlos"))
    }
}
