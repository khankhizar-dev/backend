package com.trippoint.backend.budget.model

import java.math.BigDecimal
import java.util.UUID

data class SettlementSummary(
    val totalExpense: BigDecimal,
    val memberCount: Int,
    val equalShare: BigDecimal,
    val members: List<MemberSettlement>,
    val settlements: List<SettlementTransfer>
)

data class MemberSettlement(
    val userId: UUID,
    val paidAmount: BigDecimal,
    val shareAmount: BigDecimal,
    val balance: BigDecimal
)

data class SettlementTransfer(
    val fromUserId: UUID,
    val toUserId: UUID,
    val amount: BigDecimal
)
