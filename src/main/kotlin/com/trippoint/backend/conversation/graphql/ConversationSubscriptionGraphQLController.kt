package com.trippoint.backend.conversation.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.conversation.entity.ChatMessage
import com.trippoint.backend.conversation.service.ChatSubscriptionService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.util.UUID

@Controller
class ConversationSubscriptionGraphQLController(
    private val chatSubscriptionService: ChatSubscriptionService
) {

    @SubscriptionMapping
    fun messageReceived(
        @Argument tripId: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Flux<ChatMessage> {

        return chatSubscriptionService.subscribe(
            userId = principal.userId,
            tripId = tripId
        )
    }
}