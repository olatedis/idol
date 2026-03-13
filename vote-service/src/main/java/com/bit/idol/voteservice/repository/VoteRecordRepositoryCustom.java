package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.dto.MyVoteRecordDto;

import java.util.List;

public interface VoteRecordRepositoryCustom {
    List<MyVoteRecordDto> findMyVoteRecords(int userId, Long groupId);
}
