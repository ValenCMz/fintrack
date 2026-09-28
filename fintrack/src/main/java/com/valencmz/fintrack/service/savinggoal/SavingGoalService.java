package com.valencmz.fintrack.service.savinggoal;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest;
import com.valencmz.fintrack.model.dto.savinggoal.SavingGoalResponse;
import com.valencmz.fintrack.model.entity.SavingGoal;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.SavingGoalRepository;

@Service
public class SavingGoalService {

    @Autowired
    private SavingGoalRepository savingGoalRepository;

    public List<SavingGoalResponse> getByUser(UserAuth userAuth) {
        return savingGoalRepository.findByUserId(userAuth.getUser().getId())
                .stream().map(SavingGoalResponse::new).toList();
    }

    public SavingGoalResponse getById(UUID id, UserAuth userAuth) {
        return new SavingGoalResponse(findOwnedSavingGoal(id, userAuth.getUser().getId()));
    }

    public SavingGoalResponse create(SavingGoalRequest request, UserAuth userAuth) {
        SavingGoal savingGoal = request.toEntity();
        savingGoal.setUser(userAuth.getUser());
        return new SavingGoalResponse(savingGoalRepository.save(savingGoal));
    }

    public SavingGoalResponse update(UUID id, SavingGoalRequest request, UserAuth userAuth) {
        SavingGoal savingGoal = findOwnedSavingGoal(id, userAuth.getUser().getId());
        savingGoal.setName(request.getName());
        savingGoal.setTargetAmount(request.getTargetAmount());
        savingGoal.setCurrentAmount(request.getCurrentAmount());
        savingGoal.setTargetDate(request.getTargetDate());
        savingGoal.setActive(request.isActive());
        return new SavingGoalResponse(savingGoalRepository.save(savingGoal));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        SavingGoal savingGoal = findOwnedSavingGoal(id, userAuth.getUser().getId());
        savingGoal.setActive(false);
        savingGoalRepository.save(savingGoal);
    }

    private SavingGoal findOwnedSavingGoal(UUID id, UUID userId) {
        return savingGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Saving goal not found", HttpStatus.NOT_FOUND));
    }
}
