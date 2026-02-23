package IAMService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import IAMService.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
