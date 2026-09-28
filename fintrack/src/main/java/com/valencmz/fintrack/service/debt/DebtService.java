package com.valencmz.fintrack.service.debt;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.debt.DebtRequest;
import com.valencmz.fintrack.model.dto.debt.DebtResponse;
import com.valencmz.fintrack.model.entity.Debt;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.DebtRepository;

@Service
public class DebtService {

    @Autowired
    private DebtRepository debtRepository;

    public List<DebtResponse> getByUser(UserAuth userAuth) {
        return debtRepository.findByUserId(userAuth.getUser().getId())
                .stream().map(DebtResponse::new).toList();
    }

    public DebtResponse getById(UUID id, UserAuth userAuth) {
        return new DebtResponse(findOwnedDebt(id, userAuth.getUser().getId()));
    }

    public DebtResponse create(DebtRequest request, UserAuth userAuth) {
        Debt debt = request.toEntity();
        debt.setUser(userAuth.getUser());
        return new DebtResponse(debtRepository.save(debt));
    }

    public DebtResponse update(UUID id, DebtRequest request, UserAuth userAuth) {
        Debt debt = findOwnedDebt(id, userAuth.getUser().getId());
        debt.setCreditor(request.getCreditor());
        debt.setTotalAmount(request.getTotalAmount());
        debt.setRemainingAmount(request.getRemainingAmount());
        debt.setStartDate(request.getStartDate());
        debt.setStatus(request.getStatus());
        return new DebtResponse(debtRepository.save(debt));
    }

    public void delete(UUID id, UserAuth userAuth) {
        debtRepository.delete(findOwnedDebt(id, userAuth.getUser().getId()));
    }

    private Debt findOwnedDebt(UUID id, UUID userId) {
        return debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Debt not found", HttpStatus.NOT_FOUND));
    }
}
