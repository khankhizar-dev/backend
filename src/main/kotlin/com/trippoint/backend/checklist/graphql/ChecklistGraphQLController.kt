package com.trippoint.backend.checklist.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.checklist.graphql.input.CreateChecklistInput
import com.trippoint.backend.checklist.graphql.input.CreateChecklistItemInput
import com.trippoint.backend.checklist.graphql.input.CreateChecklistSectionInput
import com.trippoint.backend.checklist.graphql.input.UpdateChecklistInput
import com.trippoint.backend.checklist.graphql.input.UpdateChecklistItemInput
import com.trippoint.backend.checklist.graphql.input.UpdateChecklistSectionInput
import com.trippoint.backend.checklist.service.ChecklistService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import java.util.UUID

@Controller
class ChecklistGraphQLController(
    private val checklistService: ChecklistService
) {

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    @QueryMapping
    fun checklists(
        @Argument tripId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): List<ChecklistResponse> {

        val userId = principal.userId
        val tripUuid = UUID.fromString(tripId)

        return checklistService
            .getChecklists(
                userId = userId,
                tripId = tripUuid
            )
            .map { checklist ->

                val sections = buildSections(
                    userId = userId,
                    tripId = tripUuid,
                    checklistId = checklist.id
                )

                val progress =
                    checklistService.getChecklistProgress(
                        checklistId = checklist.id
                    )

                checklist.toResponse(
                    sections = sections,
                    progress = progress
                )
            }
    }

    @QueryMapping
    fun checklist(
        @Argument tripId: String,
        @Argument checklistId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistResponse {

        val userId = principal.userId
        val tripUuid = UUID.fromString(tripId)
        val checklistUuid = UUID.fromString(checklistId)

        val checklist = checklistService.getChecklist(
            userId = userId,
            tripId = tripUuid,
            checklistId = checklistUuid
        )

        val sections = buildSections(
            userId = userId,
            tripId = tripUuid,
            checklistId = checklistUuid
        )

        val progress =
            checklistService.getChecklistProgress(
                checklistId = checklistUuid
            )

        return checklist.toResponse(
            sections = sections,
            progress = progress
        )
    }

    // -------------------------------------------------------------------------
    // CHECKLIST MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun createChecklist(
        @Argument tripId: String,
        @Argument input: CreateChecklistInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistResponse {

        val tripUuid = UUID.fromString(tripId)

        val checklist = checklistService.createChecklist(
            userId = principal.userId,
            tripId = tripUuid,
            name = input.name,
            description = input.description
        )

        return checklist.toResponse(
            sections = emptyList(),
            progress = checklistService.getChecklistProgress(checklist.id)
        )
    }

    @MutationMapping
    fun updateChecklist(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument input: UpdateChecklistInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistResponse {

        val tripUuid = UUID.fromString(tripId)
        val checklistUuid = UUID.fromString(checklistId)

        val checklist = checklistService.updateChecklist(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid,
            name = input.name,
            description = input.description
        )

        return buildChecklistResponse(
            userId = principal.userId,
            tripId = tripUuid,
            checklist = checklist
        )
    }

    @MutationMapping
    fun archiveChecklist(
        @Argument tripId: String,
        @Argument checklistId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistResponse {

        val tripUuid = UUID.fromString(tripId)
        val checklistUuid = UUID.fromString(checklistId)

        val checklist = checklistService.archiveChecklist(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid
        )

        return buildChecklistResponse(
            userId = principal.userId,
            tripId = tripUuid,
            checklist = checklist
        )
    }

    // -------------------------------------------------------------------------
    // SECTION MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun createChecklistSection(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument input: CreateChecklistSectionInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistSectionResponse {

        val tripUuid = UUID.fromString(tripId)
        val checklistUuid = UUID.fromString(checklistId)

        val section = checklistService.createSection(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid,
            name = input.name
        )

        return buildSectionResponse(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid,
            section = section
        )
    }

    @MutationMapping
    fun updateChecklistSection(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument sectionId: String,
        @Argument input: UpdateChecklistSectionInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistSectionResponse {

        val tripUuid = UUID.fromString(tripId)
        val checklistUuid = UUID.fromString(checklistId)
        val sectionUuid = UUID.fromString(sectionId)

        val section = checklistService.updateSection(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid,
            sectionId = sectionUuid,
            name = input.name
        )

        return buildSectionResponse(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid,
            section = section
        )
    }

    @MutationMapping
    fun deleteChecklistSection(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument sectionId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Boolean {

        return checklistService.deleteSection(
            userId = principal.userId,
            tripId = UUID.fromString(tripId),
            checklistId = UUID.fromString(checklistId),
            sectionId = UUID.fromString(sectionId)
        )
    }

    // -------------------------------------------------------------------------
    // ITEM MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun addChecklistItem(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument sectionId: String,
        @Argument input: CreateChecklistItemInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistItemResponse {

        val tripUuid = UUID.fromString(tripId)
        val checklistUuid = UUID.fromString(checklistId)
        val sectionUuid = UUID.fromString(sectionId)

        val dueDate = input.dueDate?.let {
            LocalDateTime.parse(it)
        }

        val item = checklistService.addItem(
            userId = principal.userId,
            tripId = tripUuid,
            checklistId = checklistUuid,
            sectionId = sectionUuid,
            name = input.name,
            category = input.category,
            essential = input.essential ?: false,
            dueDate = dueDate
        )

        return item.toResponse()
    }

    @MutationMapping
    fun updateChecklistItem(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument sectionId: String,
        @Argument itemId: String,
        @Argument input: UpdateChecklistItemInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistItemResponse {

        val item = checklistService.updateItem(
            userId = principal.userId,
            tripId = UUID.fromString(tripId),
            checklistId = UUID.fromString(checklistId),
            sectionId = UUID.fromString(sectionId),
            itemId = UUID.fromString(itemId),
            name = input.name,
            category = input.category,
            essential = input.essential,
            dueDate = input.dueDate?.let(LocalDateTime::parse)
        )

        return item.toResponse()
    }

    @MutationMapping
    fun completeChecklistItem(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument sectionId: String,
        @Argument itemId: String,
        @Argument completed: Boolean,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistItemResponse {

        val item = checklistService.completeItem(
            userId = principal.userId,
            tripId = UUID.fromString(tripId),
            checklistId = UUID.fromString(checklistId),
            sectionId = UUID.fromString(sectionId),
            itemId = UUID.fromString(itemId),
            completed = completed
        )

        return item.toResponse()
    }

    @MutationMapping
    fun deleteChecklistItem(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument sectionId: String,
        @Argument itemId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Boolean {

        return checklistService.deleteItem(
            userId = principal.userId,
            tripId = UUID.fromString(tripId),
            checklistId = UUID.fromString(checklistId),
            sectionId = UUID.fromString(sectionId),
            itemId = UUID.fromString(itemId)
        )
    }

    // -------------------------------------------------------------------------
    // RESPONSE BUILDERS
    // -------------------------------------------------------------------------

    private fun buildChecklistResponse(
        userId: UUID,
        tripId: UUID,
        checklist: com.trippoint.backend.checklist.entity.Checklist
    ): ChecklistResponse {

        val sections = buildSections(
            userId = userId,
            tripId = tripId,
            checklistId = checklist.id
        )

        val progress =
            checklistService.getChecklistProgress(checklist.id)

        return checklist.toResponse(
            sections = sections,
            progress = progress
        )
    }

    private fun buildSections(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID
    ): List<ChecklistSectionResponse> {

        return checklistService
            .getSections(
                userId = userId,
                tripId = tripId,
                checklistId = checklistId
            )
            .map { section ->
                buildSectionResponse(
                    userId = userId,
                    tripId = tripId,
                    checklistId = checklistId,
                    section = section
                )
            }
    }

    private fun buildSectionResponse(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        section: com.trippoint.backend.checklist.entity.ChecklistSection
    ): ChecklistSectionResponse {

        val items = checklistService.getItems(
            userId = userId,
            tripId = tripId,
            checklistId = checklistId,
            sectionId = section.id
        )

        val progress =
            checklistService.getSectionProgress(section.id)

        return section.toResponse(
            items = items,
            progress = progress
        )
    }
}