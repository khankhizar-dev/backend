package com.trippoint.backend.checklist.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.checklist.graphql.input.CreateChecklistFromTemplateInput
import com.trippoint.backend.checklist.graphql.input.CreateChecklistTemplateInput
import com.trippoint.backend.checklist.graphql.input.CreateChecklistTemplateItemInput
import com.trippoint.backend.checklist.graphql.input.CreateChecklistTemplateSectionInput
import com.trippoint.backend.checklist.graphql.input.SaveChecklistAsTemplateInput
import com.trippoint.backend.checklist.graphql.input.UpdateChecklistTemplateInput
import com.trippoint.backend.checklist.graphql.input.UpdateChecklistTemplateItemInput
import com.trippoint.backend.checklist.graphql.input.UpdateChecklistTemplateSectionInput
import com.trippoint.backend.checklist.service.ChecklistService
import com.trippoint.backend.checklist.service.ChecklistTemplateService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class ChecklistTemplateGraphQLController(
    private val templateService: ChecklistTemplateService,
    private val checklistService: ChecklistService
) {

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    @QueryMapping
    fun checklistTemplates(
        @AuthenticationPrincipal principal: UserPrincipal
    ): List<ChecklistTemplateResponse> {

        val userId = principal.userId

        return templateService
            .getTemplates(userId)
            .map { template ->
                buildTemplateResponse(
                    userId = userId,
                    template = template
                )
            }
    }

    @QueryMapping
    fun checklistTemplate(
        @Argument templateId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateResponse {

        val userId = principal.userId
        val templateUuid = UUID.fromString(templateId)

        val template = templateService.getTemplate(
            userId = userId,
            templateId = templateUuid
        )

        return buildTemplateResponse(
            userId = userId,
            template = template
        )
    }

    // -------------------------------------------------------------------------
    // TEMPLATE MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun createChecklistTemplate(
        @Argument input: CreateChecklistTemplateInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateResponse {

        val template = templateService.createTemplate(
            userId = principal.userId,
            name = input.name,
            description = input.description
        )

        return template.toResponse(emptyList())
    }

    @MutationMapping
    fun updateChecklistTemplate(
        @Argument templateId: String,
        @Argument input: UpdateChecklistTemplateInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateResponse {

        val template = templateService.updateTemplate(
            userId = principal.userId,
            templateId = UUID.fromString(templateId),
            name = input.name,
            description = input.description
        )

        return buildTemplateResponse(
            userId = principal.userId,
            template = template
        )
    }

    @MutationMapping
    fun archiveChecklistTemplate(
        @Argument templateId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateResponse {

        val template = templateService.archiveTemplate(
            userId = principal.userId,
            templateId = UUID.fromString(templateId)
        )

        return template.toResponse(emptyList())
    }

    // -------------------------------------------------------------------------
    // SECTION MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun createChecklistTemplateSection(
        @Argument templateId: String,
        @Argument input: CreateChecklistTemplateSectionInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateSectionResponse {

        val templateUuid = UUID.fromString(templateId)

        val section = templateService.createSection(
            userId = principal.userId,
            templateId = templateUuid,
            name = input.name
        )

        return buildSectionResponse(
            userId = principal.userId,
            templateId = templateUuid,
            section = section
        )
    }

    @MutationMapping
    fun updateChecklistTemplateSection(
        @Argument templateId: String,
        @Argument sectionId: String,
        @Argument input: UpdateChecklistTemplateSectionInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateSectionResponse {

        val templateUuid = UUID.fromString(templateId)
        val sectionUuid = UUID.fromString(sectionId)

        val section = templateService.updateSection(
            userId = principal.userId,
            templateId = templateUuid,
            sectionId = sectionUuid,
            name = input.name
        )

        return buildSectionResponse(
            userId = principal.userId,
            templateId = templateUuid,
            section = section
        )
    }

    @MutationMapping
    fun deleteChecklistTemplateSection(
        @Argument templateId: String,
        @Argument sectionId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Boolean {

        return templateService.deleteSection(
            userId = principal.userId,
            templateId = UUID.fromString(templateId),
            sectionId = UUID.fromString(sectionId)
        )
    }

    // -------------------------------------------------------------------------
    // ITEM MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun addChecklistTemplateItem(
        @Argument templateId: String,
        @Argument sectionId: String,
        @Argument input: CreateChecklistTemplateItemInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateItemResponse {

        val item = templateService.addItem(
            userId = principal.userId,
            templateId = UUID.fromString(templateId),
            sectionId = UUID.fromString(sectionId),
            name = input.name,
            category = input.category,
            essential = input.essential ?: false
        )

        return item.toResponse()
    }

    @MutationMapping
    fun updateChecklistTemplateItem(
        @Argument templateId: String,
        @Argument sectionId: String,
        @Argument itemId: String,
        @Argument input: UpdateChecklistTemplateItemInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateItemResponse {

        val item = templateService.updateItem(
            userId = principal.userId,
            templateId = UUID.fromString(templateId),
            sectionId = UUID.fromString(sectionId),
            itemId = UUID.fromString(itemId),
            name = input.name,
            category = input.category,
            essential = input.essential
        )

        return item.toResponse()
    }

    @MutationMapping
    fun deleteChecklistTemplateItem(
        @Argument templateId: String,
        @Argument sectionId: String,
        @Argument itemId: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Boolean {

        return templateService.deleteItem(
            userId = principal.userId,
            templateId = UUID.fromString(templateId),
            sectionId = UUID.fromString(sectionId),
            itemId = UUID.fromString(itemId)
        )
    }

    // -------------------------------------------------------------------------
    // CONVERSION MUTATIONS
    // -------------------------------------------------------------------------

    @MutationMapping
    fun saveChecklistAsTemplate(
        @Argument tripId: String,
        @Argument checklistId: String,
        @Argument input: SaveChecklistAsTemplateInput,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistTemplateResponse {

        val template = templateService.saveChecklistAsTemplate(
            userId = principal.userId,
            tripId = UUID.fromString(tripId),
            checklistId = UUID.fromString(checklistId),
            name = input.name,
            description = input.description
        )

        return buildTemplateResponse(
            userId = principal.userId,
            template = template
        )
    }

    @MutationMapping
    fun createChecklistFromTemplate(
        @Argument tripId: String,
        @Argument templateId: String,
        @Argument input: CreateChecklistFromTemplateInput?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ChecklistResponse {

        val userId = principal.userId
        val tripUuid = UUID.fromString(tripId)
        val templateUuid = UUID.fromString(templateId)

        val checklist = templateService.createChecklistFromTemplate(
            userId = userId,
            tripId = tripUuid,
            templateId = templateUuid,
            name = input?.name,
            description = input?.description
        )

        val sections = checklistService.getSections(
            userId = userId,
            tripId = tripUuid,
            checklistId = checklist.id
        ).map { section ->

            val items = checklistService.getItems(
                userId = userId,
                tripId = tripUuid,
                checklistId = checklist.id,
                sectionId = section.id
            )

            val progress = checklistService.getSectionProgress(
                userId = userId,
                tripId = tripUuid,
                sectionId = section.id,
                checklistId = checklist.id)

            section.toResponse(
                items = items,
                progress = progress
            )
        }

        val progress = checklistService.getChecklistProgress(
            userId = userId,
            tripId = tripUuid,
            checklistId = checklist.id,
        )

        return checklist.toResponse(
            sections = sections,
            progress = progress
        )
    }

    // -------------------------------------------------------------------------
    // RESPONSE BUILDERS
    // -------------------------------------------------------------------------

    private fun buildTemplateResponse(
        userId: UUID,
        template: com.trippoint.backend.checklist.entity.ChecklistTemplate
    ): ChecklistTemplateResponse {

        val sections = templateService
            .getSections(
                userId = userId,
                templateId = template.id
            )
            .map { section ->
                buildSectionResponse(
                    userId = userId,
                    templateId = template.id,
                    section = section
                )
            }

        return template.toResponse(sections)
    }

    private fun buildSectionResponse(
        userId: UUID,
        templateId: UUID,
        section: com.trippoint.backend.checklist.entity.ChecklistTemplateSection
    ): ChecklistTemplateSectionResponse {

        val items = templateService.getItems(
            userId = userId,
            templateId = templateId,
            sectionId = section.id
        )

        return section.toResponse(items)
    }
}