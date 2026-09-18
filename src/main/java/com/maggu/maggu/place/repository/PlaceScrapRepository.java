package com.maggu.maggu.place.repository;

import com.maggu.maggu.place.entity.PlaceScrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceScrapRepository extends JpaRepository<PlaceScrap, Long> {

    boolean existsByPlaceFolderIdAndTourismContentId(Long placeFolderId, String tourismContentId);

}
