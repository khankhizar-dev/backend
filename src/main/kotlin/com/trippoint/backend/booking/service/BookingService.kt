package com.trippoint.backend.booking.service

import com.trippoint.backend.booking.entity.Booking
import com.trippoint.backend.booking.entity.BookingEvent
import com.trippoint.backend.booking.entity.BookingTraveller
import com.trippoint.backend.booking.graphql.BookingEventResponse
import com.trippoint.backend.booking.graphql.BookingResponse
import com.trippoint.backend.booking.graphql.BookingTravellerResponse
import com.trippoint.backend.booking.graphql.input.BookingFilterInput
import com.trippoint.backend.booking.graphql.input.CreateBookingInput
import com.trippoint.backend.booking.graphql.input.CreateBookingTravellerInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingTravellerInput
import com.trippoint.backend.booking.model.BookingSource
import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.repository.BookingEventRepository
import com.trippoint.backend.booking.repository.BookingRepository
import com.trippoint.backend.booking.repository.BookingTravellerRepository
import com.trippoint.backend.trip.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val bookingTravellerRepository: BookingTravellerRepository,
    private val bookingEventRepository: BookingEventRepository,
    private val tripRepository: TripRepository
) {

    @Transactional
    fun createBooking(
        userId: UUID,
        tripId: UUID,
        input: CreateBookingInput
    ): BookingResponse {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val title = input.title.trim()

        require(title.isNotBlank()) {
            "Booking title cannot be blank"
        }

        val provider = input.provider
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val bookingReference = input.bookingReference
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (bookingReference != null) {
            require(
                !bookingRepository.existsByTripIdAndBookingReference(
                    tripId,
                    bookingReference
                )
            ) {
                "Booking reference already exists"
            }
        }

        input.amount?.let {
            require(it >= BigDecimal.ZERO) {
                "Amount cannot be negative"
            }
        }

        val startAt = input.startAt?.let {
            LocalDateTime.parse(it)
        }

        val endAt = input.endAt?.let {
            LocalDateTime.parse(it)
        }

        if (startAt != null && endAt != null) {
            require(!endAt.isBefore(startAt)) {
                "End time cannot be before start time"
            }
        }

        val itineraryDayId = input.itineraryDayId?.let {
            UUID.fromString(it)
        }

        val booking = bookingRepository.save(
            Booking(
                tripId = tripId,
                itineraryDayId = itineraryDayId,
                createdBy = userId,
                type = input.type,
                title = title,
                provider = provider,
                bookingReference = bookingReference,
                startAt = startAt,
                endAt = endAt,
                location = input.location
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                amount = input.amount,
                currency = input.currency
                    ?.trim()
                    ?.uppercase()
                    ?.takeIf { it.isNotBlank() },
                source = BookingSource.MANUAL,
                notes = input.notes
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                details = input.details
            )
        )

        bookingEventRepository.save(
            BookingEvent(
                bookingId = booking.id,
                eventType = "BOOKING_CREATED",
                description = "Booking created manually",
                createdBy = userId
            )
        )

        return BookingResponse.from(booking)
    }

    @Transactional(readOnly = true)
    fun getBookings(
        userId: UUID,
        tripId: UUID,
        filter: BookingFilterInput? = null
    ): List<BookingResponse> {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val search = filter?.search
            ?.trim()
            ?.lowercase()

        return bookingRepository
            .findAllByTripIdOrderByStartAtAsc(tripId)
            .filter { booking ->

                val matchesType =
                    filter?.type == null ||
                            booking.type == filter.type

                val matchesStatus =
                    filter?.status == null ||
                            booking.status == filter.status

                val matchesSearch =
                    search.isNullOrBlank() ||
                            booking.title.lowercase().contains(search) ||
                            booking.provider
                                ?.lowercase()
                                ?.contains(search) == true ||
                            booking.bookingReference
                                ?.lowercase()
                                ?.contains(search) == true

                matchesType &&
                        matchesStatus &&
                        matchesSearch
            }
            .map(BookingResponse::from)
    }

    @Transactional(readOnly = true)
    fun getBooking(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID
    ): BookingResponse {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val booking = bookingRepository.findByIdAndTripId(
            bookingId,
            tripId
        ) ?: throw IllegalArgumentException("Booking not found")

        return BookingResponse.from(booking)
    }

    @Transactional
    fun updateBooking(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID,
        input: UpdateBookingInput
    ): BookingResponse {
        var statusChanged = false

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val booking = bookingRepository.findByIdAndTripId(
            bookingId,
            tripId
        ) ?: throw IllegalArgumentException("Booking not found")

        input.type?.let {
            booking.type = it
        }

        input.title?.let {
            val title = it.trim()

            require(title.isNotBlank()) {
                "Booking title cannot be blank"
            }

            booking.title = title
        }

        input.provider?.let {
            booking.provider = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.bookingReference?.let {
            val reference = it
                .trim()
                .takeIf { value -> value.isNotBlank() }

            if (
                reference != null &&
                reference != booking.bookingReference
            ) {
                require(
                    !bookingRepository.existsByTripIdAndBookingReference(
                        tripId,
                        reference
                    )
                ) {
                    "Booking reference already exists"
                }
            }

            booking.bookingReference = reference
        }

        input.startAt?.let {
            booking.startAt = LocalDateTime.parse(it)
        }

        input.endAt?.let {
            booking.endAt = LocalDateTime.parse(it)
        }

        if (
            booking.startAt != null &&
            booking.endAt != null
        ) {
            require(
                !booking.endAt!!.isBefore(booking.startAt)
            ) {
                "End time cannot be before start time"
            }
        }

        input.location?.let {
            booking.location = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.amount?.let {
            require(it >= java.math.BigDecimal.ZERO) {
                "Amount cannot be negative"
            }

            booking.amount = it
        }

        input.currency?.let {
            val currency = it
                .trim()
                .uppercase()
                .takeIf { value -> value.isNotBlank() }

            if (currency != null) {
                require(currency.length == 3) {
                    "Currency must contain 3 characters"
                }
            }

            booking.currency = currency
        }

        input.status?.let { requestedStatus ->

            validateStatusTransition(
                booking.status,
                requestedStatus
            )

            if (booking.status != requestedStatus) {

                val previousStatus = booking.status

                booking.status = requestedStatus
                statusChanged = true

                bookingEventRepository.save(
                    BookingEvent(
                        bookingId = booking.id,
                        eventType = "STATUS_CHANGED",
                        description =
                            "Booking status changed from " +
                                    "$previousStatus to $requestedStatus",
                        createdBy = userId
                    )
                )
            }
        }

        input.notes?.let {
            booking.notes = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.details?.let {
            booking.details = it
        }

        booking.updatedAt = LocalDateTime.now()

        val savedBooking = bookingRepository.save(booking)

        if (!statusChanged) {
            bookingEventRepository.save(
                BookingEvent(
                    bookingId = booking.id,
                    eventType = "BOOKING_UPDATED",
                    description = "Booking details updated",
                    createdBy = userId
                )
            )
        }

        return BookingResponse.from(savedBooking)
    }

    @Transactional
    fun deleteBooking(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID
    ): Boolean {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val booking = bookingRepository.findByIdAndTripId(
            bookingId,
            tripId
        ) ?: throw IllegalArgumentException("Booking not found")

        bookingEventRepository.deleteAll(
            bookingEventRepository
                .findAllByBookingIdOrderByCreatedAtAsc(booking.id)
        )

        bookingTravellerRepository.deleteAllByBookingId(
            booking.id
        )

        bookingRepository.delete(booking)

        return true
    }

    @Transactional(readOnly = true)
    fun getBookingTravellers(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID
    ): List<BookingTravellerResponse> {

        validateBookingAccess(
            userId,
            tripId,
            bookingId
        )

        return bookingTravellerRepository
            .findAllByBookingId(bookingId)
            .map(BookingTravellerResponse::from)
    }

    @Transactional(readOnly = true)
    fun getBookingEvents(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID
    ): List<BookingEventResponse> {

        validateBookingAccess(
            userId,
            tripId,
            bookingId
        )

        return bookingEventRepository
            .findAllByBookingIdOrderByCreatedAtAsc(bookingId)
            .map(BookingEventResponse::from)
    }

    private fun validateBookingAccess(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID
    ) {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        bookingRepository.findByIdAndTripId(
            bookingId,
            tripId
        ) ?: throw IllegalArgumentException("Booking not found")
    }

    @Transactional
    fun addBookingTraveller(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID,
        input: CreateBookingTravellerInput
    ): BookingTravellerResponse {

        validateBookingAccess(
            userId,
            tripId,
            bookingId
        )

        val firstName = input.firstName.trim()

        require(firstName.isNotBlank()) {
            "First name cannot be blank"
        }

        val traveller = BookingTraveller(
            bookingId = bookingId,
            firstName = firstName,
            lastName = input.lastName
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            email = input.email
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            phoneNumber = input.phoneNumber
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            dateOfBirth = input.dateOfBirth?.let {
                LocalDate.parse(it)
            },
            ticketNumber = input.ticketNumber
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            seatNumber = input.seatNumber
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        )

        return BookingTravellerResponse.from(
            bookingTravellerRepository.save(traveller)
        )
    }

    @Transactional
    fun updateBookingTraveller(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID,
        travellerId: UUID,
        input: UpdateBookingTravellerInput
    ): BookingTravellerResponse {

        validateBookingAccess(
            userId,
            tripId,
            bookingId
        )

        val traveller =
            bookingTravellerRepository.findByIdAndBookingId(
                travellerId,
                bookingId
            ) ?: throw IllegalArgumentException(
                "Booking traveller not found"
            )

        input.firstName?.let {
            val firstName = it.trim()

            require(firstName.isNotBlank()) {
                "First name cannot be blank"
            }

            traveller.firstName = firstName
        }

        input.lastName?.let {
            traveller.lastName = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.email?.let {
            traveller.email = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.phoneNumber?.let {
            traveller.phoneNumber = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.dateOfBirth?.let {
            traveller.dateOfBirth =
                java.time.LocalDate.parse(it)
        }

        input.ticketNumber?.let {
            traveller.ticketNumber = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        input.seatNumber?.let {
            traveller.seatNumber = it
                .trim()
                .takeIf { value -> value.isNotBlank() }
        }

        traveller.updatedAt = java.time.LocalDateTime.now()

        return BookingTravellerResponse.from(
            bookingTravellerRepository.save(traveller)
        )
    }

    @Transactional
    fun deleteBookingTraveller(
        userId: UUID,
        tripId: UUID,
        bookingId: UUID,
        travellerId: UUID
    ): Boolean {

        validateBookingAccess(
            userId,
            tripId,
            bookingId
        )

        val traveller =
            bookingTravellerRepository.findByIdAndBookingId(
                travellerId,
                bookingId
            ) ?: throw IllegalArgumentException(
                "Booking traveller not found"
            )

        bookingTravellerRepository.delete(traveller)

        return true
    }

    private fun validateStatusTransition(
        current: BookingStatus,
        requested: BookingStatus
    ) {
        if (current == requested) {
            return
        }

        val allowed = when (current) {
            BookingStatus.PENDING -> setOf(
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED,
                BookingStatus.FAILED
            )

            BookingStatus.CONFIRMED -> setOf(
                BookingStatus.COMPLETED,
                BookingStatus.CANCELLED,
                BookingStatus.REFUNDED
            )

            BookingStatus.COMPLETED -> setOf(
                BookingStatus.REFUNDED
            )

            BookingStatus.CANCELLED,
            BookingStatus.REFUNDED,
            BookingStatus.FAILED -> emptySet()
        }

        require(requested in allowed) {
            "Invalid booking status transition: $current -> $requested"
        }
    }
}