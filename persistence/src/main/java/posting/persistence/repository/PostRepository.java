package posting.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import posting.persistence.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from User u " +
            "inner join u.posts p " +
            "left join fetch p.user " +
            "where u.username = :username " +
            "order by p.createdDate desc")
    List<Post> findByUsernameOrderByCreatedDateDescending(@Param("username") String username);

    @Query(
            value = "select p from User u " +
                    "inner join u.posts p " +
                    "left join fetch p.user " +
                    "where u.username = :username " +
                    "order by p.createdDate desc",
            countQuery = "select count(p) from User u " +
                    "inner join u.posts p " +
                    "where u.username = :username")
    Page<Post> findByUsernameOrderByCreatedDateDescending(@Param("username") String username, Pageable pageRequest);

    @Query("select p from User u " +
            "inner join u.followed f " +
            "inner join f.posts p " +
            "left join fetch p.user " +
            "where u.username = :username " +
            "order by p.createdDate desc")
    List<Post> findByFollowedOrderByCreatedDateDescending(@Param("username") String username);

    @Query(
            value = "select p from User u " +
                    "inner join u.followed f " +
                    "inner join f.posts p " +
                    "left join fetch p.user " +
                    "where u.username = :username " +
                    "order by p.createdDate desc",
            countQuery = "select count(p) from User u " +
                    "inner join u.followed f " +
                    "inner join f.posts p " +
                    "where u.username = :username")
    Page<Post> findByFollowedOrderByCreatedDateDescending(@Param("username") String username,
                                                          Pageable pageRequest);
}