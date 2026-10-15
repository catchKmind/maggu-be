package com.maggu.maggu.place.repository;

import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.entity.PlaceScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PlaceScrapRepository extends JpaRepository<PlaceScrap, Long> {

    @Query(value = """
            SELECT p.* FROM place_scrap p
            WHERE (p.place_folder_id = :placeFolderId)
                AND (:cursorId IS NULL OR (p.created_at, p.id) < (:createdAt, :cursorId))
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :size
            """, nativeQuery = true)
    List<PlaceScrap> findPageByPlaceFolderId(@Param("placeFolderId") Long placeFolderId,
                                             @Param("createdAt") Instant createdAt,
                                             @Param("cursorId") Long cursorId,
                                             @Param("size") int size);

    long countByPlaceFolder(PlaceFolder placeFolder);

    boolean existsByPlaceFolderIdAndTourismContentId(Long placeFolderId, String tourismContentId);

}
