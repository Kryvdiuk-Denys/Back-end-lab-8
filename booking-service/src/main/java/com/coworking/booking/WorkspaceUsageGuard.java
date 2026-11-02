package com.coworking.booking;

import com.coworking.booking.repository.BookingRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceUsageGuard {

    private final BookingRepository bookingRepository;

    public WorkspaceUsageGuard(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public void ensureWorkspaceCanBeDeleted(Long workspaceId) {
        if (bookingRepository.existsByWorkspaceId(workspaceId)) {
            throw new IllegalStateException("Cannot delete workspace with existing bookings");
        }
    }
}
