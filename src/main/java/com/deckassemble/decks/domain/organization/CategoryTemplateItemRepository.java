package com.deckassemble.decks.domain.organization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTemplateItemRepository extends JpaRepository<CategoryTemplateItem, Long> {

    List<CategoryTemplateItem> findByCategoryTemplateIdOrderByDisplayOrderAscIdAsc(
            Long categoryTemplateId);

    void deleteByCategoryTemplateId(Long categoryTemplateId);
}
