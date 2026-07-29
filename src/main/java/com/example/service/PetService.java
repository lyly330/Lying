package com.example.service;

import com.example.domain.Pet;

public interface PetService {
        Pet getPetByUserId(Long userId);
        Pet saveOrUpdatePet(Long userId, Pet pet);
}
