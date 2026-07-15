package com.carlos.library.scheduler;

import com.carlos.library.service.LoanService;
import com.carlos.library.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LibraryScheduler {
    private final LoanService loans;
    private final ReservationService reservations;

    @Scheduled(cron = "${app.scheduler.deadlines-cron:0 0 8 * * *}", zone = "${app.scheduler.zone:America/Maceio}")
    public void processDeadlines() {
        loans.processDeadlines();
        reservations.expireReadyReservations();
    }
}
