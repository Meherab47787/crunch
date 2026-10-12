package com.crustcrunch.repository;

import com.crustcrunch.entity.CustomizationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomizationGroupRepository extends JpaRepository<CustomizationGroup, Long> {

    List<CustomizationGroup> findAllByOrderByNameAsc();

    @Query("SELECT count(pcg) FROM ProductCustomizationGroup pcg WHERE pcg.group.id = :groupId")
    long countProductUsages(@Param("groupId") Long groupId);
}
