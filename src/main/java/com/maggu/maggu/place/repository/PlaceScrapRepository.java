package com.maggu.maggu.place.repository;

import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.entity.PlaceScrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceScrapRepository extends JpaRepository<PlaceScrap, Long> {

    List<PlaceScrap> findAllByPlaceFolderOrderByCreatedAtDesc(PlaceFolder placeFolder);

    boolean existsByPlaceFolderIdAndTourismContentId(Long placeFolderId, String tourismContentId);

}
