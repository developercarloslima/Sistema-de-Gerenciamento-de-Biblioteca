package com.carlos.library.service;

import com.carlos.library.domain.entity.Fine;
import com.carlos.library.domain.enums.FineStatus;
import com.carlos.library.dto.ApiDtos.FineResponse;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FineService {
    private final FineRepository fines;
    private final MemberService memberService;
    private final MapperService mapper;

    @Transactional(readOnly = true)
    public Page<FineResponse> list(FineStatus status, Pageable pageable) {
        return (status == null ? fines.findAll(pageable) : fines.findByStatus(status, pageable)).map(mapper::fine);
    }

    @Transactional(readOnly = true)
    public Page<FineResponse> mine(String email, Pageable pageable) {
        return fines.findByMemberId(memberService.findByEmail(email).getId(), pageable).map(mapper::fine);
    }

    @Transactional
    public FineResponse pay(UUID id) {
        Fine f = find(id);
        if (f.getStatus() != FineStatus.PENDING) throw new BusinessException("Multa não está pendente.");
        f.setStatus(FineStatus.PAID); f.setPaidAt(OffsetDateTime.now(ZoneOffset.UTC));
        return mapper.fine(f);
    }

    @Transactional
    public FineResponse cancel(UUID id) {
        Fine f = find(id);
        if (f.getStatus() != FineStatus.PENDING) throw new BusinessException("Multa não está pendente.");
        f.setStatus(FineStatus.CANCELLED);
        return mapper.fine(f);
    }

    private Fine find(UUID id) { return fines.findById(id).orElseThrow(() -> new NotFoundException("Multa não encontrada.")); }
}
