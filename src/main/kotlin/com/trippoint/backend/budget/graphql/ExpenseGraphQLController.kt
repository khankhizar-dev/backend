package com.trippoint.backend.budget.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.budget.graphql.input.CreateExpenseInput
import com.trippoint.backend.budget.graphql.input.ExpenseFilterInput
import com.trippoint.backend.budget.graphql.input.UpdateExpenseInput
import com.trippoint.backend.budget.service.ExpenseService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class ExpenseGraphQLController(
    private val expenseService: ExpenseService
) {

    @QueryMapping
    fun expenses(
        @Argument tripId: UUID,
        @Argument filter: ExpenseFilterInput?,
        authentication: Authentication?
    ): List<ExpenseResponse> {

        val userId = requireUserId(authentication)

        return expenseService
            .getExpenses(
                userId = userId,
                tripId = tripId,
                filter = filter
            )
            .map(ExpenseResponse::from)
    }

    @QueryMapping
    fun expense(
        @Argument tripId: UUID,
        @Argument expenseId: UUID,
        authentication: Authentication?
    ): ExpenseResponse {

        val userId = requireUserId(authentication)

        return ExpenseResponse.from(
            expenseService.getExpense(
                userId = userId,
                tripId = tripId,
                expenseId = expenseId
            )
        )
    }

    @MutationMapping
    fun createExpense(
        @Argument tripId: UUID,
        @Argument input: CreateExpenseInput,
        authentication: Authentication?
    ): ExpenseResponse {

        val userId = requireUserId(authentication)

        return ExpenseResponse.from(
            expenseService.createExpense(
                userId = userId,
                tripId = tripId,
                input = input
            )
        )
    }

    @MutationMapping
    fun updateExpense(
        @Argument tripId: UUID,
        @Argument expenseId: UUID,
        @Argument input: UpdateExpenseInput,
        authentication: Authentication?
    ): ExpenseResponse {

        val userId = requireUserId(authentication)

        return ExpenseResponse.from(
            expenseService.updateExpense(
                userId = userId,
                tripId = tripId,
                expenseId = expenseId,
                input = input
            )
        )
    }

    @MutationMapping
    fun archiveExpense(
        @Argument tripId: UUID,
        @Argument expenseId: UUID,
        authentication: Authentication?
    ): ExpenseResponse {

        val userId = requireUserId(authentication)

        return ExpenseResponse.from(
            expenseService.archiveExpense(
                userId = userId,
                tripId = tripId,
                expenseId = expenseId
            )
        )
    }

    private fun requireUserId(
        authentication: Authentication?
    ): UUID {

        require(authentication?.isAuthenticated == true) {
            "Authentication required"
        }

        val principal = authentication.principal as? UserPrincipal
            ?: throw IllegalArgumentException("Invalid authentication principal")

        return principal.userId
    }
}