package com.trippoint.backend.checklist

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.test.context.ActiveProfiles
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureGraphQlTester
class ChecklistTemplateGraphQLIntegrationTest {

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Test
    fun `checklist template query is exposed`() {
        graphQlTester
            .document(
                """
                query {
                    checklistTemplates {
                        id
                        name
                        description
                        type
                        status
                        createdAt
                        updatedAt
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `checklist template query by id is exposed`() {
        graphQlTester
            .document(
                """
                query {
                    checklistTemplate(templateId: "00000000-0000-0000-0000-000000000001") {
                        id
                        name
                        description
                        type
                        status
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `create checklist template mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    createChecklistTemplate(
                        input: {
                            name: "Travel Essentials"
                            description: "Basic travel checklist"
                        }
                    ) {
                        id
                        name
                        description
                        type
                        status
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `update checklist template mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    updateChecklistTemplate(
                        templateId: "00000000-0000-0000-0000-000000000001"
                        input: {
                            name: "Updated Travel Essentials"
                        }
                    ) {
                        id
                        name
                        description
                        type
                        status
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `archive checklist template mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    archiveChecklistTemplate(
                        templateId: "00000000-0000-0000-0000-000000000001"
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
    fun `create checklist template section mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    createChecklistTemplateSection(
                        templateId: "00000000-0000-0000-0000-000000000001"
                        input: {
                            name: "Documents"
                        }
                    ) {
                        id
                        templateId
                        name
                        position
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `add checklist template item mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    addChecklistTemplateItem(
                        templateId: "00000000-0000-0000-0000-000000000001"
                        sectionId: "00000000-0000-0000-0000-000000000002"
                        input: {
                            name: "Passport"
                            category: DOCUMENTS
                            essential: true
                        }
                    ) {
                        id
                        sectionId
                        name
                        category
                        essential
                        position
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `save checklist as template mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    saveChecklistAsTemplate(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        checklistId: "00000000-0000-0000-0000-000000000002"
                        input: {
                            name: "My Trip Template"
                            description: "Saved from trip checklist"
                        }
                    ) {
                        id
                        name
                        type
                        status
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `create checklist from template mutation is exposed`() {
        graphQlTester
            .document(
                """
                mutation {
                    createChecklistFromTemplate(
                        tripId: "00000000-0000-0000-0000-000000000001"
                        templateId: "00000000-0000-0000-0000-000000000002"
                        input: {
                            name: "My New Checklist"
                        }
                    ) {
                        id
                        tripId
                        name
                        status
                    }
                }
                """
            )
            .execute()
    }
}