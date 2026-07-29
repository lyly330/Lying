package com.example.repository;

import com.example.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long>{


        Optional<Pet> findByUserId(Long userId);

}
