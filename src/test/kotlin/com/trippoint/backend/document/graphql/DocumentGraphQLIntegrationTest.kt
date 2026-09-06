package com.trippoint.backend.document.graphql

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
class DocumentGraphQLIntegrationTest {

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Test
    fun `document query is exposed`() {

        graphQlTester
            .document(
                """
                query {
                    documents(
                        tripId: "00000000-0000-0000-0000-000000000001"
                    ) {
                        id
                        tripId
                        uploadedBy
                        name
                        originalFileName
                        mimeType
                        fileSize
                        category
                        source
                        status
                        description
                        documentNumber
                        issuedBy
                        issuedDate
                        expiryDate
                        favorite
                        createdAt
                        updatedAt
                    }
                }
                """
            )
            .execute()
    }

    @Test
    fun `update document mutation is exposed`() {

        graphQlTester
            .document(
                """
            mutation {
                updateDocument(
                    tripId: "00000000-0000-0000-0000-000000000001"
                    documentId: "00000000-0000-0000-0000-000000000002"
                    input: {
                        name: "Updated Passport"
                        category: PASSPORT_VISA
                        description: "Updated passport document"
                        documentNumber: "P123456"
                        issuedBy: "Government"
                        issuedDate: "2025-01-01"
                        expiryDate: "2035-01-01"
                    }
                ) {
                    id
                    name
                    category
                    description
                    documentNumber
                    issuedBy
                    issuedDate
                    expiryDate
                }
            }
            """
            )
            .execute()
    }

    @Test
    fun `favorite document mutation is exposed`() {

        graphQlTester
            .document(
                """
            mutation {
                favoriteDocument(
                    tripId: "00000000-0000-0000-0000-000000000001"
                    documentId: "00000000-0000-0000-0000-000000000002"
                    favorite: true
                ) {
                    id
                    favorite
                }
            }
            """
            )
            .execute()
    }

    @Test
    fun `trash document mutation is exposed`() {

        graphQlTester
            .document(
                """
            mutation {
                trashDocument(
                    tripId: "00000000-0000-0000-0000-000000000001"
                    documentId: "00000000-0000-0000-0000-000000000002"
                ) {
                    id
                    status
                }
            }
            """
            )
            .execute()
    }

    @Test
    fun `restore document mutation is exposed`() {

        graphQlTester
            .document(
                """
            mutation {
                restoreDocument(
                    tripId: "00000000-0000-0000-0000-000000000001"
                    documentId: "00000000-0000-0000-0000-000000000002"
                ) {
                    id
                    status
                }
            }
            """
            )
            .execute()
    }

    @Test
    fun `permanently delete document mutation is exposed`() {

        graphQlTester
            .document(
                """
            mutation {
                permanentlyDeleteDocument(
                    tripId: "00000000-0000-0000-0000-000000000001"
                    documentId: "00000000-0000-0000-0000-000000000002"
                )
            }
            """
            )
            .execute()
    }
}