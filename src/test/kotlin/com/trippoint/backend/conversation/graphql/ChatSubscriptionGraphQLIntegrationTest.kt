package com.trippoint.backend.conversation.graphql

import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import com.trippoint.backend.auth.service.JwtService
import com.trippoint.backend.conversation.entity.ChatMessage
import com.trippoint.backend.conversation.event.ChatMessageCreatedEvent
import com.trippoint.backend.conversation.model.MessageType
import com.trippoint.backend.conversation.service.ChatSubscriptionService
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.test.tester.WebSocketGraphQlTester
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ChatSubscriptionGraphQLIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var tripRepository: TripRepository

    @Autowired
    lateinit var tripMemberRepository: TripMemberRepository

    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var chatSubscriptionService: ChatSubscriptionService

    @Test
    fun `authenticated user can subscribe to trip messages`() {

        val user = userRepository.save(
            User(
                email = "chat-${UUID.randomUUID()}@example.com",
                passwordHash = "test-password",
                firstName = "Chat",
                lastName = "Tester",
                active = true,
                emailVerified = true
            )
        )

        val userId = requireNotNull(user.id)

        val trip = tripRepository.save(
            Trip(
                ownerId = userId,
                name = "Subscription Test Trip",
                destination = "Dubai",
                startDate = LocalDate.of(2026, 10, 15),
                endDate = LocalDate.of(2026, 10, 20)
            )
        )

        tripMemberRepository.save(
            TripMember(
                tripId = trip.id,
                userId = userId,
                role = TripMemberRole.OWNER,
                status = TripMemberStatus.ACCEPTED
            )
        )

        val token = jwtService.generateToken(userId)

        val webSocketClient =
            ReactorNettyWebSocketClient(
                HttpClient.create()
            )

        val graphQlTester =
            WebSocketGraphQlTester.builder(
                URI.create(
                    "ws://localhost:$port/graphql/ws"
                ),
                webSocketClient
            )
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .build()

        graphQlTester.start().block()

        try {
            // Subscription implementation will be asserted here.
        } finally {
            graphQlTester.stop().block()
        }
    }
}