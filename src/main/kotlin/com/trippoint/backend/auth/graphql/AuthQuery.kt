package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.UserResponse
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AuthQuery {

    @QueryMapping
    fun me(): UserResponse? {
        // TODO: Get current user from security context
        return null
    }
}
