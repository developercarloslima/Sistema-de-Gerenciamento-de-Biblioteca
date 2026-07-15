package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.MessageResponse;
import com.carlos.library.service.LoanService;
import com.carlos.library.service.ReservationService;
import com.carlos.library.service.DigitalBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceController {
    private final LoanService loans;
    private final ReservationService reservations;
    private final DigitalBookService digitalBooks;

    @PostMapping("/process-deadlines")
    MessageResponse processDeadlines() {
        loans.processDeadlines();
        reservations.expireReadyReservations();
        digitalBooks.expirePendingUploads();
        return new MessageResponse("Prazos, atrasos, reservas e uploads expirados foram processados.");
    }
}
