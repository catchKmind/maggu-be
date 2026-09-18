package com.maggu.maggu.place.repository;

import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceFolderRepository extends JpaRepository<PlaceFolder, Long> {

    List<PlaceFolder> findAllByUserOrderByIsDefaultDescCreatedAtAsc(AppUser user);

    boolean existsByUserAndName(AppUser user, String name);
}
