package com.trippoint.backend.booking.graphql

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureGraphQlTester
class BookingGraphQLIntegrationTest {

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Test
    fun `booking query is exposed`() {

        graphQlTester
            .document(
                """
                query {
                    bookings(
                        tripId: "00000000-0000-0000-0000-000000000001"
                    ) {
                        id
                        tripId
                        type
                        status
                        title
                        provider
                        bookingReference
                    }
                }
                """
            )
            .execute()
    }
}