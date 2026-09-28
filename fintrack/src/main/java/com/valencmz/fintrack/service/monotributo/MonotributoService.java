package com.valencmz.fintrack.service.monotributo;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest;
import com.valencmz.fintrack.model.dto.monotributo.MonotributoResponse;
import com.valencmz.fintrack.model.entity.Monotributo;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.MonotributoRepository;

@Service
public class MonotributoService {

    @Autowired
    private MonotributoRepository monotributoRepository;

    public List<MonotributoResponse> getByUser(UserAuth userAuth) {
        return monotributoRepository.findByUserId(userAuth.getUser().getId())
                .stream().map(MonotributoResponse::new).toList();
    }

    public MonotributoResponse getById(UUID id, UserAuth userAuth) {
        return new MonotributoResponse(findOwnedMonotributo(id, userAuth.getUser().getId()));
    }

    public MonotributoResponse create(MonotributoRequest request, UserAuth userAuth) {
        Monotributo monotributo = request.toEntity();
        monotributo.setUser(userAuth.getUser());
        return new MonotributoResponse(monotributoRepository.save(monotributo));
    }

    public MonotributoResponse update(UUID id, MonotributoRequest request, UserAuth userAuth) {
        Monotributo monotributo = findOwnedMonotributo(id, userAuth.getUser().getId());
        monotributo.setName(request.getName());
        monotributo.setMonthlyAmount(request.getMonthlyAmount());
        monotributo.setDueDay(request.getDueDay());
        monotributo.setStatus(request.getStatus());
        return new MonotributoResponse(monotributoRepository.save(monotributo));
    }

    public void delete(UUID id, UserAuth userAuth) {
        monotributoRepository.delete(findOwnedMonotributo(id, userAuth.getUser().getId()));
    }

    private Monotributo findOwnedMonotributo(UUID id, UUID userId) {
        return monotributoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Monotributo not found", HttpStatus.NOT_FOUND));
    }
}
