package ru.project.congratulatory.bot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Service
@Transactional
public interface UserRepo extends JpaRepository<User, Long> {
    List<User> findAllDateByChatNum(long chatNum);

    List<User> findAllDateByDate(String day);
    void deleteById(int id);
}
