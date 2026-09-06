package com.trippoint.backend.document.service

import com.trippoint.backend.document.domain.Document
import com.trippoint.backend.document.domain.DocumentCategory
import com.trippoint.backend.document.domain.DocumentSource
import com.trippoint.backend.document.domain.DocumentStatus
import com.trippoint.backend.document.repository.DocumentRepository
import com.trippoint.backend.document.storage.DocumentStorageService
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class DocumentServiceTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var documentStorageService: DocumentStorageService
    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository
    private lateinit var tripAccessService: TripAccessService

    private lateinit var service: DocumentService

    private val tripId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val otherMemberId = UUID.randomUUID()
    private val nonMemberId = UUID.randomUUID()
    private val documentId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        documentRepository = mock(DocumentRepository::class.java)
        documentStorageService = mock(DocumentStorageService::class.java)

        tripRepository = mock(TripRepository::class.java)
        tripMemberRepository = mock(TripMemberRepository::class.java)

        tripAccessService = TripAccessService(
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository
        )

        service = DocumentService(
            documentRepository = documentRepository,
            documentStorageService = documentStorageService,
            tripAccessService = tripAccessService
        )
    }

    // ---------------------------------------------------------
    // UPDATE DOCUMENT
    // ---------------------------------------------------------

    @Test
    fun `owner can update another member's document`() {

        mockOwnerAccess()

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.updateDocument(
            userId = ownerId,
            tripId = tripId,
            documentId = documentId,
            name = "Updated Passport",
            category = DocumentCategory.PASSPORT_VISA,
            description = "Updated description",
            documentNumber = "P123456",
            issuedBy = "Government",
            issuedDate = LocalDate.of(2025, 1, 1),
            expiryDate = LocalDate.of(2035, 1, 1)
        )

        assertEquals("Updated Passport", result.name)
        assertEquals("Updated description", result.description)
        assertEquals("P123456", result.documentNumber)
        assertEquals("Government", result.issuedBy)
        assertEquals(LocalDate.of(2025, 1, 1), result.issuedDate)
        assertEquals(LocalDate.of(2035, 1, 1), result.expiryDate)

        verify(documentRepository).save(document)
    }

    @Test
    fun `member can update own document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.updateDocument(
            userId = memberId,
            tripId = tripId,
            documentId = documentId,
            name = "My Updated Document",
            category = DocumentCategory.ID_PROOF,
            description = "Updated",
            documentNumber = null,
            issuedBy = null,
            issuedDate = null,
            expiryDate = null
        )

        assertEquals("My Updated Document", result.name)
        assertEquals(DocumentCategory.ID_PROOF, result.category)

        verify(documentRepository).save(document)
    }

    @Test
    fun `member cannot update another member's document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = otherMemberId
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        assertThrows<IllegalAccessException> {

            service.updateDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId,
                name = "Unauthorized Update",
                category = null,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null
            )
        }

        verify(documentRepository, never())
            .save(any(Document::class.java))
    }

    @Test
    fun `non member cannot update document`() {

        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip()))

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                nonMemberId
            )
        ).thenReturn(null)

        assertThrows<IllegalAccessException> {

            service.updateDocument(
                userId = nonMemberId,
                tripId = tripId,
                documentId = documentId,
                name = "Unauthorized",
                category = null,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null
            )
        }
    }

    @Test
    fun `cannot update trashed document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId,
            status = DocumentStatus.TRASHED
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        assertThrows<IllegalArgumentException> {
            service.updateDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId,
                name = "Updated",
                category = null,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null
            )
        }

        verify(documentRepository, never())
            .save(any(Document::class.java))
    }

    // ---------------------------------------------------------
    // DATE VALIDATION
    // ---------------------------------------------------------

    @Test
    fun `expiry date cannot be before issued date`() {

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        assertThrows<IllegalArgumentException> {
            service.updateDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId,
                name = null,
                category = null,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = LocalDate.of(2026, 10, 1),
                expiryDate = LocalDate.of(2026, 9, 1)
            )
        }

        verify(documentRepository, never())
            .save(any(Document::class.java))
    }

    @Test
    fun `valid issued and expiry dates are accepted`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.updateDocument(
            userId = memberId,
            tripId = tripId,
            documentId = documentId,
            name = null,
            category = null,
            description = null,
            documentNumber = null,
            issuedBy = null,
            issuedDate = LocalDate.of(2026, 1, 1),
            expiryDate = LocalDate.of(2027, 1, 1)
        )

        assertEquals(
            LocalDate.of(2026, 1, 1),
            result.issuedDate
        )

        assertEquals(
            LocalDate.of(2027, 1, 1),
            result.expiryDate
        )

        verify(documentRepository).save(document)
    }

    // ---------------------------------------------------------
    // FAVORITE
    // ---------------------------------------------------------

    @Test
    fun `member can favorite document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = otherMemberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.updateFavorite(
            userId = memberId,
            tripId = tripId,
            documentId = documentId,
            favorite = true
        )

        assertTrue(result.favorite)

        verify(documentRepository).save(document)
    }

    @Test
    fun `member can remove favorite from document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId,
            favorite = true
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        whenever(documentRepository.save(document))
            .thenReturn(document)

        val result = service.updateFavorite(
            userId = memberId,
            tripId = tripId,
            documentId = documentId,
            favorite = false
        )

        assertFalse(result.favorite)

        verify(documentRepository).save(document)
    }

    // ---------------------------------------------------------
    // TRASH / RESTORE
    // ---------------------------------------------------------

    @Test
    fun `member can trash own document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.trashDocument(
            userId = memberId,
            tripId = tripId,
            documentId = documentId
        )

        assertEquals(DocumentStatus.TRASHED, result.status)

        verify(documentRepository).save(document)
    }

    @Test
    fun `member cannot trash another member's document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = otherMemberId
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        assertThrows<IllegalAccessException> {

            service.trashDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId
            )
        }

        verify(documentRepository, never())
            .save(any(Document::class.java))
    }

    @Test
    fun `owner can trash another member's document`() {

        mockOwnerAccess()

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.trashDocument(
            userId = ownerId,
            tripId = tripId,
            documentId = documentId
        )

        assertEquals(DocumentStatus.TRASHED, result.status)

        verify(documentRepository).save(document)
    }

    @Test
    fun `member can restore own trashed document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId,
            status = DocumentStatus.TRASHED
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.restoreDocument(
            userId = memberId,
            tripId = tripId,
            documentId = documentId
        )

        assertEquals(DocumentStatus.ACTIVE, result.status)

        verify(documentRepository).save(document)
    }

    // ---------------------------------------------------------
    // GET DOCUMENT
    // ---------------------------------------------------------

    @Test
    fun `member can get document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = otherMemberId
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        val result = service.getDocument(
            userId = memberId,
            tripId = tripId,
            documentId = documentId
        )

        assertEquals(documentId, result.id)
        assertEquals(tripId, result.tripId)

        verify(documentRepository)
            .findByIdAndTripId(documentId, tripId)
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @Test
    fun `owner can permanently delete another member's document`() {

        mockOwnerAccess()

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(documentRepository.findByIdAndTripId(documentId, tripId))
            .thenReturn(document)

        whenever(documentStorageService.exists(document.storageKey))
            .thenReturn(true)

        service.permanentlyDeleteDocument(
            userId = ownerId,
            tripId = tripId,
            documentId = documentId
        )

        verify(documentStorageService)
            .delete(document.storageKey)

        verify(documentRepository)
            .delete(document)
    }

    @Test
    fun `member cannot permanently delete another member's document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = otherMemberId
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        assertThrows<IllegalAccessException> {

            service.permanentlyDeleteDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId
            )
        }

        verify(documentRepository, never())
            .delete(any(Document::class.java))

        verify(documentStorageService, never())
            .delete(anyString())
    }

    @Test
    fun `member can upload valid pdf document`() {

        mockAcceptedMember(memberId)

        val content = "fake pdf content".toByteArray()
        val inputStream = content.inputStream()

        whenever(documentRepository.save(any(Document::class.java)))
            .thenAnswer { it.arguments[0] }

        val result = service.uploadDocument(
            userId = memberId,
            tripId = tripId,
            name = "My Passport",
            originalFileName = "passport.pdf",
            mimeType = "application/pdf",
            fileSize = content.size.toLong(),
            category = DocumentCategory.PASSPORT_VISA,
            source = DocumentSource.DEVICE,
            description = "Passport copy",
            documentNumber = "P123456",
            issuedBy = "Government",
            issuedDate = LocalDate.of(2025, 1, 1),
            expiryDate = LocalDate.of(2035, 1, 1),
            inputStream = inputStream
        )

        assertEquals(tripId, result.tripId)
        assertEquals(memberId, result.uploadedBy)
        assertEquals("My Passport", result.name)
        assertEquals("passport.pdf", result.originalFileName)
        assertEquals("application/pdf", result.mimeType)
        assertEquals(DocumentCategory.PASSPORT_VISA, result.category)
        assertEquals(DocumentSource.DEVICE, result.source)
        assertEquals(DocumentStatus.ACTIVE, result.status)
        assertFalse(result.favorite)

        verify(documentRepository).save(any(Document::class.java))
    }

    @Test
    fun `upload rejects empty file`() {

        mockAcceptedMember(memberId)

        assertThrows<IllegalArgumentException> {
            service.uploadDocument(
                userId = memberId,
                tripId = tripId,
                name = "Passport",
                originalFileName = "passport.pdf",
                mimeType = "application/pdf",
                fileSize = 0,
                category = DocumentCategory.PASSPORT_VISA,
                source = DocumentSource.DEVICE,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null,
                inputStream = ByteArray(0).inputStream()
            )
        }
    }

    @Test
    fun `upload rejects file larger than 20 MB`() {

        mockAcceptedMember(memberId)

        val oversizedFile = 20L * 1024L * 1024L + 1

        assertThrows<IllegalArgumentException> {
            service.uploadDocument(
                userId = memberId,
                tripId = tripId,
                name = "Large document",
                originalFileName = "large.pdf",
                mimeType = "application/pdf",
                fileSize = oversizedFile,
                category = DocumentCategory.OTHER,
                source = DocumentSource.DEVICE,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null,
                inputStream = ByteArray(0).inputStream()
            )
        }
    }

    @Test
    fun `upload rejects unsupported file type`() {

        mockAcceptedMember(memberId)

        assertThrows<IllegalArgumentException> {
            service.uploadDocument(
                userId = memberId,
                tripId = tripId,
                name = "Executable",
                originalFileName = "virus.exe",
                mimeType = "application/octet-stream",
                fileSize = 100,
                category = DocumentCategory.OTHER,
                source = DocumentSource.DEVICE,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null,
                inputStream = ByteArray(100).inputStream()
            )
        }
    }

    @Test
    fun `upload rejects blank document name`() {

        mockAcceptedMember(memberId)

        assertThrows<IllegalArgumentException> {
            service.uploadDocument(
                userId = memberId,
                tripId = tripId,
                name = "   ",
                originalFileName = "passport.pdf",
                mimeType = "application/pdf",
                fileSize = 100,
                category = DocumentCategory.PASSPORT_VISA,
                source = DocumentSource.DEVICE,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null,
                inputStream = ByteArray(100).inputStream()
            )
        }
    }

    @Test
    fun `upload rejects expiry date before issued date`() {

        mockAcceptedMember(memberId)

        assertThrows<IllegalArgumentException> {
            service.uploadDocument(
                userId = memberId,
                tripId = tripId,
                name = "Passport",
                originalFileName = "passport.pdf",
                mimeType = "application/pdf",
                fileSize = 100,
                category = DocumentCategory.PASSPORT_VISA,
                source = DocumentSource.DEVICE,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = LocalDate.of(2030, 1, 1),
                expiryDate = LocalDate.of(2029, 1, 1),
                inputStream = ByteArray(100).inputStream()
            )
        }
    }

    @Test
    fun `stored file is deleted when database save fails`() {

        mockAcceptedMember(memberId)

        whenever(documentRepository.save(any(Document::class.java)))
            .thenThrow(RuntimeException("Database failure"))

        assertThrows<RuntimeException> {
            service.uploadDocument(
                userId = memberId,
                tripId = tripId,
                name = "Passport",
                originalFileName = "passport.pdf",
                mimeType = "application/pdf",
                fileSize = 100,
                category = DocumentCategory.PASSPORT_VISA,
                source = DocumentSource.DEVICE,
                description = null,
                documentNumber = null,
                issuedBy = null,
                issuedDate = null,
                expiryDate = null,
                inputStream = ByteArray(100).inputStream()
            )
        }

        verify(documentStorageService)
            .delete(anyString())
    }

    @Test
    fun `member can download active document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = otherMemberId
        )

        val content = "document content".toByteArray()

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        whenever(documentStorageService.exists(document.storageKey))
            .thenReturn(true)

        whenever(documentStorageService.load(document.storageKey))
            .thenReturn(content.inputStream())

        val result = service.downloadDocument(
            userId = memberId,
            tripId = tripId,
            documentId = documentId
        )

        assertEquals("passport.pdf", result.fileName)
        assertEquals("application/pdf", result.contentType)
        assertEquals(1024L, result.fileSize)

        assertArrayEquals(
            content,
            result.inputStream.readBytes()
        )
    }

    @Test
    fun `cannot download trashed document`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId,
            status = DocumentStatus.TRASHED
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        assertThrows<IllegalArgumentException> {
            service.downloadDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId
            )
        }
    }

    @Test
    fun `download fails when stored file does not exist`() {

        mockAcceptedMember(memberId)

        val document = createDocument(
            uploadedBy = memberId
        )

        whenever(
            documentRepository.findByIdAndTripId(
                documentId,
                tripId
            )
        ).thenReturn(document)

        whenever(documentStorageService.exists(document.storageKey))
            .thenReturn(false)

        assertThrows<IllegalArgumentException> {
            service.downloadDocument(
                userId = memberId,
                tripId = tripId,
                documentId = documentId
            )
        }
    }

    @Test
    fun `non member cannot download document`() {

        mockNonMember(nonMemberId)

        assertThrows<IllegalAccessException> {
            service.downloadDocument(
                userId = nonMemberId,
                tripId = tripId,
                documentId = documentId
            )
        }
    }

    // ---------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------

    private fun createDocument(
        uploadedBy: UUID,
        status: DocumentStatus = DocumentStatus.ACTIVE,
        favorite: Boolean = false
    ): Document {
        return Document(
            id = documentId,
            tripId = tripId,
            uploadedBy = uploadedBy,
            name = "Passport",
            originalFileName = "passport.pdf",
            mimeType = "application/pdf",
            fileSize = 1024L,
            storageKey = "trips/$tripId/documents/$documentId.pdf",
            category = DocumentCategory.PASSPORT_VISA,
            source = DocumentSource.DEVICE,
            status = status,
            description = "Passport document",
            documentNumber = "P123456",
            issuedBy = "Government",
            issuedDate = LocalDate.of(2025, 1, 1),
            expiryDate = LocalDate.of(2035, 1, 1),
            favorite = favorite
        )
    }

    private fun mockAcceptedMember(userId: UUID) {

        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip()))

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        ).thenReturn(
            TripMember(
                id = UUID.randomUUID(),
                tripId = tripId,
                userId = userId,
                role = TripMemberRole.MEMBER,
                status = TripMemberStatus.ACCEPTED
            )
        )
    }

    private fun mockNonMember(userId: UUID) {

        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip()))

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        ).thenReturn(null)
    }

    private fun trip(): Trip {
        return Trip(
            id = tripId,
            ownerId = ownerId,
            name = "Dubai Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 10)
        )
    }

    private fun mockOwnerAccess() {

        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip()))
    }
}