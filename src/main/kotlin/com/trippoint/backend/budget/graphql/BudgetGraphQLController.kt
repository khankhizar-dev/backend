package com.trippoint.backend.budget.graphql

import com.trippoint.backend.auth.security.UserPrincipal
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.util.UUID

@Controller
class BudgetGraphQLController(
    private val budgetService: BudgetService
) {

    @QueryMapping
    fun budget(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID
    ): BudgetResponse {

        return BudgetResponse.from(
            budgetService.getBudget(
                userId = principal.userId,
                tripId = tripId
            )
        )
    }

    @QueryMapping
    fun budgetSummary(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID
    ): BudgetSummaryResponse {

        val budget = budgetService.getBudget(
            userId = principal.userId,
            tripId = tripId
        )

        return BudgetSummaryResponse(
            budget = BudgetResponse.from(budget),
            spentAmount = budgetService.getSpentAmount(
                userId = principal.userId,
                tripId = tripId
            ),
            remainingAmount = budgetService.getRemainingAmount(
                userId = principal.userId,
                tripId = tripId
            ),
            expenseCount = budgetService.getExpenseCount(
                userId = principal.userId,
                tripId = tripId
            ),
            categoryBreakdown = budgetService
                .getCategoryBreakdown(principal.userId, tripId)
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
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument input: CreateBudgetInput
    ): BudgetResponse {


        return BudgetResponse.from(
            budgetService.createBudget(
                userId = principal.userId,
                tripId = tripId,
                input = input
            )
        )
    }

    @MutationMapping
    fun updateBudget(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument input: UpdateBudgetInput
    ): BudgetResponse {

        return BudgetResponse.from(
            budgetService.updateBudget(
                userId = principal.userId,
                tripId = tripId,
                input = input
            )
        )
    }

    @QueryMapping
    fun budgetOverview(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID
    ): BudgetOverview {

        return budgetService.getOverview(
            userId = principal.userId,
            tripId = tripId
        )
    }

    @QueryMapping
    fun dailyExpenseReport(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument fromDate: String,
        @Argument toDate: String
    ): List<DailyExpenseReport> {

        return budgetService.getDailyExpenseReport(
            userId = principal.userId,
            tripId = tripId,
            fromDate = LocalDate.parse(fromDate),
            toDate = LocalDate.parse(toDate)
        )
    }

    @QueryMapping
    fun categoryExpenseReport(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID,
        @Argument fromDate: String,
        @Argument toDate: String
    ): List<CategoryExpenseReport> {

        return budgetService.getCategoryExpenseReport(
            userId = principal.userId,
            tripId = tripId,
            fromDate = LocalDate.parse(fromDate),
            toDate = LocalDate.parse(toDate)
        )
    }

    @QueryMapping
    fun settlementSummary(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Argument tripId: UUID
    ): SettlementSummary {

        return budgetService.getSettlementSummary(
            userId = principal.userId,
            tripId = tripId
        )
    }
}