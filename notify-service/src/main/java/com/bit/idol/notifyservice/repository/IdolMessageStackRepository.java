package com.bit.idol.notifyservice.repository;

import com.bit.idol.notifyservice.entity.IdolMessageStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IdolMessageStackRepository extends JpaRepository<IdolMessageStack, Long> {

    Optional<IdolMessageStack> findByReceiverIdAndIdolId(int receiverId, long idolId);

    List<IdolMessageStack> findAllByReceiverIdOrderByLastOccurredAtDesc(int receiverId);

    int deleteByReceiverId(int receiverId);
}
