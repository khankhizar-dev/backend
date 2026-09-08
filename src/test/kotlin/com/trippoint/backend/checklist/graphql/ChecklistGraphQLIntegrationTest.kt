package com.trippoint.backend.checklist.graphql

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureGraphQlTester
class ChecklistGraphQLIntegrationTest {

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Test
    fun `checklists query is exposed`() {

        graphQlTester
            .document(
                """
                query {
                    checklists(
                        tripId: "00000000-0000-0000-0000-000000000001"
                    ) {
                        id
                        tripId
                        createdBy
                        name
                        description
                        status
                        totalItems
                        completedItems
                        progress
                        sections {
                            id
                            checklistId
                            name
                            position
                            totalItems
                            completedItems
                            progress
                            items {
                                id
                                sectionId
                                createdBy
                                name
                                category
                                essential
                                completed
                                dueDate
                                position
                                createdAt
                                updatedAt
                            }
                            createdAt
                            updatedAt
                        }
                        createdAt
                        updatedAt
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `checklist query is exposed`() {

        graphQlTester
            .document(
                """
                query {
                    checklist(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                    ) {
                        id
                        tripId
                        createdBy
                        name
                        description
                        status
                        totalItems
                        completedItems
                        progress
                        sections {
                            id
                            checklistId
                            name
                            position
                            totalItems
                            completedItems
                            progress
                            items {
                                id
                                sectionId
                                createdBy
                                name
                                category
                                essential
                                completed
                                dueDate
                                position
                                createdAt
                                updatedAt
                            }
                        }
                        createdAt
                        updatedAt
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `create checklist mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    createChecklist(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        input: {
                            name: "Packing List"
                            description: "Things to pack"
                        }
                    ) {
                        id
                        tripId
                        createdBy
                        name
                        description
                        status
                        totalItems
                        completedItems
                        progress
                        sections {
                            id
                            name
                            position
                            totalItems
                            completedItems
                            progress
                        }
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `update checklist mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    updateChecklist(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        input: {
                            name: "Updated Packing List"
                            description: "Updated description"
                        }
                    ) {
                        id
                        name
                        description
                        status
                        totalItems
                        completedItems
                        progress
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `archive checklist mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    archiveChecklist(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                    ) {
                        id
                        name
                        status
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `create checklist section mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    createChecklistSection(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        input: {
                            name: "Documents"
                        }
                    ) {
                        id
                        checklistId
                        name
                        position
                        totalItems
                        completedItems
                        progress
                        items {
                            id
                            name
                            category
                            essential
                            completed
                            position
                        }
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `update checklist section mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    updateChecklistSection(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        sectionId: "00000000-0000-0000-0000-000000000003"
                        input: {
                            name: "Travel Documents"
                        }
                    ) {
                        id
                        checklistId
                        name
                        position
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `delete checklist section mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    deleteChecklistSection(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        sectionId: "00000000-0000-0000-0000-000000000003"
                    )
                }
                """
            )
            .execute()
    }

    @Test
    fun `add checklist item mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    addChecklistItem(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        sectionId: "00000000-0000-0000-0000-000000000003"
                        input: {
                            name: "Passport"
                            category: DOCUMENTS
                            essential: true
                        }
                    ) {
                        id
                        sectionId
                        createdBy
                        name
                        category
                        essential
                        completed
                        dueDate
                        position
                        createdAt
                        updatedAt
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `update checklist item mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    updateChecklistItem(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        sectionId: "00000000-0000-0000-0000-000000000003"
                        itemId: "00000000-0000-0000-0000-000000000004"
                        input: {
                            name: "Passport Copy"
                            category: DOCUMENTS
                            essential: true
                        }
                    ) {
                        id
                        sectionId
                        name
                        category
                        essential
                        completed
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `complete checklist item mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    completeChecklistItem(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        sectionId: "00000000-0000-0000-0000-000000000003"
                        itemId: "00000000-0000-0000-0000-000000000004"
                        completed: true
                    ) {
                        id
                        name
                        completed
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `delete checklist item mutation is exposed`() {

        graphQlTester
            .document(
                """
                mutation {
                    deleteChecklistItem(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        sectionId: "00000000-0000-0000-0000-000000000003"
                        itemId: "00000000-0000-0000-0000-000000000004"
                    )
                }
                """
            )
            .execute()
    }
}