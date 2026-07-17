package com.carlos.library.repository;

import com.carlos.library.domain.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    boolean existsByIsbn(String isbn);

    @Query("""
        select b from Book b
        where b.active = true
          and (:query = '' or lower(b.title) like concat('%', lower(:query), '%')
               or lower(b.author) like concat('%', lower(:query), '%')
               or lower(b.isbn) like concat('%', lower(:query), '%'))
          and (:category = '' or lower(b.category) = lower(:category))
          and (:availableOnly = false or exists (
              select c.id from BookCopy c where c.book = b and c.status = com.carlos.library.domain.enums.BookCopyStatus.AVAILABLE
          ))
          and (:digitalOnly = false or exists (
              select a.id from BookAsset a where a.book = b and a.assetType = com.carlos.library.domain.enums.BookAssetType.PDF
          ))
        """)
    Page<Book> search(@Param("query") String query, @Param("category") String category,
                      @Param("availableOnly") boolean availableOnly, @Param("digitalOnly") boolean digitalOnly,
                      Pageable pageable);
    @Query("""
        select distinct b from Book b join BookAsset a on a.book = b
        where b.active = true
          and a.assetType = com.carlos.library.domain.enums.BookAssetType.PDF
          and a.accessLevel in :accessLevels
          and (:query = '' or lower(b.title) like concat('%', lower(:query), '%')
               or lower(b.author) like concat('%', lower(:query), '%')
               or lower(b.isbn) like concat('%', lower(:query), '%'))
        """)
    Page<Book> searchDigitalCatalog(@Param("query") String query,
                                    @Param("accessLevels") java.util.Collection<com.carlos.library.domain.enums.DigitalAccessLevel> accessLevels,
                                    Pageable pageable);

}
