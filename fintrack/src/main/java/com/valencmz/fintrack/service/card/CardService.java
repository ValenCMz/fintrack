package com.valencmz.fintrack.service.card;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.card.CardRequest;
import com.valencmz.fintrack.model.dto.card.CardResponse;
import com.valencmz.fintrack.model.entity.Card;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.CardRepository;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    public List<CardResponse> getByUser(UserAuth userAuth) {
        return cardRepository.findByUserId(userAuth.getUser().getId())
                .stream().map(CardResponse::new).toList();
    }

    public CardResponse getById(UUID id, UserAuth userAuth) {
        return new CardResponse(findOwnedCard(id, userAuth.getUser().getId()));
    }

    public CardResponse create(CardRequest request, UserAuth userAuth) {
        Card card = request.toEntity();
        card.setUser(userAuth.getUser());
        return new CardResponse(cardRepository.save(card));
    }

    public CardResponse update(UUID id, CardRequest request, UserAuth userAuth) {
        Card card = findOwnedCard(id, userAuth.getUser().getId());
        card.setHolderName(request.getHolderName());
        card.setDueDay(request.getDueDay());
        card.setAmount(request.getAmount());
        card.setActive(request.isActive());
        return new CardResponse(cardRepository.save(card));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        Card card = findOwnedCard(id, userAuth.getUser().getId());
        card.setActive(false);
        cardRepository.save(card);
    }

    private Card findOwnedCard(UUID id, UUID userId) {
        return cardRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Card not found", HttpStatus.NOT_FOUND));
    }
}
