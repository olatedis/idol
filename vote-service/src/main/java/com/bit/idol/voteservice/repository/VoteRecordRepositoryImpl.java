package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.dto.MyVoteRecordDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.bit.idol.voteservice.entity.QCandidate.candidate;
import static com.bit.idol.voteservice.entity.QVote.vote;
import static com.bit.idol.voteservice.entity.QVoteRecord.voteRecord;

@RequiredArgsConstructor
public class VoteRecordRepositoryImpl implements VoteRecordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyVoteRecordDto> findMyVoteRecords(int userId) {
        return queryFactory
                .select(Projections.constructor(MyVoteRecordDto.class,
                        voteRecord.voteId,
                        vote.title,
                        candidate.number, // candidateNumber -> number 수정
                        candidate.name,
                        voteRecord.votedAt
                ))
                .from(voteRecord)
                .join(voteRecord.vote, vote)           // Vote 조인
                .join(voteRecord.candidate, candidate) // Candidate 조인
                .where(voteRecord.userId.eq(userId))
                .orderBy(voteRecord.votedAt.desc())    // 최신순 정렬
                .fetch();
    }
}
