package com.trippoint.backend.budget.graphql

import com.trippoint.backend.budget.entity.BudgetOverview
import com.trippoint.backend.budget.graphql.input.CreateBudgetInput
import com.trippoint.backend.budget.graphql.input.UpdateBudgetInput
import com.trippoint.backend.budget.model.CategoryExpenseReport
import com.trippoint.backend.budget.model.DailyExpenseReport
import com.trippoint.backend.budget.model.SettlementSummary
import com.trippoint.backend.budget.service.BudgetService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.util.UUID

@Controller
class BudgetGraphQLController(
    private val budgetService: BudgetService
) {

    @QueryMapping
    fun budget(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): BudgetResponse {

        val userId = requireUserId(authentication)

        return BudgetResponse.from(
            budgetService.getBudget(
                userId = userId,
                tripId = tripId
            )
        )
    }

    @QueryMapping
    fun budgetSummary(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): BudgetSummaryResponse {

        val userId = requireUserId(authentication)

        val budget = budgetService.getBudget(
            userId = userId,
            tripId = tripId
        )

        return BudgetSummaryResponse(
            budget = BudgetResponse.from(budget),
            spentAmount = budgetService.getSpentAmount(
                userId = userId,
                tripId = tripId
            ),
            remainingAmount = budgetService.getRemainingAmount(
                userId = userId,
                tripId = tripId
            ),
            expenseCount = budgetService.getExpenseCount(
                userId = userId,
                tripId = tripId
            ),
            categoryBreakdown = budgetService
                .getCategoryBreakdown(userId, tripId)
                .map { (category, amount) ->
                    CategorySpendingResponse(
                        category = category,
                        amount = amount
                    )
                }
        )
    }

    @MutationMapping
    fun createBudget(
        @Argument tripId: UUID,
        @Argument input: CreateBudgetInput,
        authentication: Authentication?
    ): BudgetResponse {

        val userId = requireUserId(authentication)

        return BudgetResponse.from(
            budgetService.createBudget(
                userId = userId,
                tripId = tripId,
                input = input
            )
        )
    }

    @MutationMapping
    fun updateBudget(
        @Argument tripId: UUID,
        @Argument input: UpdateBudgetInput,
        authentication: Authentication?
    ): BudgetResponse {

        val userId = requireUserId(authentication)

        return BudgetResponse.from(
            budgetService.updateBudget(
                userId = userId,
                tripId = tripId,
                input = input
            )
        )
    }

    @QueryMapping
    fun budgetOverview(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): BudgetOverview {

        val userId = requireUserId(authentication)

        return budgetService.getOverview(
            userId = userId,
            tripId = tripId
        )
    }

    @QueryMapping
    fun dailyExpenseReport(
        @Argument tripId: UUID,
        @Argument fromDate: String,
        @Argument toDate: String,
        authentication: Authentication?
    ): List<DailyExpenseReport> {

        val userId = requireUserId(authentication)

        return budgetService.getDailyExpenseReport(
            userId = userId,
            tripId = tripId,
            fromDate = LocalDate.parse(fromDate),
            toDate = LocalDate.parse(toDate)
        )
    }

    private fun requireUserId(
        authentication: Authentication?
    ): UUID {

        require(authentication?.isAuthenticated == true) {
            "Authentication required"
        }

        return UUID.fromString(authentication.name)
    }

    @QueryMapping
    fun categoryExpenseReport(
        @Argument tripId: UUID,
        @Argument fromDate: String,
        @Argument toDate: String,
        authentication: Authentication?
    ): List<CategoryExpenseReport> {

        val userId = requireUserId(authentication)

        return budgetService.getCategoryExpenseReport(
            userId = userId,
            tripId = tripId,
            fromDate = LocalDate.parse(fromDate),
            toDate = LocalDate.parse(toDate)
        )
    }

    @QueryMapping
    fun settlementSummary(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): SettlementSummary {

        val userId = requireUserId(authentication)

        return budgetService.getSettlementSummary(
            userId = userId,
            tripId = tripId
        )
    }
}